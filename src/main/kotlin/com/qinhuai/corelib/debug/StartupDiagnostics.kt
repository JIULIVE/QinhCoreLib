package com.qinhuai.corelib.debug

import com.qinhuai.corelib.bootstrap.StartupReporter
import com.qinhuai.corelib.lang.Lang

object StartupDiagnostics {
    fun status(): List<BridgeStatus> {
        val hooks = mutableListOf<BridgeStatus>()
        hooks += BridgeDiagnostics.available("StartupSummary", source = "QCL", hint = Lang.get("startup-diagnostics.summary-generated"))
        return hooks
    }

    fun diagnose(): DiagnosticResult<List<BridgeStatus>> = DiagnosticResult.ok(status(), source = "startup")
}
