package com.qinhuai.corelib.script

import com.qinhuai.corelib.debug.DiagnosticResult
import com.qinhuai.corelib.lang.Lang

object ScriptDiagnostics {
    fun unavailable(reason: String = Lang.get("script-diagnostics.unavailable-reason")): DiagnosticResult<Unit> {
        return DiagnosticResult.fail(
            code = "SCRIPT_UNAVAILABLE",
            message = reason,
            suggestion = Lang.get("script-diagnostics.unavailable-suggestion"),
        )
    }

    fun parseFailed(reference: String): DiagnosticResult<Unit> {
        return DiagnosticResult.fail(
            code = "SCRIPT_PARSE_FAILED",
            message = Lang.get("script-diagnostics.parse-failed", "reference" to reference),
            suggestion = Lang.get("script-diagnostics.parse-failed-suggestion"),
        )
    }

    fun notFound(path: String): DiagnosticResult<Unit> {
        return DiagnosticResult.fail(
            code = "SCRIPT_NOT_FOUND",
            message = Lang.get("script-diagnostics.not-found", "path" to path),
            suggestion = Lang.get("script-diagnostics.not-found-suggestion"),
        )
    }

    fun functionMissing(name: String): DiagnosticResult<Unit> {
        return DiagnosticResult.fail(
            code = "SCRIPT_FUNCTION_MISSING",
            message = Lang.get("script-diagnostics.function-missing", "name" to name),
            suggestion = Lang.get("script-diagnostics.function-missing-suggestion"),
        )
    }
}
