package com.qinhuai.corelib.debug

import com.qinhuai.corelib.lang.Lang

object BridgeDiagnostics {
    fun available(name: String, source: String = "bridge", hint: String = ""): BridgeStatus =
        BridgeStatus(name = name, available = true, enabled = true, source = source, message = if (hint.isBlank()) Lang.get("common.available") else hint, recoverable = true)

    fun unavailable(name: String, source: String = "bridge", hint: String = "", recoverable: Boolean = true): BridgeStatus =
        BridgeStatus(name = name, available = false, enabled = false, source = source, message = if (hint.isBlank()) Lang.get("common.unavailable") else hint, recoverable = recoverable)

    fun diagnose(name: String, available: Boolean, source: String = "bridge", hint: String = ""): DiagnosticResult<BridgeStatus> =
        if (available) DiagnosticResult.ok(available(name, source, hint), source = source) else DiagnosticResult.ok(unavailable(name, source, hint), source = source)
}
