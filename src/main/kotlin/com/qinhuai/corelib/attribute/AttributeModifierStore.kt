package com.qinhuai.corelib.attribute

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object AttributeModifierStore {

    private val data = ConcurrentHashMap<UUID, CopyOnWriteArrayList<StatModifier>>()

    private class FoldEntry(
        val base: Map<String, Double>,
        val result: Map<String, Double>,
        val earliestExpiry: Long,
    )

    private val foldCache = ConcurrentHashMap<UUID, ConcurrentHashMap<ActionHand, FoldEntry>>()

    private fun invalidate(holder: UUID) {
        foldCache.remove(holder)
    }

    private fun earliestExpiry(holder: UUID, now: Long): Long {
        val list = data[holder] ?: return Long.MAX_VALUE
        var earliest = Long.MAX_VALUE
        for (m in list) {
            val exp = m.expireAtMillis ?: continue
            if (exp > now && exp < earliest) earliest = exp
        }
        return earliest
    }

    fun add(holder: UUID, modifier: StatModifier): String {
        data.getOrPut(holder) { CopyOnWriteArrayList() }.add(modifier)
        invalidate(holder)
        return modifier.id
    }

    fun addTimed(
        holder: UUID,
        key: String,
        amount: Double,
        operation: ModifierOp,
        source: String,
        durationTicks: Long,
    ): String {
        val expire = if (durationTicks > 0) System.currentTimeMillis() + durationTicks * 50L else null
        return add(holder, StatModifier(key, amount, operation, source, expire))
    }

    fun remove(holder: UUID, id: String): Boolean {
        val list = data[holder] ?: return false
        val removed = list.removeIf { it.id == id }
        if (list.isEmpty()) data.remove(holder)
        if (removed) invalidate(holder)
        return removed
    }

    fun removeSource(holder: UUID, source: String): Boolean {
        val list = data[holder] ?: return false
        val removed = list.removeIf { it.source == source }
        if (list.isEmpty()) data.remove(holder)
        if (removed) invalidate(holder)
        return removed
    }

    fun removeSourceKey(holder: UUID, source: String, key: String): Boolean {
        val list = data[holder] ?: return false
        val removed = list.removeIf { it.source == source && it.key == key }
        if (list.isEmpty()) data.remove(holder)
        if (removed) invalidate(holder)
        return removed
    }

    fun clear(holder: UUID) {
        data.remove(holder)
        invalidate(holder)
    }

    fun has(holder: UUID): Boolean = data[holder]?.isNotEmpty() == true

    fun combine(holder: UUID, key: String, base: Double): Double =
        combine(holder, key, base, ActionHand.ANY)

    fun combine(holder: UUID, key: String, base: Double, hand: ActionHand): Double {
        val list = data[holder] ?: return base
        val now = System.currentTimeMillis()
        var flat = 0.0
        var rel = 0.0
        var mul = 1.0
        var touched = false
        for (m in list) {
            if (m.key != key || m.isExpired(now) || !hand.accepts(m.slot)) continue
            touched = true
            when (m.operation) {
                ModifierOp.FLAT -> flat += m.amount
                ModifierOp.RELATIVE -> rel += m.amount
                ModifierOp.MULTIPLY -> mul *= (1.0 + m.amount)
            }
        }
        if (!touched) return base
        return (base + flat) * (1.0 + rel) * mul
    }

    fun foldTotals(holder: UUID, base: Map<String, Double>): Map<String, Double> =
        foldTotals(holder, base, ActionHand.ANY)

    fun foldTotals(holder: UUID, base: Map<String, Double>, hand: ActionHand): Map<String, Double> {
        if (!has(holder)) return base
        val now = System.currentTimeMillis()
        val byHand = foldCache[holder]
        val cached = byHand?.get(hand)
        if (cached != null && cached.base === base && now < cached.earliestExpiry) return cached.result
        val out = LinkedHashMap<String, Double>(base)
        for (key in activeKeys(holder)) out.putIfAbsent(key, 0.0)
        val result = LinkedHashMap<String, Double>(out.size)
        for ((key, value) in out) result[key] = combine(holder, key, value, hand)
        foldCache.getOrPut(holder) { ConcurrentHashMap() }[hand] = FoldEntry(base, result, earliestExpiry(holder, now))
        return result
    }

    fun activeKeys(holder: UUID): Set<String> {
        val list = data[holder] ?: return emptySet()
        val now = System.currentTimeMillis()
        val keys = LinkedHashSet<String>()
        for (m in list) if (!m.isExpired(now)) keys.add(m.key)
        return keys
    }

    fun active(holder: UUID): List<StatModifier> {
        val list = data[holder] ?: return emptyList()
        val now = System.currentTimeMillis()
        return list.filter { !it.isExpired(now) }
    }

    fun activeBySource(holder: UUID, source: String): List<StatModifier> {
        val list = data[holder] ?: return emptyList()
        val now = System.currentTimeMillis()
        return list.filter { it.source == source && !it.isExpired(now) }
    }

    fun purgeExpired() {
        val now = System.currentTimeMillis()
        val empties = ArrayList<UUID>()
        for ((holder, list) in data) {
            if (list.removeIf { it.isExpired(now) }) invalidate(holder)
            if (list.isEmpty()) empties.add(holder)
        }
        for (holder in empties) data.remove(holder, CopyOnWriteArrayList())
    }
}
