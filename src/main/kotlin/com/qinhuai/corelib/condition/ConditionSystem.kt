package com.qinhuai.corelib.condition

import com.qinhuai.corelib.debug.DebugTraceRegistry
import com.qinhuai.corelib.debug.TraceBuilder
import com.qinhuai.corelib.debug.TraceReport
import java.util.Locale

interface Condition {
    val id: String
    fun evaluate(context: ConditionContext): Boolean
}

data class ConditionContext(
    val variables: MutableMap<String, Any> = mutableMapOf(),
    val traceId: String? = null,
    val debug: Boolean = false,
) {
    fun setVar(key: String, value: Any) {
        variables[key] = value
    }

    fun getVar(key: String): Any? = variables[key]

    fun getVarString(key: String): String? = getVar(key)?.toString()

    fun traceBuilder(subjectId: String = "condition"): TraceBuilder =
        TraceBuilder(traceId ?: java.util.UUID.randomUUID().toString(), "condition", subjectId)
}

data class ConditionNode(
    val id: String,
    val type: String,
    val params: Map<String, Any?> = emptyMap(),
)

data class ConditionDslProgram(
    val id: String,
    val nodes: List<ConditionNode>,
    val logic: String = "AND",
)

interface ConditionCompiler {
    fun compile(source: Any): ConditionDslProgram
}

interface ConditionValidator {
    fun validate(program: ConditionDslProgram): List<String>
}

interface ConditionOptimizer {
    fun optimize(program: ConditionDslProgram): ConditionDslProgram
}

interface ConditionExecutor {
    fun execute(program: ConditionDslProgram, context: ConditionContext): TraceReport
}

object ConditionRegistry {
    private val conditions = mutableMapOf<String, Condition>()

    fun register(condition: Condition) {
        conditions[condition.id.lowercase(Locale.ROOT)] = condition
    }

    fun get(id: String): Condition? = conditions[id.lowercase(Locale.ROOT)]

    fun all(): List<Condition> = conditions.values.toList()
}

class CompositeCondition(
    private val conditions: List<Condition>,
    private val operator: LogicOperator = LogicOperator.AND
) : Condition {
    override val id: String = "composite"

    override fun evaluate(context: ConditionContext): Boolean {
        return when (operator) {
            LogicOperator.AND -> conditions.all { it.evaluate(context) }
            LogicOperator.OR -> conditions.any { it.evaluate(context) }
            LogicOperator.NOT -> !conditions.all { it.evaluate(context) }
        }
    }
}

enum class LogicOperator {
    AND, OR, NOT
}

class ConditionPipeline {
    private val conditions = mutableListOf<Condition>()
    private var dslProgram: ConditionDslProgram? = null

    fun addCondition(condition: Condition) {
        conditions.add(condition)
    }

    fun loadProgram(program: ConditionDslProgram) {
        dslProgram = program
    }

    fun evaluate(context: ConditionContext): Boolean {
        dslProgram?.let { program ->
            val trace = context.traceBuilder(program.id)
            var success = true
            for (node in program.nodes) {
                val resolved = evaluateNode(node, context)
                trace.record("condition", node.id, resolved, if (resolved) "condition passed" else "condition failed", node.params)
                if (!resolved) {
                    success = false
                    if (program.logic.equals("AND", ignoreCase = true)) break
                }
            }
            val report = trace.build(success, if (success) "condition program passed" else "condition program failed")
            DebugTraceRegistry.store(report)
            return success
        }

        return when (operatorFallback()) {
            LogicOperator.AND -> conditions.all { it.evaluate(context) }
            LogicOperator.OR -> conditions.any { it.evaluate(context) }
            LogicOperator.NOT -> !conditions.all { it.evaluate(context) }
        }
    }

    private fun operatorFallback(): LogicOperator = LogicOperator.AND

    private fun evaluateNode(node: ConditionNode, context: ConditionContext): Boolean {
        val left = node.params["left"]
        val op = node.params["op"]?.toString()?.lowercase(Locale.ROOT)
        val right = node.params["right"]
        return when (op) {
            "eq" -> left == right
            "neq" -> left != right
            "exists" -> left != null
            "notnull" -> left != null
            "contains" -> left?.toString()?.contains(right?.toString().orEmpty()) == true
            "var_eq" -> context.getVar(left?.toString().orEmpty())?.toString() == right?.toString()
            else -> false
        }
    }
}
