package com.qinhuai.corelib.debug

object BridgeStatusRegistry {
    private val bridges = linkedMapOf<String, BridgeStatus>()

    fun register(status: BridgeStatus) {
        bridges[status.name.lowercase()] = status
    }

    fun unregister(name: String) {
        bridges.remove(name.lowercase())
    }

    fun get(name: String): BridgeStatus? = bridges[name.lowercase()]

    fun all(): List<BridgeStatus> = bridges.values.toList()

    fun clear() {
        bridges.clear()
    }
}
