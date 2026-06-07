package com.qinhuai.corelib.debug

object ApiBoundaryDiagnostics {
    private val publicApiClasses = listOf(
        "com.qinhuai.corelib.api.item.ItemManagerAPI",
        "com.qinhuai.corelib.api.item.module.ItemModule",
        "com.qinhuai.corelib.script.QinhScriptApi",
        "com.qinhuai.corelib.economy.EconomyBridge",
        "com.qinhuai.corelib.database.DatabaseManager",
        "com.qinhuai.corelib.pdc.PdcServiceManager",
        "com.qinhuai.corelib.placeholder.PapiBridge",
        "com.qinhuai.items.api.QinhItemsAPI",
        "com.qinhuai.items.api.QinhActionBridges",
        "com.qinhuai.skills.api.QinhSkillsAPI",
        "com.qinhuai.forge.api.QinhForgeAPI",
        "com.qinhuai.strengthen.api.QinhStrengthenAPI",
    )

    fun publicApiClasses(): List<String> = publicApiClasses

    fun diagnose(): DiagnosticResult<List<String>> = DiagnosticResult.ok(publicApiClasses(), source = "api-boundary")
}
