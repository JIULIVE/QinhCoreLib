package com.qinhuai.corelib.bootstrap

interface Module {
    val name: String
    val priority: Int get() = 0
    
    fun load()
    fun enable()
    fun disable()
    fun unload()
}

abstract class AbstractModule(override val name: String) : Module {
    override fun load() {}
    override fun enable() {}
    override fun disable() {}
    override fun unload() {}
}
