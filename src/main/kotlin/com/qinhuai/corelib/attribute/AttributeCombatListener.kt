package com.qinhuai.corelib.attribute

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
import java.util.concurrent.ThreadLocalRandom

object AttributeCombatListener : Listener {

    private var reentrant = false

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onDamage(event: EntityDamageEvent) {
        if (reentrant || !AttributeService.isNativeActive()) return
        if (!isPhysicalCombat(event.cause)) return
        val victim = event.entity as? LivingEntity ?: return
        val attacker = if (event is EntityDamageByEntityEvent) resolveAttacker(event.damager) else null
        val attackerActive = attacker != null && hasAttributes(attacker)
        val victimActive = hasAttributes(victim)
        if (!attackerActive && !victimActive) return

        val trace = mutableListOf<String>()
        var damage = event.damage
        val bypassStream = if (attacker != null && attackerActive) {
            val t = AttributeStatStore.totals(attacker.uniqueId)
            val trueDmg = ((t["true_attack"] ?: 0.0) * (1.0 + (t["true_damage"] ?: 0.0))).coerceAtLeast(0.0)
            trueDmg + elementalDamage(t, victim)
        } else 0.0
        reentrant = true
        try {
            if (attacker != null && attackerActive) {
                val isProjectile = event.cause == EntityDamageEvent.DamageCause.PROJECTILE
                damage = applyOffense(attacker, victim, DamageType.PHYSICAL, damage, isProjectile, trace)
            }
            if (victimActive) {
                damage = applyDefense(victim, attacker, damage, bypassStream, trace)
            } else {
                damage += bypassStream
                if (bypassStream > 0.0) trace.add(Lang.get("attribute-combat-listener.penetration-damage", "amount" to fmt(bypassStream)))
            }
        } finally {
            reentrant = false
        }
        if (damage.isFinite() && damage >= 0.0 && damage != event.damage) {
            event.damage = damage
        }
        emitTrace(attacker as? Player, victim as? Player, trace)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDeath(event: EntityDeathEvent) {
        if (!AttributeService.isNativeActive()) return
        val dead = event.entity
        if (dead !is Player && AttributeStatStore.has(dead.uniqueId)) MobAttributeService.clear(dead)
        val killer = event.entity.killer ?: return
        val totals = AttributeStatStore.totals(killer.uniqueId)
        for (def in hookedFor(AttributeHooks.ON_KILL, totals)) {
            val ref = def.hook(AttributeHooks.ON_KILL) ?: continue
            AttributeScriptRunner.runEffectHook(ref, killer, effectVars(def, totals, "victim", event.entity))
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onExpChange(event: PlayerExpChangeEvent) {
        if (!AttributeService.isNativeActive() || event.amount <= 0) return
        val bonus = AttributeStatStore.total(event.player.uniqueId, "exp_bonus")
        if (bonus <= 0.0) return
        event.amount = Math.round(event.amount * (1.0 + bonus)).toInt()
    }

    private fun applyOffense(attacker: LivingEntity, victim: Entity?, type: DamageType, damage: Double, isProjectile: Boolean, trace: MutableList<String>): Double {
        var current = damage
        val totals = AttributeStatStore.totals(attacker.uniqueId)
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
            val rate = totals[type.critRateKey] ?: 0.0
            if (rate > 0.0 && ThreadLocalRandom.current().nextDouble() < rate) {
                val critDmg = totals[type.critDamageKey] ?: 0.0
                val before = current
                current *= (1.5 + critDmg)
                trace.add(Lang.get("attribute-combat-listener.crit", "type" to type.displayName, "before" to fmt(before), "current" to fmt(current)))
                AttributeRegistry.resolve(type.critRateKey)?.message?.let { msg ->
                    (attacker as? Player)?.let { TextUtil.sendColored(it, msg.replace("{damage}", fmt(current)).replace("{value}", fmt(rate))) }
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

    private fun applyDefense(victim: LivingEntity, attacker: Entity?, physical: Double, bypassStream: Double, trace: MutableList<String>): Double {
        val totals = AttributeStatStore.totals(victim.uniqueId)
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
        if (bypassPart > 0.0) trace.add(Lang.get("attribute-combat-listener.penetration-damage-typed", "amount" to fmt(bypassPart)))
        return physPart + bypassPart
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

    private fun elementalDamage(attackerTotals: Map<String, Double>, victim: Entity?): Double {
        val elements = ElementRegistry.all()
        if (elements.isEmpty()) return 0.0
        val vT = if (victim is Player) AttributeStatStore.totals(victim.uniqueId) else emptyMap()
        val targetElement = ElementRegistry.dominantElement(vT)
        var sum = 0.0
        for (elem in elements) {
            val flat = attackerTotals[elem.damageKey] ?: 0.0
            if (flat <= 0.0) continue
            val bonus = attackerTotals[elem.bonusKey] ?: 0.0
            val resist = (vT[elem.resistKey] ?: 0.0).coerceIn(0.0, 1.0)
            val restraint = ElementRegistry.restraintMultiplier(elem, targetElement)
            sum += flat * (1.0 + bonus) * (1.0 - resist) * restraint
        }
        return sum
    }

    private fun isPhysicalCombat(cause: EntityDamageEvent.DamageCause): Boolean =
        cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK ||
            cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK ||
            cause == EntityDamageEvent.DamageCause.PROJECTILE

    private fun hasAttributes(entity: LivingEntity): Boolean =
        entity is Player || AttributeStatStore.has(entity.uniqueId)

    private fun sendAttrMessage(entity: LivingEntity, def: AttributeDef, value: Double, damage: Double) {
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
