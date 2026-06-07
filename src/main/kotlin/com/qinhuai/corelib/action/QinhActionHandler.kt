package com.qinhuai.corelib.action

/**
 * 外部 Action 执行器 — QinhItems 路由，QinhSkills 等插件实现。
 * payload 语义由 handler 自行解释，QI 原样传递。
 */
interface QinhActionHandler {
    val handlerId: String

    fun isAvailable(): Boolean = true

    fun dispatch(context: QinhActionContext): ActionDispatchResult
}
