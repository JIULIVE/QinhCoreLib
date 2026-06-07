package com.qinhuai.corelib.debug

/**
 * apiJar 公开边界清单。
 *
 * 目标：仅导出对外 API / 契约包，不包含内部实现细节。
 */
object ApiJarManifest {
    private val exposedPackages = listOf(
        "com.qinhuai.corelib.api.item",
        "com.qinhuai.corelib.api.item.module",
        "com.qinhuai.corelib.script",
        "com.qinhuai.corelib.economy",
        "com.qinhuai.corelib.database",
        "com.qinhuai.corelib.pdc",
        "com.qinhuai.corelib.placeholder",
        "com.qinhuai.items.api",
        "com.qinhuai.skills.api",
        "com.qinhuai.forge.api",
        "com.qinhuai.strengthen.api",
    )

    private val internalPackages = listOf(
        "com.qinhuai.corelib.command",
        "com.qinhuai.corelib.bootstrap",
        "com.qinhuai.corelib.debug",
        "com.qinhuai.corelib.item",
        "com.qinhuai.corelib.customgui",
        "com.qinhuai.corelib.customblock",
        "com.qinhuai.corelib.customcrops",
        "com.qinhuai.corelib.craftengine",
        "com.qinhuai.corelib.magicgem",
        "com.qinhuai.corelib.mmoitems",
        "com.qinhuai.corelib.neigeitems",
        "com.qinhuai.corelib.modelengine",
        "com.qinhuai.corelib.mythicmobs",
        "com.qinhuai.corelib.placeholder",
        "com.qinhuai.corelib.scheduler",
        "com.qinhuai.corelib.util",
    )

    fun exposedPackages(): List<String> = exposedPackages

    fun internalPackages(): List<String> = internalPackages

    fun diagnose(): DiagnosticResult<List<String>> = DiagnosticResult.ok(exposedPackages(), source = "api-jar")
}
