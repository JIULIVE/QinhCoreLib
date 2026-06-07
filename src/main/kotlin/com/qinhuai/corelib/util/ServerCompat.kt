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
import java.util.logging.Logger

/**
 * Paper / Purpur / Spigot 兼容与版本校验（MC 1.21.11+，Java 25+；Purpur/Paper 26.1 服务端需 JDK 25）。
 */
object ServerCompat {

    const val MIN_JAVA_VERSION = 25
    const val MIN_MC_PATCH = 11

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

    /** Paper/Purpur 聊天事件；纯 Spigot 上为 false。 */
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

    fun validateServer(logger: Logger): String? {
        val javaError = validateJava()
        if (javaError != null) return javaError

        val mcError = validateMinecraftVersion()
        if (mcError != null) return mcError

        logger.info(
            "[QinhCoreLib] ${platformLabel()} | MC ${bukkitVersionLabel()} | Java ${Runtime.version().feature()}",
        )
        warnIfKotlinLibrariesUnsupported(logger)
        return null
    }

    /** Kotlin 通过 plugin.yml libraries 加载，纯 Spigot 无此能力。 */
    fun warnIfKotlinLibrariesUnsupported(logger: Logger) {
        if (!supportsPluginLibraries) {
            logger.warning(
                "[Qinh系列] 当前为 ${platformLabel()}，不支持 plugin.yml libraries；" +
                    "秦淮 Kotlin 插件需 Paper / Purpur，否则可能无法启动。",
            )
        }
    }

    fun validateJava(): String? {
        val feature = Runtime.version().feature()
        return if (feature < MIN_JAVA_VERSION) {
            "需要 Java $MIN_JAVA_VERSION 或更高版本（Purpur 26.1 需 JDK 25；当前: $feature）"
        } else {
            null
        }
    }

    fun validateMinecraftVersion(): String? {
        val parts = parseBukkitVersion() ?: return "无法识别服务端版本，需要 Minecraft 1.21.$MIN_MC_PATCH 或更高"
        val (major, minor, patch) = parts
        if (major < 1) return "需要 Minecraft 1.21.$MIN_MC_PATCH+（当前: ${bukkitVersionLabel()}）"
        if (major == 1 && minor < 21) {
            return "需要 Minecraft 1.21.$MIN_MC_PATCH+（当前: ${bukkitVersionLabel()}）"
        }
        if (major == 1 && minor == 21 && patch < MIN_MC_PATCH) {
            return "需要 Minecraft 1.21.$MIN_MC_PATCH+（当前: ${bukkitVersionLabel()}）"
        }
        return null
    }

    /** @return (major, minor, patch) 例如 1.21.11 → (1, 21, 11) */
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
            // 部分 Spigot 构建未暴露自定义堆叠
        }
    }

    /** 将配置中的枚举名（如 ENTITY_PLAYER_LEVELUP）转为 registry 键并解析。 */
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
