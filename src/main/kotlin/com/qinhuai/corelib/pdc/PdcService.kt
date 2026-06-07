package com.qinhuai.corelib.pdc

import com.qinhuai.corelib.QinhCoreLib
import com.qinhuai.corelib.debug.BridgeStatus
import com.qinhuai.corelib.debug.BridgeStatusRegistry
import com.qinhuai.corelib.debug.DiagnosticResult
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

class PdcService(private val namespace: String) {

    fun createKey(key: String): NamespacedKey {
        return NamespacedKey(QinhCoreLib.instance, "${namespace}_$key")
    }

    fun <T : Any, Z : Any> set(
        container: PersistentDataContainer,
        key: String,
        type: PersistentDataType<T, Z>,
        value: Z,
    ) {
        container.set(createKey(key), type, value)
    }

    fun <T : Any, Z : Any> get(
        container: PersistentDataContainer,
        key: String,
        type: PersistentDataType<T, Z>,
    ): Z? {
        return container.get(createKey(key), type)
    }

    fun has(container: PersistentDataContainer, key: String): Boolean {
        return container.has(createKey(key))
    }

    fun remove(container: PersistentDataContainer, key: String) {
        container.remove(createKey(key))
    }

    fun setString(container: PersistentDataContainer, key: String, value: String) {
        set(container, key, PersistentDataType.STRING, value)
    }

    fun getString(container: PersistentDataContainer, key: String): String? {
        return get(container, key, PersistentDataType.STRING)
    }

    fun setInt(container: PersistentDataContainer, key: String, value: Int) {
        set(container, key, PersistentDataType.INTEGER, value)
    }

    fun getInt(container: PersistentDataContainer, key: String): Int? {
        return get(container, key, PersistentDataType.INTEGER)
    }

    fun getIntOrDefault(container: PersistentDataContainer, key: String, default: Int): Int {
        return getInt(container, key) ?: default
    }

    fun setLong(container: PersistentDataContainer, key: String, value: Long) {
        set(container, key, PersistentDataType.LONG, value)
    }

    fun getLong(container: PersistentDataContainer, key: String): Long? {
        return get(container, key, PersistentDataType.LONG)
    }

    fun setDouble(container: PersistentDataContainer, key: String, value: Double) {
        set(container, key, PersistentDataType.DOUBLE, value)
    }

    fun getDouble(container: PersistentDataContainer, key: String): Double? {
        return get(container, key, PersistentDataType.DOUBLE)
    }

    fun setBoolean(container: PersistentDataContainer, key: String, value: Boolean) {
        setInt(container, key, if (value) 1 else 0)
    }

    fun getBoolean(container: PersistentDataContainer, key: String): Boolean? {
        return getInt(container, key)?.let { it == 1 }
    }

    fun getBooleanOrDefault(container: PersistentDataContainer, key: String, default: Boolean): Boolean {
        return getBoolean(container, key) ?: default
    }

    fun bridgeStatus(): BridgeStatus = BridgeStatus(
        name = "PDC",
        available = true,
        enabled = true,
        source = namespace,
        message = "PDC 服务可用",
        recoverable = true,
    )

    fun diagnose(): DiagnosticResult<BridgeStatus> = DiagnosticResult.ok(bridgeStatus(), source = "pdc")
}

object PdcServiceManager {
    private val serviceInstances = mutableMapOf<String, PdcService>()

    fun get(namespace: String): PdcService {
        return serviceInstances.getOrPut(namespace) { PdcService(namespace) }
    }

    fun allNamespaces(): List<String> = serviceInstances.keys.toList()

    fun diagnoseAll(): DiagnosticResult<List<BridgeStatus>> = DiagnosticResult.ok(
        serviceInstances.values.map { it.bridgeStatus() },
        source = "pdc",
    )

    fun registerStatus(namespace: String) {
        BridgeStatusRegistry.register(get(namespace).bridgeStatus())
    }
}
