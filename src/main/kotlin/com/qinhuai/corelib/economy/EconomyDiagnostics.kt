package com.qinhuai.corelib.economy

import com.qinhuai.corelib.debug.DiagnosticResult
import com.qinhuai.corelib.lang.Lang

object EconomyDiagnostics {
    fun unavailable(): DiagnosticResult<Unit> = DiagnosticResult.fail(
        code = "ECONOMY_UNAVAILABLE",
        message = Lang.get("economy-diagnostics.unavailable-message"),
        suggestion = Lang.get("economy-diagnostics.unavailable-suggestion"),
    )

    fun providerMissing(providerId: String): DiagnosticResult<Unit> = DiagnosticResult.fail(
        code = "ECONOMY_PROVIDER_MISSING",
        message = Lang.get("economy-diagnostics.provider-missing-message", "id" to providerId),
        suggestion = Lang.get("economy-diagnostics.provider-missing-suggestion"),
    )

    fun currencyMissing(providerId: String): DiagnosticResult<Unit> = DiagnosticResult.fail(
        code = "ECONOMY_CURRENCY_MISSING",
        message = Lang.get("economy-diagnostics.currency-missing-message", "id" to providerId),
        suggestion = Lang.get("economy-diagnostics.currency-missing-suggestion"),
    )
}
