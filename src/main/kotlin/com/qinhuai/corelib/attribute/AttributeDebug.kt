package com.qinhuai.corelib.attribute

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object AttributeDebug {

    private val watching = ConcurrentHashMap.newKeySet<UUID>()

    fun toggle(player: UUID): Boolean =
        if (watching.remove(player)) false else { watching.add(player); true }

    fun isOn(player: UUID): Boolean = watching.contains(player)

    fun clear(player: UUID) {
        watching.remove(player)
    }
}
