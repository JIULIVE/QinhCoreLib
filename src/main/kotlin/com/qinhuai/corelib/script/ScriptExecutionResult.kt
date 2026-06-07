package com.qinhuai.corelib.script

import com.qinhuai.corelib.debug.DiagnosticResult

data class ScriptExecutionResult(
    val success: Boolean,
    val value: Any? = null,
    val message: String = "",
    val skipped: Boolean = false,
    val code: String = "OK",
    val suggestion: String = "",
    val traceId: String = "",
) {
    fun asBoolean(default: Boolean = false): Boolean = when {
        skipped -> default
        value is Boolean -> value
        else -> success
    }

    fun toDiagnostic(source: String = "script"): DiagnosticResult<Any?> =
        if (success) {
            DiagnosticResult.ok(value, message, source, traceId)
        } else {
            DiagnosticResult(false, value, code, message, source, recoverable = !skipped, suggestion = suggestion, traceId = traceId)
        }

    companion object {
        fun ok(value: Any? = true, message: String = "", traceId: String = "") = ScriptExecutionResult(true, value, message, traceId = traceId)
        fun fail(message: String, code: String = "SCRIPT_FAILED", suggestion: String = "", traceId: String = "") = ScriptExecutionResult(false, null, message, false, code, suggestion, traceId)
        fun skipped(message: String = "", traceId: String = "") = ScriptExecutionResult(false, null, message, skipped = true, code = "SCRIPT_SKIPPED", traceId = traceId)
    }
}
