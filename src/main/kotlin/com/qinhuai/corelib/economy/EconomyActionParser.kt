package com.qinhuai.corelib.economy

import java.util.Locale

/**
 * 解析 GUI / 动作里的经济参数。
 *
 * 支持格式（`|` 后为失败提示，可选）：
 * - `100`
 * - `100:money`
 * - `excellenteconomy:gold:100`
 * - `vault:50`
 * - `playerpoints:100`
 */
data class EconomyActionSpec(
    val amount: Double,
    val providerId: String?,
    val currencyId: String?,
    val failMessage: String? = null,
)

object EconomyActionParser {

    private val knownProviders = setOf(
        "auto",
        "vault",
        "excellenteconomy",
        "playerpoints",
    )

    fun parse(value: String): EconomyActionSpec? {
        if (value.isBlank()) return null

        val pipeParts = value.split("|", limit = 2)
        val main = pipeParts[0].trim()
        val failMessage = pipeParts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }

        val segments = main.split(":").map { it.trim() }.filter { it.isNotEmpty() }
        if (segments.isEmpty()) return null

        val amountIndex = segments.indexOfLast { it.toDoubleOrNull() != null }
        if (amountIndex < 0) return null

        val amount = segments[amountIndex].toDoubleOrNull() ?: return null
        val prefix = segments.take(amountIndex)

        var providerId: String? = null
        var currencyId: String? = null

        when (prefix.size) {
            0 -> Unit
            1 -> {
                val token = normalizeProvider(prefix[0])
                if (token in knownProviders) {
                    providerId = token
                } else {
                    currencyId = prefix[0].lowercase(Locale.ROOT)
                }
            }
            else -> {
                providerId = normalizeProvider(prefix[0])
                currencyId = prefix.drop(1).joinToString(":").lowercase(Locale.ROOT).takeIf { it.isNotBlank() }
            }
        }

        return EconomyActionSpec(amount, providerId, currencyId, failMessage)
    }

    private fun normalizeProvider(raw: String): String = when (raw.lowercase(Locale.ROOT)) {
        "ee" -> "excellenteconomy"
        "pp" -> "playerpoints"
        else -> raw.lowercase(Locale.ROOT)
    }
}
