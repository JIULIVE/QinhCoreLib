package com.qinhuai.corelib.script

import com.qinhuai.corelib.debug.DiagnosticResult

object ScriptDiagnostics {
    fun unavailable(reason: String = "JavaScript 不可用（需 Paper/Purpur libraries 加载 GraalJS）"): DiagnosticResult<Unit> {
        return DiagnosticResult.fail(
            code = "SCRIPT_UNAVAILABLE",
            message = reason,
            suggestion = "检查服务器是否启用了 GraalJS 运行时",
        )
    }

    fun parseFailed(reference: String): DiagnosticResult<Unit> {
        return DiagnosticResult.fail(
            code = "SCRIPT_PARSE_FAILED",
            message = "脚本引用解析失败: $reference",
            suggestion = "使用 namespace:path.js:function 格式，例如 global:example.js:main",
        )
    }

    fun notFound(path: String): DiagnosticResult<Unit> {
        return DiagnosticResult.fail(
            code = "SCRIPT_NOT_FOUND",
            message = "找不到脚本: $path",
            suggestion = "确认脚本文件是否放在 plugins/QinhCoreLib/scripts 对应目录下",
        )
    }

    fun functionMissing(name: String): DiagnosticResult<Unit> {
        return DiagnosticResult.fail(
            code = "SCRIPT_FUNCTION_MISSING",
            message = "脚本函数不存在: $name",
            suggestion = "确认脚本内已定义同名函数",
        )
    }
}
