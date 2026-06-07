package com.qinhuai.corelib.debug

object ApiJarFilter {
    fun shouldExport(className: String): Boolean {
        val pkg = className.substringBeforeLast('.', "")
        return ApiJarManifest.exposedPackages().any { exposed ->
            pkg == exposed || pkg.startsWith("$exposed.")
        }
    }

    fun exportableClasses(candidateClasses: Collection<String>): List<String> =
        candidateClasses.filter { shouldExport(it) }
}
