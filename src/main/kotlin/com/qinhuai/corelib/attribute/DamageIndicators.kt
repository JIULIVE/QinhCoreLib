package com.qinhuai.corelib.attribute

import com.qinhuai.corelib.QinhCoreLib
import com.qinhuai.corelib.combat.QinhDamageEvent
import com.qinhuai.corelib.util.TextUtil
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Display
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Transformation
import org.bukkit.util.Vector
import org.joml.AxisAngle4f
import org.joml.Vector3f
import java.text.DecimalFormat
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

object DamageIndicators : Listener {

    private val pdcKey = NamespacedKey("qinhcorelib", "indicator")

    private var enabled = true
    private var showOnPlayers = true
    private var minDamage = 0.5
    private var lifespanTicks = 20L
    private var tickPeriod = 3L
    private var gravity = 1.0
    private var initialUpwardVelocity = 0.5
    private var radialVelocity = 1.0
    private var spawnHeightFraction = 0.55
    private var normalScale = 1.2f
    private var critScale = 1.7f
    private var normalFormat = "{icon} &f{value}"
    private var critFormat = "{icon} &c&l{value}"
    private var healFormat = "{icon} &a&l{value}"
    private var elementFormat = "{icon} {color}{value}"
    private var mergeElements = true
    private var icons = HashMap<String, Pair<String, String>>()
    private var decimalFormat = DecimalFormat("0.#")

    private class CombatHint(
        val at: Long,
        val crit: Boolean,
        val styleKey: String,
        val packets: List<com.qinhuai.corelib.combat.DamagePacket>,
    )

    private val hints = ConcurrentHashMap<UUID, CombatHint>()

    private val defaultIcons = mapOf(
        "physical" to ("&f⚔" to "&c&l⚔"),
        "magic" to ("&b✦" to "&b&l✦"),
        "fire" to ("&6♨" to "&6&l♨"),
        "heal" to ("&a✚" to "&a&l✚"),
        "other" to ("&7◆" to "&7&l◆"),
    )

    fun reload() {
        val cfg = QinhCoreLib.instance.config
        val root = "combat.damage-indicators"
        enabled = cfg.getBoolean("$root.enabled", true)
        showOnPlayers = cfg.getBoolean("$root.show-on-players", true)
        minDamage = cfg.getDouble("$root.min-damage", 0.5)
        lifespanTicks = cfg.getLong("$root.lifespan", 20L).coerceIn(4L, 200L)
        tickPeriod = cfg.getLong("$root.tick-period", 3L).coerceIn(1L, 20L)
        gravity = cfg.getDouble("$root.gravity", 1.0)
        initialUpwardVelocity = cfg.getDouble("$root.initial-upward-velocity", 0.5)
        radialVelocity = cfg.getDouble("$root.radial-velocity", 1.0)
        spawnHeightFraction = cfg.getDouble("$root.spawn-height", 0.55).coerceIn(0.0, 1.2)
        normalScale = cfg.getDouble("$root.scale", 1.2).coerceIn(0.3, 5.0).toFloat()
        critScale = cfg.getDouble("$root.crit-scale", 1.7).coerceIn(0.3, 6.0).toFloat()
        normalFormat = cfg.getString("$root.format", "{icon} &f{value}") ?: "{icon} &f{value}"
        critFormat = cfg.getString("$root.crit-format", "{icon} &c&l{value}") ?: "{icon} &c&l{value}"
        healFormat = cfg.getString("$root.heal-format", "{icon} &a&l{value}") ?: "{icon} &a&l{value}"
        elementFormat = cfg.getString("$root.element-format", "{icon} {color}{value}") ?: "{icon} {color}{value}"
        mergeElements = cfg.getBoolean("$root.merge-elements", true)
        icons = HashMap()
        val section = cfg.getConfigurationSection("$root.icons")
        for ((key, def) in defaultIcons) {
            val sub = section?.getConfigurationSection(key)
            val normal = sub?.getString("normal") ?: def.first
            val crit = sub?.getString("crit") ?: def.second
            icons[key] = normal to crit
        }
        decimalFormat = runCatching { DecimalFormat(cfg.getString("$root.decimal-format", "0.#")) }
            .getOrDefault(DecimalFormat("0.#"))
    }

    fun isEnabled(): Boolean = enabled

    /** 监听 CoreLib 战斗总线：记录本次伤害的暴击与类型，供随后的飘字使用（取代旧的 markCrit 硬调用）。 */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onQinhDamage(event: QinhDamageEvent) {
        if (!enabled) return
        val m = event.metadata
        hints[m.victim.uniqueId] = CombatHint(System.currentTimeMillis(), m.crit, styleKeyOf(m.damageType), m.packets)
    }

    fun clearMark(victim: UUID) {
        hints.remove(victim)
    }

    fun clearOrphans() {
        for (world in Bukkit.getWorlds()) {
            for (display in world.getEntitiesByClass(TextDisplay::class.java)) {
                if (display.persistentDataContainer.has(pdcKey, PersistentDataType.BYTE)) display.remove()
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDamage(event: EntityDamageEvent) {
        if (!enabled) return
        val victim = event.entity as? LivingEntity ?: return
        if (victim is Player && !showOnPlayers) {
            hints.remove(victim.uniqueId)
            return
        }
        val amount = event.finalDamage
        if (!amount.isFinite() || amount < minDamage) {
            hints.remove(victim.uniqueId)
            return
        }
        val hint = consumeHint(victim.uniqueId)
        val packets = hint?.packets ?: emptyList()
        if (mergeElements && packets.any { it.element != null }) {
            spawnMerged(victim, amount, packets, directionFrom(event))
            return
        }
        val crit = hint?.crit ?: false
        val key = hint?.styleKey ?: styleKey(event.cause)
        val template = if (crit) critFormat else normalFormat
        val scale = if (crit) critScale else normalScale
        spawn(victim, amount, template, iconFor(key, crit), scale, directionFrom(event))
    }

    private fun spawnMerged(victim: LivingEntity, amount: Double, packets: List<com.qinhuai.corelib.combat.DamagePacket>, direction: Vector) {
        val rawSum = packets.sumOf { it.value }
        if (rawSum <= 0.0) return
        val scale = amount / rawSum
        val parts = mutableListOf<String>()
        for (p in packets) {
            val v = p.value * scale
            if (v < minDamage) continue
            parts += renderPacket(p, v)
        }
        if (parts.isEmpty()) return
        val anyCrit = packets.any { it.crit }
        spawnText(victim, parts.joinToString(" "), if (anyCrit) critScale else normalScale, direction)
    }

    private fun renderPacket(p: com.qinhuai.corelib.combat.DamagePacket, value: Double): String {
        val icon = iconFor(p.styleKey, p.crit)
        val color = if (p.element != null) ElementRegistry.get(p.element)?.color ?: "" else ""
        val template = if (p.element != null) elementFormat else if (p.crit) critFormat else normalFormat
        return renderFormat(value, template, icon, color)
    }

    fun showHeal(target: LivingEntity, amount: Double) {
        if (!enabled || amount < minDamage) return
        spawn(target, amount, healFormat, iconFor("heal", false), normalScale, randomDirection())
    }

    fun show(victim: LivingEntity, amount: Double, template: String, icon: String = "", scale: Float = normalScale, direction: Vector? = null) {
        if (!enabled) return
        spawn(victim, amount, template, icon, scale, direction ?: randomDirection())
    }

    private fun iconFor(key: String, crit: Boolean): String {
        val pair = icons[key] ?: icons["other"] ?: return ""
        return if (crit) pair.second else pair.first
    }

    private fun spawn(victim: LivingEntity, amount: Double, template: String, icon: String, scale: Float, direction: Vector) {
        spawnText(victim, renderFormat(amount, template, icon), scale, direction)
    }

    private fun spawnText(victim: LivingEntity, text: String, scale: Float, direction: Vector) {
        val world = victim.world
        val box = victim.boundingBox
        val width = (box.widthX + box.widthZ) / 2.0
        val angle = ThreadLocalRandom.current().nextDouble() * 2.0 * Math.PI
        val r = 0.05 + width * 0.5
        val h = victim.height * spawnHeightFraction
        val loc = victim.location.clone().add(Math.cos(angle) * r, h, Math.sin(angle) * r)

        val display = world.spawn(loc, TextDisplay::class.java) { td ->
            td.persistentDataContainer.set(pdcKey, PersistentDataType.BYTE, 1.toByte())
            td.text(TextUtil.toComponent(text))
            td.billboard = Display.Billboard.CENTER
            td.isSeeThrough = true
            td.isDefaultBackground = false
            td.backgroundColor = Color.fromARGB(0, 0, 0, 0)
            td.brightness = Display.Brightness(15, 15)
            td.isPersistent = false
            td.teleportDuration = tickPeriod.toInt().coerceIn(1, 59)
            td.transformation = Transformation(
                Vector3f(0f, 0f, 0f),
                AxisAngle4f(0f, 0f, 0f, 1f),
                Vector3f(scale, scale, scale),
                AxisAngle4f(0f, 0f, 0f, 1f),
            )
        }
        flyOut(display, direction)
    }

    private fun flyOut(display: TextDisplay, dir: Vector) {
        val dt = tickPeriod / 20.0
        val acc = -10.0 * gravity
        object : BukkitRunnable() {
            val loc: Location = display.location.clone()
            var v = 2.5 * initialUpwardVelocity
            var ticks = 0L

            override fun run() {
                if (!display.isValid) {
                    cancel()
                    return
                }
                if (ticks == 0L) dir.multiply(1.2 * radialVelocity)
                if (ticks++ * tickPeriod >= lifespanTicks) {
                    display.remove()
                    cancel()
                    return
                }
                v += acc * dt
                loc.add(dir.x * dt, v * dt, dir.z * dt)
                display.teleport(loc)
            }
        }.runTaskTimer(QinhCoreLib.instance, 0L, tickPeriod)
    }

    private fun styleKey(cause: EntityDamageEvent.DamageCause): String = when (cause) {
        EntityDamageEvent.DamageCause.ENTITY_ATTACK,
        EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK,
        EntityDamageEvent.DamageCause.PROJECTILE,
        EntityDamageEvent.DamageCause.THORNS,
        -> "physical"
        EntityDamageEvent.DamageCause.MAGIC,
        EntityDamageEvent.DamageCause.POISON,
        EntityDamageEvent.DamageCause.WITHER,
        EntityDamageEvent.DamageCause.DRAGON_BREATH,
        EntityDamageEvent.DamageCause.SONIC_BOOM,
        -> "magic"
        EntityDamageEvent.DamageCause.FIRE,
        EntityDamageEvent.DamageCause.FIRE_TICK,
        EntityDamageEvent.DamageCause.LAVA,
        EntityDamageEvent.DamageCause.HOT_FLOOR,
        -> "fire"
        else -> "other"
    }

    private fun directionFrom(event: EntityDamageEvent): Vector {
        if (event is EntityDamageByEntityEvent) {
            val dir = event.entity.location.toVector()
                .subtract(event.damager.location.toVector())
                .setY(0.0)
            if (dir.lengthSquared() > 0) {
                var a = Math.atan2(dir.z, dir.x)
                a += Math.PI / 2.0 * (ThreadLocalRandom.current().nextDouble() - 0.5)
                return Vector(Math.cos(a), 0.0, Math.sin(a))
            }
        }
        return randomDirection()
    }

    private fun randomDirection(): Vector {
        val a = ThreadLocalRandom.current().nextDouble() * Math.PI * 2.0
        return Vector(Math.cos(a), 0.0, Math.sin(a))
    }

    private fun consumeHint(victim: UUID): CombatHint? {
        val h = hints.remove(victim) ?: return null
        return if (System.currentTimeMillis() - h.at <= 200L) h else null
    }

    private fun styleKeyOf(type: com.qinhuai.corelib.attribute.DamageType): String = when (type) {
        DamageType.PHYSICAL -> "physical"
        DamageType.MAGIC, DamageType.SKILL -> "magic"
        DamageType.TRUE -> "other"
    }

    private fun renderFormat(amount: Double, template: String, icon: String, color: String = ""): String {
        val value = decimalFormat.format(amount)
        var out = template
        out = if (icon.isBlank()) out.replace("{icon} ", "").replace("{icon}", "") else out.replace("{icon}", icon)
        out = out.replace("{color}", color)
        return out.replace("{value}", value)
    }
}
