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
        if (leechFromElemental && elementFinal > 0.0) (attacker as? Player)?.let { runMagicDamageHooks(it, elementFinal, victim) }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMagicDamage(event: EntityDamageByEntityEvent) {
        if (!AttributeService.isNativeActive()) return
        if (event.cause !in magicCauses && !(isolateMlSkill && MythicLibDamageBridge.isSkillDamage(event))) return
        val attacker = resolveAttacker(event.damager) as? Player ?: return
        if (!hasAttributes(attacker)) return
        runMagicDamageHooks(attacker, event.finalDamage, event.entity)
    }

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

