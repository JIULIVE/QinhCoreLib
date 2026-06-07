package com.qinhuai.corelib.item

import com.qinhuai.corelib.debug.DiagnosticResult

/**
 * 解析传奇系物品引用：
 * - {@code mm-海神三叉戟} · {@code mi-SWORD-Legendary} · {@code ni-blade::{"品质":"传说"}}
 * - {@code qinhitems:legendary_blade} · {@code vanilla:iron_ingot}
 * - {@code material-IRON_INGOT} · {@code type-WOOL}
 */
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
                message = "物品引用解析失败: $ref",
                suggestion = "使用 namespace:id 或 alias-id 格式，例如 qinhitems:blade / mm-Dragon_Sword",
            )
        return DiagnosticResult.ok(parsed, source = "item-reference")
    }

    /** {@code SWORD-Legendary_Blade} → {@code SWORD:Legendary_Blade} */
    private fun parseMmoItemsRest(rest: String): String {
        if (rest.contains(':')) return rest
        val idx = rest.indexOf('-')
        if (idx <= 0) return rest
        val type = rest.substring(0, idx)
        val id = rest.substring(idx + 1)
        return "$type:$id"
    }
}
