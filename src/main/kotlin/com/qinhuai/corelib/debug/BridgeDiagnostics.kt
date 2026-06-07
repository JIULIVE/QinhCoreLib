package com.qinhuai.corelib.debug

/**
 * 统一桥诊断辅助：避免每个 bridge 自己造一套错误码。
 */
object BridgeDiagnostics {
    fun available(name: String, source: String = "bridge", hint: String = ""): BridgeStatus =
        BridgeStatus(name = name, available = true, enabled = true, source = source, message = if (hint.isBlank()) "可用" else hint, recoverable = true)

    fun unavailable(name: String, source: String = "bridge", hint: String = "", recoverable: Boolean = true): BridgeStatus =
        BridgeStatus(name = name, available = false, enabled = false, source = source, message = if (hint.isBlank()) "不可用" else hint, recoverable = recoverable)

    fun diagnose(name: String, available: Boolean, source: String = "bridge", hint: String = ""): DiagnosticResult<BridgeStatus> =
        if (available) DiagnosticResult.ok(available(name, source, hint), source = source) else DiagnosticResult.ok(unavailable(name, source, hint), source = source)
}
