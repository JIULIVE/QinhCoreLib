package com.qinhuai.corelib.debug

import com.qinhuai.corelib.lang.Lang

object ApiJarDiagnostics {
    fun status(): List<BridgeStatus> = listOf(
        BridgeDiagnostics.available("ApiJarManifest", source = "QCL", hint = Lang.get("api-jar-diagnostics.manifest-generated")),
        BridgeDiagnostics.available("ApiBoundary", source = "QCL", hint = Lang.get("api-jar-diagnostics.boundary-generated")),
        BridgeDiagnostics.available("ApiJarFilter", source = "QCL", hint = Lang.get("api-jar-diagnostics.filter-hint")),
        BridgeDiagnostics.available("ApiJarInternalPackages", source = "QCL", hint = Lang.get("api-jar-diagnostics.internal-packages-declared")),
    )

    fun diagnose(): DiagnosticResult<List<BridgeStatus>> = DiagnosticResult.ok(status(), source = "api-jar")
}
