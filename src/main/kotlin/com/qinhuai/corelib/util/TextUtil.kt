package com.qinhuai.corelib.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.java.JavaPlugin
import java.time.Duration

object TextUtil {
    private val miniMessage = MiniMessage.miniMessage()
    private val legacySerializer = LegacyComponentSerializer.legacySection()
    private val legacyAmpersandSerializer = LegacyComponentSerializer.legacyAmpersand()

    /** 真实 MiniMessage 标签（避免把 `<ID/all>` 等说明文字误判为标签） */
    private val miniMessageTagPattern =
        Regex("</?[a-z0-9_:#'!@\\-]+>", RegexOption.IGNORE_CASE)

    fun colored(text: String): String = legacySerializer.serialize(toComponent(text))

    fun toComponent(text: String): Component {
        val normalized = text.replace('§', '&')
        val component = when {
            normalized.contains('&') -> legacyAmpersandSerializer.deserialize(normalized)
            miniMessageTagPattern.containsMatchIn(text) -> {
                try {
                    miniMessage.deserialize(text)
                } catch (_: Exception) {
                    Component.text(text)
                }
            }
            else -> Component.text(text)
        }
        return component.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
    }

    fun applyItemDisplay(meta: ItemMeta, name: String?, lore: List<String>) {
        name?.let { meta.displayName(toComponent(it)) }
        if (lore.isNotEmpty()) {
            meta.lore(lore.map { toComponent(it) })
        }
    }

    fun sendColored(sender: CommandSender, message: String) {
        sender.sendMessage(toComponent(message))
    }

    /** Paper 控制台/日志 — {@code logger.info("§a…")} 不会上色，需走 ComponentLogger */
    fun logColored(plugin: JavaPlugin, message: String) {
        plugin.componentLogger.info(toComponent(message))
    }

    fun broadcastColored(message: String) {
        Bukkit.getServer().broadcast(toComponent(message))
    }

    fun showColoredTitle(
        player: Player,
        text: String,
        fadeIn: Int = 10,
        stay: Int = 60,
        fadeOut: Int = 10,
    ) {
        player.showTitle(
            Title.title(
                toComponent(text),
                Component.empty(),
                Title.Times.times(
                    Duration.ofMillis(fadeIn * 50L),
                    Duration.ofMillis(stay * 50L),
                    Duration.ofMillis(fadeOut * 50L),
                ),
            ),
        )
    }
}

fun Player.maxHealthValue(): Double =
    getAttribute(ServerCompat.ATTR_MAX_HEALTH)?.value ?: 20.0
