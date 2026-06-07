package com.qinhuai.corelib.economy

import com.qinhuai.corelib.debug.DiagnosticResult

data class EconomyTransactionResult(
    val success: Boolean,
    val message: String? = null,
    val code: String = "OK",
    val suggestion: String = "",
    val provider: String = "",
) {
    fun toDiagnostic(source: String = provider.ifBlank { "economy" }): DiagnosticResult<Unit> =
        if (success) {
            DiagnosticResult.ok(Unit, message ?: "", source)
        } else {
            DiagnosticResult.fail(
                code = code,
                message = message ?: "经济操作失败",
                source = source,
                suggestion = suggestion,
            )
        }

    companion object {
        fun ok(provider: String = ""): EconomyTransactionResult = EconomyTransactionResult(true, provider = provider)
        fun fail(message: String, code: String = "ECONOMY_FAILED", suggestion: String = "", provider: String = ""): EconomyTransactionResult =
            EconomyTransactionResult(false, message, code, suggestion, provider)
    }
}
