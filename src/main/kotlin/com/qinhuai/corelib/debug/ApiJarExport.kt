package com.qinhuai.corelib.debug

object ApiJarExport {
    fun includedResourcePatterns(): List<String> = listOf(
        "com/qinhuai/corelib/api/**",
        "com/qinhuai/corelib/script/**",
        "com/qinhuai/corelib/economy/**",
        "com/qinhuai/corelib/database/**",
        "com/qinhuai/corelib/pdc/**",
        "com/qinhuai/corelib/placeholder/**",
        "com/qinhuai/items/api/**",
        "com/qinhuai/skills/api/**",
        "com/qinhuai/forge/api/**",
        "com/qinhuai/strengthen/api/**",
    )

    fun diagnose(): DiagnosticResult<List<String>> = DiagnosticResult.ok(includedResourcePatterns(), source = "api-jar")
}
