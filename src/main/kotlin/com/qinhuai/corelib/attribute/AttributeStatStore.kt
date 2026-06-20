package com.qinhuai.corelib.attribute

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object AttributeStatStore {

    private val data = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Map<String, Double>>>()

    fun set(player: UUID, source: String, values: Map<String, Double>) {
        val bySource = data.getOrPut(player) { ConcurrentHashMap() }
        if (values.isEmpty()) bySource.remove(source) else bySource[source] = HashMap(values)
        if (bySource.isEmpty()) data.remove(player)
    }

    fun removeSource(player: UUID, source: String) {
        val bySource = data[player] ?: return
        bySource.remove(source)
        if (bySource.isEmpty()) data.remove(player)
    }

    fun clear(player: UUID) {
        data.remove(player)
    }

    fun has(player: UUID): Boolean = data[player]?.isNotEmpty() == true

    fun total(player: UUID, attrKey: String): Double {
        val bySource = data[player] ?: return 0.0
        var sum = 0.0
        for (values in bySource.values) sum += values[attrKey] ?: 0.0
        return AttributeRegistry.resolve(attrKey)?.clamp(sum) ?: sum
    }

    fun activeAttrs(player: UUID): Set<String> {
        val bySource = data[player] ?: return emptySet()
        val keys = LinkedHashSet<String>()
        for (values in bySource.values) keys.addAll(values.keys)
        return keys
    }

    fun snapshot(player: UUID): Map<String, Map<String, Double>> {
        val bySource = data[player] ?: return emptyMap()
        val out = LinkedHashMap<String, Map<String, Double>>()
        for ((source, values) in bySource) out[source] = LinkedHashMap(values)
        return out
    }

    fun totals(player: UUID): Map<String, Double> {
        val bySource = data[player] ?: return emptyMap()
        val out = LinkedHashMap<String, Double>()
        for (values in bySource.values) {
            for ((k, v) in values) out[k] = (out[k] ?: 0.0) + v
        }
        val clamped = LinkedHashMap<String, Double>(out.size)
        for ((k, v) in out) clamped[k] = AttributeRegistry.resolve(k)?.clamp(v) ?: v
        return clamped
    }
}
