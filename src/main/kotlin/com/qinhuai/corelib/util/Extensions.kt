package com.qinhuai.corelib.util

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

fun String.toComponent(): Component = TextUtil.toComponent(this)

fun String.colored(): String = TextUtil.colored(this)

fun Player.sendColoredMessage(message: String) = TextUtil.sendColored(this, message)

fun String.parseMiniMessage(): Component = TextUtil.toComponent(this)

fun String.replacePlaceholders(vararg pairs: Pair<String, Any>): String {
    var result = this
    pairs.forEach { (key, value) ->
        result = result.replace("{$key}", value.toString())
    }
    return result
}
