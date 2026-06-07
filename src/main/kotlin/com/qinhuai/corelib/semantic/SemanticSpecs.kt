package com.qinhuai.corelib.semantic

/**
 * Qinh 统一语义层：把物品、技能、锻造、效果、动作、条件收束为统一的可编排规格。
 *
 * 设计目标：
 * 1. 统一 ID / namespace / version / tags / variables
 * 2. 统一 validate / explain / trace 输入输出
 * 3. 作为后续 DSL、编辑器、调试系统的共同数据底座
 */

interface SemanticSpec {
    val id: String
    val namespace: String
    val version: Int
    val tags: Set<String>
    val variables: Map<String, Any?>

    fun qualifiedId(): String = "$namespace:$id"
}

data class SpecIssue(
    val code: String,
    val message: String,
    val field: String? = null,
    val severity: Severity = Severity.ERROR,
) {
    enum class Severity { INFO, WARN, ERROR }
}

data class SpecValidationResult(
    val valid: Boolean,
    val issues: List<SpecIssue> = emptyList(),
) {
    companion object {
        fun ok(): SpecValidationResult = SpecValidationResult(true)
        fun fail(vararg issues: SpecIssue): SpecValidationResult = SpecValidationResult(false, issues.toList())
    }
}

interface SpecValidator<T : SemanticSpec> {
    fun validate(spec: T): SpecValidationResult
}

data class ItemSpec(
    override val id: String,
    override val namespace: String = "qinhitems",
    override val version: Int = 1,
    override val tags: Set<String> = emptySet(),
    override val variables: Map<String, Any?> = emptyMap(),
    val material: String? = null,
    val displayName: String? = null,
    val lore: List<String> = emptyList(),
    val sourceRef: String? = null,
) : SemanticSpec

data class SkillSpec(
    override val id: String,
    override val namespace: String = "qinhskills",
    override val version: Int = 1,
    override val tags: Set<String> = emptySet(),
    override val variables: Map<String, Any?> = emptyMap(),
    val trigger: String? = null,
    val cooldownTicks: Int = 0,
    val cost: Map<String, Any?> = emptyMap(),
) : SemanticSpec

data class ForgeSpec(
    override val id: String,
    override val namespace: String = "qinhforge",
    override val version: Int = 1,
    override val tags: Set<String> = emptySet(),
    override val variables: Map<String, Any?> = emptyMap(),
    val stationId: String? = null,
    val recipeId: String? = null,
    val materials: List<String> = emptyList(),
    val outputRef: String? = null,
) : SemanticSpec

data class EffectSpec(
    override val id: String,
    override val namespace: String = "qinhcorelib",
    override val version: Int = 1,
    override val tags: Set<String> = emptySet(),
    override val variables: Map<String, Any?> = emptyMap(),
    val type: String? = null,
    val amount: Double? = null,
    val durationTicks: Int? = null,
) : SemanticSpec

data class ActionSpec(
    override val id: String,
    override val namespace: String = "qinhcorelib",
    override val version: Int = 1,
    override val tags: Set<String> = emptySet(),
    override val variables: Map<String, Any?> = emptyMap(),
    val steps: List<String> = emptyList(),
    val input: Map<String, Any?> = emptyMap(),
) : SemanticSpec

data class ConditionSpec(
    override val id: String,
    override val namespace: String = "qinhcorelib",
    override val version: Int = 1,
    override val tags: Set<String> = emptySet(),
    override val variables: Map<String, Any?> = emptyMap(),
    val expression: String? = null,
    val operands: Map<String, Any?> = emptyMap(),
) : SemanticSpec

object SemanticSpecRegistry {
    private val specs = linkedMapOf<String, SemanticSpec>()

    fun register(spec: SemanticSpec) {
        specs[spec.qualifiedId().lowercase()] = spec
    }

    fun get(qualifiedId: String): SemanticSpec? = specs[qualifiedId.lowercase()]

    fun all(): List<SemanticSpec> = specs.values.toList()

    fun clear() {
        specs.clear()
    }
}
