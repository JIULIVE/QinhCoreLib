package com.qinhuai.corelib.script

import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

data class ScriptContext(
    val plugin: Plugin,
    val player: Player? = null,
    val variables: MutableMap<String, Any> = mutableMapOf(),
    val silent: Boolean = false,
)
