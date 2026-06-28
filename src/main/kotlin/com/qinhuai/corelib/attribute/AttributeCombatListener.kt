package com.qinhuai.corelib.attribute

import com.qinhuai.corelib.QinhCoreLib
import com.qinhuai.corelib.lang.Lang
import com.qinhuai.corelib.util.TextUtil
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerExpChangeEvent
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

object AttributeCombatListener : Listener {

    private var reentrant = false
    private var combatMessages = false
    private var passCrit = false
    private var sweepScale = true
    private var sweepFactor = 0.5
    private var magicCauses: Set<EntityDamageEvent.DamageCause> = setOf(EntityDamageEvent.DamageCause.MAGIC)
    private var leechFromElemental = true
    private var isolateMlSkill = true

    fun reload() {
        combatMessages = QinhCoreLib.instance.config.getBoolean("combat.attribute-messages.enabled", false)
        sweepScale = QinhCoreLib.instance.config.getBoolean("combat.sweep.scale-with-attack", true)
        sweepFactor = QinhCoreLib.instance.config.getDouble("combat.sweep.factor", 0.5)
        magicCauses = QinhCoreLib.instance.config.getStringList("combat.spell-vampirism.magic-causes")
            .mapNotNull { runCatching { EntityDamageEvent.DamageCause.valueOf(it.trim().uppercase()) }.getOrNull() }
            .toSet()
            .ifEmpty { setOf(EntityDamageEvent.DamageCause.MAGIC) }
        leechFromElemental = QinhCoreLib.instance.config.getBoolean("combat.spell-vampirism.leech-from-elemental", true)
        isolateMlSkill = QinhCoreLib.instance.config.getBoolean("combat.isolate-mythiclib-skill-damage", true)
    }

    private fun eff(holder: UUID): Map<String, Double> = eff(holder, ActionHand.ANY)

    private fun eff(holder: UUID, hand: ActionHand): Map<String, Double> =
        AttributeModifierStore.foldTotals(holder, AttributeStatStore.totals(holder, hand), hand)

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onDamage(event: EntityDamageEvent) {
        if (reentrant || !AttributeService.isNativeActive()) return
        val handler = AttackHandlerRegistry.match(event)
        if (handler == null && !isPhysicalCombat(event.cause)) return
        // MM 技能伤害即便以 cause=ENTITY_ATTACK 落地，也据 MythicLib 类型隔离出物理近战管线（不套物理增伤/暴击）；
        // 其法术吸血等改由 onMagicDamage 处理。仅在无自定义 AttackHandler 接管时生效。
        if (handler == null && isolateMlSkill && MythicLibDamageBridge.isSkillDamage(event)) return
        val victim = event.entity as? LivingEntity ?: return
        val attacker = when {
            handler != null -> handler.resolveAttacker(event)
            event is EntityDamageByEntityEvent -> resolveAttacker(event.damager)
            else -> null
        }
        val attackerActive = attacker != null && hasAttributes(attacker)
        val victimActive = hasAttributes(victim)
        if (!attackerActive && !victimActive) return

        if (event.cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK && sweepScale && attacker != null && attackerActive) {
            rescaleSweep(event, attacker)
        }

        val trace = mutableListOf<String>()
        val baseDamage = event.damage
        var damage = event.damage
        passCrit = false
        val projectile = handler?.isProjectile(event) ?: (event.cause == EntityDamageEvent.DamageCause.PROJECTILE)
        val swingHand = if (projectile) ActionHand.ANY else ActionHand.MAIN
        val attackerTotals = if (attacker != null && attackerActive) eff(attacker.uniqueId, swingHand) else emptyMap()
        val trueDmg = if (attacker != null && attackerActive)
            ((attackerTotals["true_attack"] ?: 0.0) * (1.0 + (attackerTotals["true_damage"] ?: 0.0))).coerceAtLeast(0.0)
        else 0.0
        val elementParts = if (attacker != null && attackerActive) elementalBreakdown(attackerTotals, victim, attackerTotals["magic_penetration"] ?: 0.0) else emptyList()
        val elementSum = elementParts.sumOf { it.second }
        val bypassStream = trueDmg + elementSum
        reentrant = true
        var physFinal = damage
        var evasionMult = 1.0
        try {
            if (attacker != null && attackerActive) {
                damage = applyOffense(attacker, victim, DamageType.PHYSICAL, damage, projectile, trace, swingHand)
                physFinal = damage
            }
            if (victimActive) {
                val def = applyDefense(victim, attacker, damage, bypassStream, trace, attackerTotals["armor_penetration"] ?: 0.0)
                damage = def.total
                physFinal = def.physPart
                evasionMult = def.evasionMult
            } else {
                physFinal = damage
                damage += bypassStream
            }
        } finally {
            reentrant = false
        }
        // 真实伤害无视一切防御；元素伤害无视物理防御但已在 elementalBreakdown 计过抗性/相克 —— 两者分开报，勿混淆
        val trueFinal = trueDmg * evasionMult
        val elementFinal = elementSum * evasionMult
        if (trueFinal > 0.0) trace.add(Lang.get("attribute-combat-listener.true-damage", "amount" to fmt(trueFinal)))
        if (elementFinal > 0.0) trace.add(Lang.get("attribute-combat-listener.elemental-damage", "amount" to fmt(elementFinal)))
        val packets = buildPackets(physFinal, trueDmg, elementParts, evasionMult, passCrit)
        val meta = com.qinhuai.corelib.combat.DamageMetadata(
            attacker = attacker,
            victim = victim,
            cause = event.cause,
            damageType = DamageType.PHYSICAL,
            baseDamage = baseDamage,
            finalDamage = damage,
            projectile = projectile,
            crit = passCrit,
            pvp = victim is Player,
            penetration = bypassStream,
            trace = trace,
            packets = packets,
        )
        val busEvent = com.qinhuai.corelib.combat.QinhDamageEvent(meta)
        QinhCoreLib.instance.server.pluginManager.callEvent(busEvent)
        if (busEvent.isCancelled) {
            event.isCancelled = true
            emitTrace(attacker as? Player, victim as? Player, trace)
            return
        }
        damage = meta.finalDamage
        if (damage.isFinite() && damage >= 0.0 && damage != event.damage) {
            event.damage = damage
        }
        emitTrace(attacker as? Player, victim as? Player, trace)
        // 近战/弹射附带的元素伤害也触发法术吸血（这部分由物理管线精确算出；纯技能伤害走下方 onMagicDamage）
        if (leechFromElemental && elementFinal > 0.0) (attacker as? Player)?.let { runMagicDamageHooks(it, elementFinal, victim) }
    }

    /**
     * 法术伤害链入口（与物理 applyOffense 的 on_damage_dealt 对称）：技能伤害发生在 MM 内部、
     * 不进上面的物理管线，故在最终伤害事件上按「法术 cause」或 MythicLib 标记的技能伤害监听，
     * 跑 on_magic_damage_dealt 钩子（法术吸血等）。物理 cause 的近战不在此处理（避免与物理管线重复）。
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMagicDamage(event: EntityDamageByEntityEvent) {
        if (!AttributeService.isNativeActive()) return
        if (event.cause !in magicCauses && !(isolateMlSkill && MythicLibDamageBridge.isSkillDamage(event))) return
        val attacker = resolveAttacker(event.damager) as? Player ?: return
        if (!hasAttributes(attacker)) return
        runMagicDamageHooks(attacker, event.finalDamage, event.entity)
    }

    /** 跑攻击者的 on_magic_damage_dealt 钩子（ctx.damage=法术伤害量, type=MAGIC）；钩子内做回血等，不改伤害(MONITOR)。 */
    private fun runMagicDamageHooks(attacker: Player, magicDamage: Double, victim: Entity?) {
        if (magicDamage <= 0.0 || !magicDamage.isFinite()) return
        val totals = eff(attacker.uniqueId)
        for (def in hookedFor(AttributeHooks.ON_MAGIC_DAMAGE_DEALT, totals)) {
            val ref = def.hook(AttributeHooks.ON_MAGIC_DAMAGE_DEALT) ?: continue
            AttributeScriptRunner.runDamageHook(ref, attacker, damageVars(def, magicDamage, totals, DamageType.MAGIC, "victim", victim))
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDeath(event: EntityDeathEvent) {
        if (!AttributeService.isNativeActive()) return
        val dead = event.entity
        if (dead !is Player && AttributeStatStore.has(dead.uniqueId)) MobAttributeService.clear(dead)
        val killer = event.entity.killer ?: return
        val totals = eff(killer.uniqueId)
        for (def in hookedFor(AttributeHooks.ON_KILL, totals)) {
            val ref = def.hook(AttributeHooks.ON_KILL) ?: continue
            AttributeScriptRunner.runEffectHook(ref, killer, effectVars(def, totals, "victim", event.entity))
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onExpChange(event: PlayerExpChangeEvent) {
        if (!AttributeService.isNativeActive() || event.amount <= 0) return
        val uuid = event.player.uniqueId
        val bonus = AttributeModifierStore.combine(uuid, "exp_bonus", AttributeStatStore.total(uuid, "exp_bonus"))
        if (bonus <= 0.0) return
        event.amount = Math.round(event.amount * (1.0 + bonus)).toInt()
    }

    private fun applyOffense(attacker: LivingEntity, victim: Entity?, type: DamageType, damage: Double, isProjectile: Boolean, trace: MutableList<String>, hand: ActionHand): Double {
        var current = damage
        val totals = eff(attacker.uniqueId, hand)
        trace.add(Lang.get("attribute-combat-listener.base-damage", "type" to type.displayName, "current" to fmt(current)))
        val bonus = totals[type.bonusKey] ?: 0.0
        if (bonus != 0.0) {
            current *= (1.0 + bonus)
            trace.add(Lang.get("attribute-combat-listener.bonus-damage", "type" to type.displayName, "key" to type.bonusKey, "current" to fmt(current), "mult" to fmt(1.0 + bonus)))
        }
        if (isProjectile) {
            val projBonus = totals["projectile_damage"] ?: 0.0
            if (projBonus != 0.0) {
                current *= (1.0 + projBonus)
                trace.add(Lang.get("attribute-combat-listener.projectile-bonus", "current" to fmt(current), "mult" to fmt(1.0 + projBonus)))
            }
        }
        if (type.hasCrit) {
            // 攻击者暴击率扣除受害者暴击抗性(crit_resist)；暴击是硬编码 roll，无法走 JS hook，故在此结算
            val rate = ((totals[type.critRateKey] ?: 0.0) - critResistOf(victim)).coerceAtLeast(0.0)
            if (rate > 0.0 && ThreadLocalRandom.current().nextDouble() < rate) {
                val critDmg = totals[type.critDamageKey] ?: 0.0
                val before = current
                current *= (1.5 + critDmg)
                passCrit = true
                trace.add(Lang.get("attribute-combat-listener.crit", "type" to type.displayName, "before" to fmt(before), "current" to fmt(current)))
                if (combatMessages) {
                    AttributeRegistry.resolve(type.critRateKey)?.message?.let { msg ->
                        (attacker as? Player)?.let { TextUtil.sendColored(it, msg.replace("{damage}", fmt(current)).replace("{value}", fmt(rate))) }
                    }
                }
            }
        }
        val isPvp = victim is Player
        val targetBonus = totals[if (isPvp) "pvp_damage" else "pve_damage"] ?: 0.0
        if (targetBonus != 0.0) {
            current *= (1.0 + targetBonus)
            trace.add(Lang.get("attribute-combat-listener.target-damage", "mode" to (if (isPvp) "PVP" else "PVE"), "current" to fmt(current), "mult" to fmt(1.0 + targetBonus)))
        }
        for (def in hookedFor(AttributeHooks.ON_DAMAGE_DEALT, totals)) {
            val ref = def.hook(AttributeHooks.ON_DAMAGE_DEALT) ?: continue
            val before = current
            val result = AttributeScriptRunner.runDamageHook(ref, attacker, damageVars(def, current, totals, type, "victim", victim))
            if (result != null && result.isFinite() && result >= 0.0) {
                current = result
                if (current != before) {
                    val note = if (current > before) " §c(${def.displayName})" else ""
                    trace.add(Lang.get("attribute-combat-listener.hook-offense", "type" to type.displayName, "key" to def.key, "current" to fmt(current), "note" to note))
                    sendAttrMessage(attacker, def, totals[def.key] ?: 0.0, current)
                }
            }
        }
        return current
    }

    private data class DefenseResult(val total: Double, val physPart: Double, val evasionMult: Double)

    private fun applyDefense(victim: LivingEntity, attacker: Entity?, physical: Double, bypassStream: Double, trace: MutableList<String>, armorPen: Double): DefenseResult {
        val totals = applyArmorPenetration(eff(victim.uniqueId), armorPen, trace)
        val hooks = hookedFor(AttributeHooks.ON_DAMAGE_TAKEN, totals)
        val combined = physical + bypassStream
        var afterEvasion = combined
        for (def in hooks) {
            if (def.mitigation) continue
            val ref = def.hook(AttributeHooks.ON_DAMAGE_TAKEN) ?: continue
            val before = afterEvasion
            val result = AttributeScriptRunner.runDamageHook(ref, victim, damageVars(def, afterEvasion, totals, null, "attacker", attacker))
            if (result != null && result.isFinite() && result >= 0.0) {
                afterEvasion = result
                if (afterEvasion != before) {
                    trace.add(Lang.get("attribute-combat-listener.evasion", "key" to def.key, "before" to fmt(before), "after" to fmt(afterEvasion)))
                    sendAttrMessage(victim, def, totals[def.key] ?: 0.0, afterEvasion)
                }
            }
        }
        val evasionMult = if (combined > 0.0) afterEvasion / combined else 0.0
        var physPart = physical * evasionMult
        val bypassPart = bypassStream * evasionMult
        for (def in hooks) {
            if (!def.mitigation) continue
            val ref = def.hook(AttributeHooks.ON_DAMAGE_TAKEN) ?: continue
            val before = physPart
            val result = AttributeScriptRunner.runDamageHook(ref, victim, damageVars(def, physPart, totals, null, "attacker", attacker))
            if (result != null && result.isFinite() && result >= 0.0) {
                physPart = result
                if (physPart != before) {
                    trace.add(Lang.get("attribute-combat-listener.mitigation", "key" to def.key, "before" to fmt(before), "after" to fmt(physPart)))
                    sendAttrMessage(victim, def, totals[def.key] ?: 0.0, physPart)
                }
            }
        }
        val isPvp = attacker is Player
        val defBonus = totals[if (isPvp) "pvp_defense" else "pve_defense"] ?: 0.0
        if (defBonus != 0.0) {
            val before = physPart
            physPart *= (1.0 - defBonus).coerceAtLeast(0.0)
            if (physPart != before) trace.add(Lang.get("attribute-combat-listener.target-defense", "mode" to (if (isPvp) "PVP" else "PVE"), "before" to fmt(before), "after" to fmt(physPart)))
        }
        return DefenseResult(physPart + bypassPart, physPart, evasionMult)
    }

    private fun buildPackets(
        physical: Double,
        trueDmg: Double,
        elementParts: List<Pair<Element, Double>>,
        evasionMult: Double,
        crit: Boolean,
    ): MutableList<com.qinhuai.corelib.combat.DamagePacket> {
        val out = mutableListOf<com.qinhuai.corelib.combat.DamagePacket>()
        if (physical > 0.0) out += com.qinhuai.corelib.combat.DamagePacket(physical, DamageType.PHYSICAL, null, crit, "physical")
        for ((elem, raw) in elementParts) {
            val v = raw * evasionMult
            if (v > 0.0) out += com.qinhuai.corelib.combat.DamagePacket(v, DamageType.MAGIC, elem.id, false, elem.id)
        }
        val t = trueDmg * evasionMult
        if (t > 0.0) out += com.qinhuai.corelib.combat.DamagePacket(t, DamageType.TRUE, null, false, "true")
        return out
    }

    private fun hookedFor(hook: String, totals: Map<String, Double>): List<AttributeDef> =
        AttributeRegistry.all()
            .filter { it.hook(hook) != null && (totals[it.key] ?: 0.0) != 0.0 }
            .sortedBy { it.order }

    private fun damageVars(
        def: AttributeDef,
        damage: Double,
        totals: Map<String, Double>,
        type: DamageType?,
        otherKey: String,
        other: Entity?,
    ): Map<String, Any> {
        val vars = HashMap<String, Any>()
        for ((k, v) in totals) vars[k] = v
        vars["attribute"] = def.key
        vars["value"] = totals[def.key] ?: 0.0
        vars["damage"] = damage
        if (type != null) vars["type"] = type.name
        if (other != null) vars[otherKey] = other
        return vars
    }

    private fun effectVars(def: AttributeDef, totals: Map<String, Double>, otherKey: String, other: Entity?): Map<String, Any> {
        val vars = HashMap<String, Any>()
        for ((k, v) in totals) vars[k] = v
        vars["attribute"] = def.key
        vars["value"] = totals[def.key] ?: 0.0
        if (other != null) vars[otherKey] = other
        return vars
    }

    private fun emitTrace(attacker: Player?, victim: Player?, trace: List<String>) {
        if (trace.isEmpty()) return
        val watchers = LinkedHashSet<Player>()
        if (attacker != null && AttributeDebug.isOn(attacker.uniqueId)) watchers.add(attacker)
        if (victim != null && AttributeDebug.isOn(victim.uniqueId)) watchers.add(victim)
        for (p in watchers) {
            TextUtil.sendColored(p, Lang.get("attribute-combat-listener.trace-header"))
            trace.forEach { TextUtil.sendColored(p, "  $it") }
        }
    }

    private fun elementalBreakdown(attackerTotals: Map<String, Double>, victim: Entity?, magicPen: Double): List<Pair<Element, Double>> {
        val elements = ElementRegistry.all()
        if (elements.isEmpty()) return emptyList()
        val vT = if (victim is Player) eff(victim.uniqueId) else emptyMap()
        val targetElement = ElementRegistry.dominantElement(vT)
        val penMult = (1.0 - magicPen).coerceIn(0.0, 1.0)
        val out = mutableListOf<Pair<Element, Double>>()
        for (elem in elements) {
            val flat = attackerTotals[elem.damageKey] ?: 0.0
            if (flat <= 0.0) continue
            val bonus = attackerTotals[elem.bonusKey] ?: 0.0
            // 法术穿透按百分比削减目标元素抗性
            val resist = ((vT[elem.resistKey] ?: 0.0) * penMult).coerceIn(0.0, 1.0)
            val restraint = ElementRegistry.restraintMultiplier(elem, targetElement)
            val v = flat * (1.0 + bonus) * (1.0 - resist) * restraint
            if (v > 0.0) out += elem to v
        }
        return out
    }

    /** 护甲穿透：armor_penetration 为 0~1 小数(与暴击率同制,0.08=穿8%)。削减目标 defense；目标无防御(=0)时不动,夹在[0,defense]绝不为负。 */
    private fun applyArmorPenetration(totals: Map<String, Double>, armorPen: Double, trace: MutableList<String>): Map<String, Double> {
        if (armorPen <= 0.0) return totals
        val defense = totals["defense"] ?: 0.0
        if (defense <= 0.0) return totals
        val reduced = defense * (1.0 - armorPen).coerceIn(0.0, 1.0)
        val penPct = Math.round(armorPen * 10000.0) / 100.0
        trace.add(Lang.get("attribute-combat-listener.armor-penetration", "pen" to fmt(penPct), "before" to fmt(defense), "after" to fmt(reduced)))
        return totals.toMutableMap().apply { this["defense"] = reduced }
    }

    private fun rescaleSweep(event: EntityDamageEvent, attacker: LivingEntity) {
        val def = AttributeRegistry.resolve("attack_damage") ?: return
        val attackDamage = NativeAttributeBackend.vanillaEffective(attacker, def) ?: return
        if (attackDamage > 0.0) event.damage = attackDamage * sweepFactor
    }

    private fun isPhysicalCombat(cause: EntityDamageEvent.DamageCause): Boolean =
        cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK ||
            cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK ||
            cause == EntityDamageEvent.DamageCause.PROJECTILE

    private fun hasAttributes(entity: LivingEntity): Boolean =
        entity is Player || AttributeStatStore.has(entity.uniqueId)

    /** 受害者暴击抗性(0~1)，直接削减攻击者暴击率。非生物/无属性=0。 */
    private fun critResistOf(victim: Entity?): Double {
        val v = victim as? LivingEntity ?: return 0.0
        if (!hasAttributes(v)) return 0.0
        return (eff(v.uniqueId)["crit_resist"] ?: 0.0).coerceIn(0.0, 1.0)
    }

    private fun sendAttrMessage(entity: LivingEntity, def: AttributeDef, value: Double, damage: Double) {
        if (!combatMessages) return
        val player = entity as? Player ?: return
        val msg = def.message ?: return
        TextUtil.sendColored(player, msg.replace("{value}", fmt(value)).replace("{damage}", fmt(damage)))
    }

    private fun fmt(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.2f", v)

    private fun resolveAttacker(damager: Entity): LivingEntity? = when (damager) {
        is LivingEntity -> damager
        is Projectile -> damager.shooter as? LivingEntity
        else -> null
    }
}
