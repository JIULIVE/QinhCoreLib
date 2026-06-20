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

    private val miniMessageTagPattern =
        Regex("</?[a-z0-9_:#'!@\\-]+>", RegexOption.IGNORE_CASE)

    private val legacyCodeMap = mapOf(
        '0' to "black", '1' to "dark_blue", '2' to "dark_green", '3' to "dark_aqua",
        '4' to "dark_red", '5' to "dark_purple", '6' to "gold", '7' to "gray",
        '8' to "dark_gray", '9' to "blue", 'a' to "green", 'b' to "aqua",
        'c' to "red", 'd' to "light_purple", 'e' to "yellow", 'f' to "white",
        'k' to "obfuscated", 'l' to "bold", 'm' to "strikethrough",
        'n' to "underlined", 'o' to "italic", 'r' to "reset",
    )

    fun colored(text: String): String = legacySerializer.serialize(toComponent(text))

    fun toComponent(text: String): Component {
        val normalized = text.replace('§', '&')
        val hasLegacy = normalized.contains('&')
        val hasMini = miniMessageTagPattern.containsMatchIn(text)
        val component = when {
            hasLegacy && hasMini -> {
                try {
                    miniMessage.deserialize(legacyToMiniMessage(normalized))
                } catch (_: Exception) {
                    legacyAmpersandSerializer.deserialize(normalized)
                }
            }
            hasLegacy -> legacyAmpersandSerializer.deserialize(normalized)
            hasMini -> {
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

    private fun legacyToMiniMessage(input: String): String {
        val sb = StringBuilder(input.length + 16)
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c == '&' && i + 1 < input.length) {
                val next = input[i + 1]
                if (next == '#' && i + 8 <= input.length) {
                    val hex = input.substring(i + 2, i + 8)
                    if (hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                        sb.append("<#").append(hex).append('>')
                        i += 8
                        continue
                    }
                }
                val tag = legacyCodeMap[next.lowercaseChar()]
                if (tag != null) {
                    sb.append('<').append(tag).append('>')
                    i += 2
                    continue
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString()
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
