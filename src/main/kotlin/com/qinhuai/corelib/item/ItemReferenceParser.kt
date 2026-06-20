package com.qinhuai.corelib.item

import com.qinhuai.corelib.debug.DiagnosticResult
import com.qinhuai.corelib.lang.Lang

object ItemReferenceParser {

    data class Parsed(
        val alias: String,
        val itemId: String,
        val paramsJson: String? = null,
    )

    fun parse(ref: String): Parsed? {
        val trimmed = ref.trim()
        if (trimmed.isEmpty()) return null

        val paramsSplit = trimmed.split("::", limit = 2)
        val main = paramsSplit[0].trim()
        val params = paramsSplit.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }

        if (main.contains(':')) {
            val parts = main.split(':', limit = 2)
            return Parsed(parts[0].lowercase(), parts[1], params)
        }

        val dash = main.indexOf('-')
        if (dash <= 0) {
            return Parsed("vanilla", main, params)
        }

        val alias = main.substring(0, dash).lowercase()
        var itemId = main.substring(dash + 1)

        if (alias == "mi" || alias == "mmoitems") {
            itemId = parseMmoItemsRest(itemId)
        }

        return Parsed(alias, itemId, params)
    }

    fun diagnose(ref: String): DiagnosticResult<Parsed> {
        val parsed = parse(ref)
            ?: return DiagnosticResult.fail(
                code = "ITEM_REF_PARSE_FAILED",
                message = Lang.get("item-reference-parser.parse-failed", "ref" to ref),
                suggestion = Lang.get("item-reference-parser.parse-suggestion"),
            )
        return DiagnosticResult.ok(parsed, source = "item-reference")
    }

    private fun parseMmoItemsRest(rest: String): String {
        if (rest.contains(':')) return rest
        val idx = rest.indexOf('-')
        if (idx <= 0) return rest
        val type = rest.substring(0, idx)
        val id = rest.substring(idx + 1)
        return "$type:$id"
    }
}
