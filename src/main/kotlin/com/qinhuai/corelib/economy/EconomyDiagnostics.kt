package com.qinhuai.corelib.economy

import com.qinhuai.corelib.debug.DiagnosticResult

object EconomyDiagnostics {
    fun unavailable(): DiagnosticResult<Unit> = DiagnosticResult.fail(
        code = "ECONOMY_UNAVAILABLE",
        message = "没有可用的经济后端",
        suggestion = "检查 Vault / ExcellentEconomy / PlayerPoints 是否安装并启用",
    )

    fun providerMissing(providerId: String): DiagnosticResult<Unit> = DiagnosticResult.fail(
        code = "ECONOMY_PROVIDER_MISSING",
        message = "未找到经济后端: $providerId",
        suggestion = "检查 economy.default-provider 配置，或确认后端已注册",
    )

    fun currencyMissing(providerId: String): DiagnosticResult<Unit> = DiagnosticResult.fail(
        code = "ECONOMY_CURRENCY_MISSING",
        message = "$providerId 需要指定 currency",
        suggestion = "为该经济后端补充 currencyId，例如 money / gold / silver",
    )
}
