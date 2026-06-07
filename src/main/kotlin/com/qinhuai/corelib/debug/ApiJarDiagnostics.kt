package com.qinhuai.corelib.debug

object ApiJarDiagnostics {
    fun status(): List<BridgeStatus> = listOf(
        BridgeDiagnostics.available("ApiJarManifest", source = "QCL", hint = "公开API包清单已生成"),
        BridgeDiagnostics.available("ApiBoundary", source = "QCL", hint = "公开API类清单已生成"),
        BridgeDiagnostics.available("ApiJarFilter", source = "QCL", hint = "构建时应仅包含 manifest 公开包"),
        BridgeDiagnostics.available("ApiJarInternalPackages", source = "QCL", hint = "内部包已声明，用于过滤与审计"),
    )

    fun diagnose(): DiagnosticResult<List<BridgeStatus>> = DiagnosticResult.ok(status(), source = "api-jar")
}
