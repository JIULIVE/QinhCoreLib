package com.qinhuai.corelib.placeholder

import com.qinhuai.corelib.attribute.AttributeRegistry
import com.qinhuai.corelib.attribute.AttributeService
import org.bukkit.OfflinePlayer

object AttributePlaceholders : QinhPlaceholderProvider {

    override val identifier: String = "qinhcorelib"

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        val online = player?.player ?: return null
        if (params.equals("combatpower", ignoreCase = true)) return fmt(AttributeService.combatPower(online))
        val idx = params.indexOf('_')
        if (idx <= 0) return null
        val prefix = params.substring(0, idx).lowercase()
        val rawKey = params.substring(idx + 1)
        val def = AttributeRegistry.resolve(rawKey)
        val key = def?.key ?: rawKey.lowercase()
        return when (prefix) {
            "attr" -> fmt(AttributeService.total(online, key))
            "attrpct" -> "${Math.round(AttributeService.total(online, key) * 100)}%"
            "attrname" -> def?.displayName ?: rawKey
            else -> null
        }
    }

    private fun fmt(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.2f", v)
}
