package com.qinhuai.corelib.action

interface QinhActionHandler {
    val handlerId: String

    fun isAvailable(): Boolean = true

    fun dispatch(context: QinhActionContext): ActionDispatchResult
}
