package com.qinhuai.corelib.script

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.PolyglotAccess
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.io.IOAccess

internal class GraalJavaScriptEngine(
    private val config: QinhScriptConfig,
) {

    private val engine: Engine = Engine.newBuilder()
        .option("engine.WarnInterpreterOnly", "false")
        .build()

    fun close() {
        engine.close()
    }

    fun execute(source: ScriptSource, functionName: String, context: ScriptContext): ScriptExecutionResult {
        if (!config.enabled) {
            return ScriptExecutionResult.fail("JavaScript 未启用")
        }
        return try {
            Context.newBuilder("js")
                .engine(engine)
                .allowExperimentalOptions(true)
                .allowPolyglotAccess(PolyglotAccess.NONE)
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup { false }
                .allowCreateThread(false)
                .allowNativeAccess(false)
                .allowIO(IOAccess.NONE)
                .build().use { graalContext ->
                    val api = QinhScriptApi(context, source.logicalPath)
                    val bindings = graalContext.getBindings("js")
                    bindings.putMember("qcl", api)
                    bindings.putMember("ctx", api.ctx)
                    graalContext.eval(
                        Source.newBuilder("js", source.content, source.logicalPath).buildLiteral(),
                    )
                    val function = bindings.getMember(functionName)
                        ?: graalContext.getBindings("js").getMember(functionName)
                    if (function == null || !function.canExecute()) {
                        return ScriptExecutionResult.fail("脚本函数不存在: $functionName", code = "SCRIPT_FUNCTION_MISSING", suggestion = "确认脚本内是否定义了同名函数")
                    }
                    mapReturnValue(function.execute())
                }
        } catch (t: Throwable) {
            if (!context.silent && config.debugPrintStacktrace) {
                t.printStackTrace()
            }
            ScriptExecutionResult.fail(t.message ?: t.javaClass.simpleName)
        }
    }

    fun executeInline(
        script: String,
        sourceName: String,
        functionName: String,
        context: ScriptContext,
    ): ScriptExecutionResult {
        return execute(
            ScriptSource(sourceName, script),
            functionName,
            context,
        )
    }

    private fun mapReturnValue(value: Value?): ScriptExecutionResult {
        if (value == null || value.isNull) {
            return ScriptExecutionResult.ok(true)
        }
        if (value.isBoolean) {
            return if (value.asBoolean()) ScriptExecutionResult.ok(true) else ScriptExecutionResult.fail("脚本返回 false")
        }
        if (value.isString) {
            return ScriptExecutionResult.ok(value.asString())
        }
        if (value.isNumber) {
            return ScriptExecutionResult.ok(value.asDouble())
        }
        if (value.hasMembers()) {
            val success = if (value.hasMember("success")) value.getMember("success").asBoolean() else true
            val message = if (value.hasMember("message")) value.getMember("message").asString() else ""
            return if (success) ScriptExecutionResult.ok(true, message) else ScriptExecutionResult.fail(message.ifBlank { "脚本返回失败" }, code = "SCRIPT_RETURN_FALSE")
        }
        return ScriptExecutionResult.ok(value.`as`(Any::class.java))
    }

    companion object {
        fun isRuntimeAvailable(): Boolean = try {
            Class.forName("org.graalvm.polyglot.Context")
            true
        } catch (_: Throwable) {
            false
        }
    }
}
