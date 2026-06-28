package com.qinhuai.corelib.util

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.attribute.Attribute
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffectType
import com.qinhuai.corelib.lang.Lang
import java.util.logging.Logger

object ServerCompat {

    const val MIN_JAVA_VERSION = 25
    const val MIN_MC_PATCH = 11

    const val MAX_TESTED_MAJOR = 1
    const val MAX_TESTED_MINOR = 21
    val MAX_TESTED_LABEL = "1.21.x"

    val ATTR_MAX_HEALTH: Attribute = attribute("max_health", "generic.max_health")
    val ATTR_ATTACK_DAMAGE: Attribute = attribute("attack_damage", "generic.attack_damage")
    val ATTR_FOLLOW_RANGE: Attribute = attribute("follow_range", "generic.follow_range")
    val ATTR_KNOCKBACK_RESISTANCE: Attribute = attribute("knockback_resistance", "generic.knockback_resistance")
    val ATTR_MOVEMENT_SPEED: Attribute = attribute("movement_speed", "generic.movement_speed")
    val ATTR_ARMOR: Attribute = attribute("armor", "generic.armor")

    val ENCHANT_UNBREAKING: Enchantment = Enchantment.UNBREAKING

    enum class PlatformKind {
        PAPER,
        PURPUR,
        SPIGOT,
        CRAFTBUKKIT,
        UNKNOWN,
    }

    val platform: PlatformKind by lazy { detectPlatform() }

    val supportsPluginLibraries: Boolean by lazy {
        platform == PlatformKind.PAPER || platform == PlatformKind.PURPUR
    }

    val supportsAsyncChatEvent: Boolean by lazy {
        classExists("io.papermc.paper.event.player.AsyncChatEvent")
    }

    fun detectPlatform(): PlatformKind {
        return when {
            classExists("com.destroystokyo.paper.PaperConfig") ||
                classExists("io.papermc.paper.PaperBootstrap") -> PlatformKind.PAPER
            classExists("org.purpurmc.purpur.PurpurConfig") -> PlatformKind.PURPUR
            classExists("org.spigotmc.SpigotConfig") -> PlatformKind.SPIGOT
            classExists("org.bukkit.craftbukkit.CraftServer") -> PlatformKind.CRAFTBUKKIT
            else -> PlatformKind.UNKNOWN
        }
    }

    private fun classExists(name: String): Boolean =
        try {
            Class.forName(name)
            true
        } catch (_: ClassNotFoundException) {
            false
        }

    fun platformLabel(): String = when (platform) {
        PlatformKind.PAPER -> "Paper"
        PlatformKind.PURPUR -> "Purpur"
        PlatformKind.SPIGOT -> "Spigot"
        PlatformKind.CRAFTBUKKIT -> "CraftBukkit"
        PlatformKind.UNKNOWN -> "Bukkit"
    }

    fun validateServer(logger: Logger, strictVersionCheck: Boolean = false): String? {
        val javaError = validateJava()
        if (javaError != null) return javaError

        val mcError = validateMinecraftVersion(logger, strictVersionCheck)
        if (mcError != null) return mcError

        logger.info(
            "[QinhCoreLib] ${platformLabel()} | MC ${bukkitVersionLabel()} | Java ${Runtime.version().feature()}",
        )
        warnIfKotlinLibrariesUnsupported(logger)
        return null
    }

    fun warnIfKotlinLibrariesUnsupported(logger: Logger) {
        if (!supportsPluginLibraries) {
            logger.warning(
                Lang.get("server-compat.kotlin-libraries-unsupported", "platform" to platformLabel()),
            )
        }
    }

    fun validateJava(): String? {
        val feature = Runtime.version().feature()
        return if (feature < MIN_JAVA_VERSION) {
            Lang.get("server-compat.java-version-too-low", "min" to MIN_JAVA_VERSION, "current" to feature)
        } else {
            null
        }
    }

    fun validateMinecraftVersion(logger: Logger, strictVersionCheck: Boolean = false): String? {
        val parts = parseBukkitVersion() ?: return null
        val (major, minor, patch) = parts
        val tooOld = major < 1 ||
            (major == 1 && minor < 21) ||
            (major == 1 && minor == 21 && patch < MIN_MC_PATCH)
        if (tooOld) {
            return Lang.get("server-compat.mc-version-too-low", "patch" to MIN_MC_PATCH, "current" to bukkitVersionLabel())
        }
        val beyondTested = major > MAX_TESTED_MAJOR ||
            (major == MAX_TESTED_MAJOR && minor > MAX_TESTED_MINOR)
        if (beyondTested) {
            if (strictVersionCheck) {
                return Lang.get("server-compat.mc-untested-strict", "current" to bukkitVersionLabel(), "tested" to MAX_TESTED_LABEL)
            }
            logger.warning(Lang.get("server-compat.mc-untested-warn", "current" to bukkitVersionLabel(), "tested" to MAX_TESTED_LABEL))
        }
        return null
    }

    fun parseBukkitVersion(): Triple<Int, Int, Int>? {
        val raw = bukkitVersionLabel()
        val core = raw.substringBefore('-').substringBefore('_')
        val segments = core.split('.')
        if (segments.size < 2) return null
        val major = segments[0].toIntOrNull() ?: return null
        val minor = segments[1].toIntOrNull() ?: return null
        val patch = segments.getOrNull(2)?.toIntOrNull() ?: 0
        return Triple(major, minor, patch)
    }

    fun bukkitVersionLabel(): String =
        org.bukkit.Bukkit.getBukkitVersion()

    fun pluginVersion(plugin: Plugin): String =
        plugin.pluginMeta.version

    fun applyMaxStackSize(meta: ItemMeta, size: Int) {
        try {
            val method = meta.javaClass.getMethod("setMaxStackSize", Int::class.javaPrimitiveType)
            method.invoke(meta, size)
        } catch (_: ReflectiveOperationException) {
        }
    }

    fun resolveSound(name: String): Sound? =
        registryGet(Registry.SOUNDS, *legacyNameCandidates(name))

    fun resolvePotionEffectType(name: String): PotionEffectType? =
        registryGet(Registry.POTION_EFFECT_TYPE, *legacyNameCandidates(name))

    fun sound(vararg names: String): Sound =
        names.firstNotNullOfOrNull { resolveSound(it) } ?: Sound.BLOCK_STONE_BREAK

    fun particle(vararg names: String): Particle {
        for (name in names) {
            registryGet(Registry.PARTICLE_TYPE, *legacyNameCandidates(name))?.let { return it }
            try {
                return Particle.valueOf(name)
            } catch (_: IllegalArgumentException) {
            }
        }
        return Particle.FLAME
    }

    fun material(vararg names: String): Material {
        for (name in names) {
            registryGet(Registry.MATERIAL, *legacyNameCandidates(name))?.let { return it }
            try {
                return Material.valueOf(name)
            } catch (_: IllegalArgumentException) {
            }
        }
        return Material.STONE
    }

    fun playBlockStepEffect(world: World?, location: org.bukkit.Location, material: Material) {
        if (world == null) return
        world.spawnParticle(
            Particle.BLOCK,
            location.clone().add(0.5, 0.05, 0.5),
            4,
            0.15,
            0.0,
            0.15,
            0.01,
            material.createBlockData(),
        )
    }

    private fun attribute(vararg keyCandidates: String): Attribute {
        for (key in keyCandidates) {
            registryGet(Registry.ATTRIBUTE, key)?.let { return it }
        }
        error("No Attribute registry key for: ${keyCandidates.joinToString()}")
    }

    private fun legacyNameCandidates(name: String): Array<String> {
        val dot = name.lowercase().replace('_', '.')
        return if (dot.startsWith("generic.")) {
            arrayOf(dot)
        } else {
            arrayOf(dot, "generic.$dot", "minecraft:$dot", "minecraft:generic.$dot")
        }
    }

    private fun <T : org.bukkit.Keyed> registryGet(registry: Registry<T>, vararg keys: String): T? {
        for (raw in keys) {
            val key = if (raw.contains(':')) {
                NamespacedKey.fromString(raw)
            } else {
                NamespacedKey.minecraft(raw)
            } ?: continue
            registry.get(key)?.let { return it }
        }
        return null
    }
}
