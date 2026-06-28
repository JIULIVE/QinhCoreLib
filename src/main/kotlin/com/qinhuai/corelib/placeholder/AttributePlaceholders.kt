package com.qinhuai.corelib.placeholder

import com.qinhuai.corelib.attribute.AttributeRegistry
import com.qinhuai.corelib.attribute.AttributeService
import org.bukkit.OfflinePlayer

object AttributePlaceholders : QinhPlaceholderProvider {

    override val identifier: String = "qinhcorelib"

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        val online = player?.player ?: return null
        val low = params.lowercase()
        if (low == "combatpower") return fmt(AttributeService.combatPower(online))
        // 冷却缩减便捷占位符（供 MM 技能 Cooldown / NI / 任意 config 消费，不依赖 QS）
        if (low == "cdr") return fmt(cdr(online))
        if (low == "cdrmult") return fmt(1.0 - cdr(online))
        val idx = params.indexOf('_')
        if (idx <= 0) return null
        val prefix = params.substring(0, idx).lowercase()
        val rawKey = params.substring(idx + 1)
        // 冷却缩减应用到基础值: %qinhcorelib_cdrapply_<基础冷却>% → 基础 × (1-冷却缩减)
        if (prefix == "cdrapply") {
            val base = rawKey.toDoubleOrNull() ?: return null
            return fmt(base * (1.0 - cdr(online)))
        }
        // 通用减免应用: %qinhcorelib_reduceapply_<属性>_<基础值>% → 基础 × (1 - 该属性总值[0~1])
        if (prefix == "reduceapply") {
            val sep = rawKey.lastIndexOf('_')
            if (sep <= 0) return null
            val attr = rawKey.substring(0, sep)
            val base = rawKey.substring(sep + 1).toDoubleOrNull() ?: return null
            val frac = AttributeService.total(online, AttributeRegistry.resolve(attr)?.key ?: attr.lowercase()).coerceIn(0.0, 1.0)
            return fmt(base * (1.0 - frac))
        }
        val def = AttributeRegistry.resolve(rawKey)
        val key = def?.key ?: rawKey.lowercase()
        return when (prefix) {
            "attr" -> fmt(AttributeService.total(online, key))
            "attrpct" -> "${Math.round(AttributeService.total(online, key) * 100)}%"
            "attrname" -> def?.displayName ?: rawKey
            else -> null
        }
    }

    private fun cdr(online: org.bukkit.entity.Player): Double =
        AttributeService.total(online, "cooldown_reduction").coerceIn(0.0, MAX_CDR)

    private const val MAX_CDR = 0.9

    private fun fmt(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.2f", v)
}
