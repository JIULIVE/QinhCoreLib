package com.qinhuai.corelib.debug

data class TraceEvent(
    val stage: String,
    val nodeId: String,
    val success: Boolean,
    val message: String,
    val data: Map<String, Any?> = emptyMap(),
)

data class TraceReport(
    val traceId: String,
    val subjectType: String,
    val subjectId: String,
    val success: Boolean,
    val events: List<TraceEvent> = emptyList(),
    val summary: String = "",
) {
    fun firstFailure(): TraceEvent? = events.firstOrNull { !it.success }
}

class TraceBuilder(
    private val traceId: String,
    private val subjectType: String,
    private val subjectId: String,
) {
    private val events = mutableListOf<TraceEvent>()

    fun record(stage: String, nodeId: String, success: Boolean, message: String, data: Map<String, Any?> = emptyMap()) {
        events += TraceEvent(stage, nodeId, success, message, data)
    }

    fun build(success: Boolean, summary: String = ""): TraceReport {
        return TraceReport(traceId, subjectType, subjectId, success, events.toList(), summary)
    }
}

object DebugTraceRegistry {
    private val traces = linkedMapOf<String, TraceReport>()

    fun store(report: TraceReport) {
        traces[report.traceId] = report
    }

    fun get(traceId: String): TraceReport? = traces[traceId]

    fun all(): List<TraceReport> = traces.values.toList()

    fun clear() {
        traces.clear()
    }
}
