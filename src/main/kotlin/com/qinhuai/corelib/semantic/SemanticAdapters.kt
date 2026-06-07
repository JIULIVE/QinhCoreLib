package com.qinhuai.corelib.semantic

/**
 * 语义适配层占位：
 * CoreLib 不直接依赖 QinhItems / QinhSkills / QinhForge 的实现类，
 * 具体适配应由各模块在各自工程内完成并注册到语义中心。
 */
object SemanticAdapters {
    fun registerItem(any: Any) {}
    fun registerSkill(any: Any) {}
    fun registerForgeRecipe(any: Any, stationId: String? = null) {}
    fun registerForgeStation(any: Any) {}
}
