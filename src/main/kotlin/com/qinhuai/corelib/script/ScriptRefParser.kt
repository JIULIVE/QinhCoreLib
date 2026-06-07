package com.qinhuai.corelib.script

object ScriptRefParser {

    data class Call(
        val logicalPath: String,
        val functionName: String,
    )

    fun parse(value: String, defaultFunction: String = "main"): Call {
        var ref = value.trim()
        if (ref.isBlank()) {
            return Call("", defaultFunction)
        }
        var function = defaultFunction
        val lastColon = ref.lastIndexOf(':')
        if (lastColon > 0) {
            val tail = ref.substring(lastColon + 1)
            if (!tail.contains('/') && !tail.endsWith(".js", ignoreCase = true)) {
                function = tail
                ref = ref.substring(0, lastColon)
            }
        }
        if (!ref.contains(':')) {
            ref = "global:$ref"
        }
        if (!ref.endsWith(".js", ignoreCase = true)) {
            ref = "$ref.js"
        }
        return Call(ref, function)
    }
}
