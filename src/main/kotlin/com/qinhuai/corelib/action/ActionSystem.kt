package com.qinhuai.corelib.action

import com.qinhuai.corelib.debug.TraceBuilder
import com.qinhuai.corelib.debug.TraceReport
import com.qinhuai.corelib.debug.DebugTraceRegistry
import org.bukkit.entity.Player
import java.util.Locale
import java.util.UUID

interface Action {
    val id: String
    fun execute(context: ActionContext)
}

/**
 * 统一动作语义上下文：支持执行变量、追踪、输入输出数据。
 */
data class ActionContext(
    val player: Player? = null,
    val variables: MutableMap<String, Any> = mutableMapOf(),
    val traceId: String = UUID.randomUUID().toString(),
    val debug: Boolean = false,
) {
    fun setVar(key: String, value: Any) {
        variables[key] = value
    }

    fun getVar(key: String): Any? = variables[key]

    fun getVarString(key: String): String? = getVar(key)?.toString()

    fun getVarInt(key: String): Int? = getVar(key) as? Int

    fun traceBuilder(subjectType: String = "action", subjectId: String = "pipeline"): TraceBuilder =
        TraceBuilder(traceId, subjectType, subjectId)
}

/**
 * DSL 执行单元：由 compile -> validate -> optimize -> execute -> trace 组成。
 */
data class ActionNode(
    val id: String,
    val kind: String,
    val params: Map<String, Any?> = emptyMap(),
)

data class ActionDslProgram(
    val id: String,
    val nodes: List<ActionNode>,
    val metadata: Map<String, Any?> = emptyMap(),
)

interface ActionCompiler {
    fun compile(source: Any): ActionDslProgram
}

interface ActionValidator {
    fun validate(program: ActionDslProgram): List<String>
}

interface ActionOptimizer {
    fun optimize(program: ActionDslProgram): ActionDslProgram
}

interface ActionExecutor {
    fun execute(program: ActionDslProgram, context: ActionContext): TraceReport
}

class ActionPipeline {
    private val actions = mutableListOf<Action>()
    private var dslProgram: ActionDslProgram? = null

    fun addAction(action: Action) {
        actions.add(action)
    }

    fun loadProgram(program: ActionDslProgram) {
        dslProgram = program
    }

    fun execute(context: ActionContext) {
        dslProgram?.let { program ->
            val trace = context.traceBuilder("action", program.id)
            trace.record("pipeline", program.id, true, "start", mapOf("nodes" to program.nodes.size))
            program.nodes.forEachIndexed { index, node ->
                val payload = node.params.toMutableMap()
                payload["index"] = index
                trace.record("execute", node.id, true, "node executed", payload)
            }
            val report = trace.build(true, "dsl program executed")
            DebugTraceRegistry.store(report)
            return
        }

        actions.forEach { action ->
            action.execute(context)
        }
    }
}

object ActionRegistry {
    private val registry = mutableMapOf<String, Action>()

    fun register(action: Action) {
        registry[action.id.lowercase(Locale.ROOT)] = action
    }

    fun get(id: String): Action? = registry[id.lowercase(Locale.ROOT)]

    fun parseActionString(actionStr: String): Action? {
        return registry[actionStr.lowercase(Locale.ROOT)]
    }

    fun all(): List<Action> = registry.values.toList()
}
