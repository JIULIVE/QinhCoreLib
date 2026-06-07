package com.qinhuai.corelib.script

import com.qinhuai.corelib.QinhCoreLib

data class QinhScriptConfig(
    val enabled: Boolean = true,
    val defaultFunction: String = "main",
    val debugPrintStacktrace: Boolean = false,
) {
    companion object {
        fun fromPlugin(): QinhScriptConfig {
            val conf = QinhCoreLib.instance.config
            return QinhScriptConfig(
                enabled = conf.getBoolean("javascript.enabled", true),
                defaultFunction = conf.getString("javascript.default-function", "main") ?: "main",
                debugPrintStacktrace = conf.getBoolean("javascript.debug.print-stacktrace", false),
            )
        }
    }
}
