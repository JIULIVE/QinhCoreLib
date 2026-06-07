package com.qinhuai.corelib.debug

import com.qinhuai.corelib.bootstrap.StartupReporter

object StartupDiagnostics {
    fun status(): List<BridgeStatus> {
        val hooks = mutableListOf<BridgeStatus>()
        // StartupReporter only exposes summary counters, so we surface a lightweight diagnostic layer here.
        hooks += BridgeDiagnostics.available("StartupSummary", source = "QCL", hint = "启动摘要已生成")
        return hooks
    }

    fun diagnose(): DiagnosticResult<List<BridgeStatus>> = DiagnosticResult.ok(status(), source = "startup")
}
