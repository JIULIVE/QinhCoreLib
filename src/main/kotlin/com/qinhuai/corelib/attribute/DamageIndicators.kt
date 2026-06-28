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

