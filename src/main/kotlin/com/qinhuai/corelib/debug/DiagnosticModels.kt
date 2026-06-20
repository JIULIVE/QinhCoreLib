package com.qinhuai.corelib.debug

data class DiagnosticResult<T>(
    val success: Boolean,
    val value: T? = null,
    val code: String = "OK",
    val message: String = "",
    val source: String = "",
    val recoverable: Boolean = true,
    val suggestion: String = "",
    val traceId: String = "",
) {
    companion object {
        fun <T> ok(value: T, message: String = "", source: String = "", traceId: String = "") =
            DiagnosticResult(true, value, "OK", message, source, true, "", traceId)

        fun <T> fail(
            code: String,
            message: String,
            source: String = "",
            recoverable: Boolean = true,
            suggestion: String = "",
            traceId: String = "",
        ) = DiagnosticResult<T>(false, null, code, message, source, recoverable, suggestion, traceId)
    }
}
