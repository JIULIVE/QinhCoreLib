package com.qinhuai.corelib.debug

data class PlatformStatus(
    val name: String,
    val version: String,
    val enabled: Boolean,
    val modules: List<ModuleStatus> = emptyList(),
    val bridges: List<BridgeStatus> = emptyList(),
    val health: HealthReport = HealthReport.healthy(),
    val trace: TraceContext = TraceContext(),
)

data class ModuleStatus(
    val name: String,
    val enabled: Boolean,
    val available: Boolean = true,
    val message: String = "",
)

data class BridgeStatus(
    val name: String,
    val available: Boolean,
    val enabled: Boolean = available,
    val source: String = "",
    val message: String = "",
    val recoverable: Boolean = true,
)

data class HealthReport(
    val ok: Boolean,
    val code: String = "OK",
    val message: String = "",
    val suggestion: String = "",
) {
    companion object {
        fun healthy(message: String = "") = HealthReport(true, "OK", message, "")
        fun degraded(code: String, message: String, suggestion: String = "") = HealthReport(false, code, message, suggestion)
    }
}

data class TraceContext(
    val traceId: String = "",
    val module: String = "",
    val bridge: String = "",
    val durationMs: Long = 0,
    val result: String = "",
    val hint: String = "",
)
