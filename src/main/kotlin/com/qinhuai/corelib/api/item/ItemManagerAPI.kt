package com.qinhuai.corelib.api.item

import com.qinhuai.corelib.api.item.module.ItemModule
import com.qinhuai.corelib.debug.DiagnosticResult
import com.qinhuai.corelib.item.ItemReferenceParser
import com.qinhuai.corelib.item.ItemSource
import com.qinhuai.corelib.item.ItemSourceManager
import com.qinhuai.corelib.item.ItemSourceModuleAdapter
import com.qinhuai.corelib.item.ItemSourceType
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.Locale

/**
 * 统一物品桥 — 对标传奇 LegendCore ItemManagerAPI。
 *
 * ```groovy
 * ItemManagerAPI.instance.register(plugin, module, "ox", "oraxen")
 * def stack = ItemManagerAPI.instance.getHookItem("mm-海神三叉戟")
 * ```
 */
class ItemManagerAPI private constructor() {

    data class RegisteredModule(
        val ownerKey: String,
        val module: ItemModule,
    )

    private val byAlias = linkedMapOf<String, RegisteredModule>()

    companion object {
        /** 传奇系 Groovy 脚本使用 {@code ItemManagerAPI.instance} */
        @JvmField
        val instance: ItemManagerAPI = ItemManagerAPI()

        @JvmStatic
        fun getHookItem(ref: String): ItemStack? = instance.getHookItem(ref, null, 1)

        @JvmStatic
        fun getHookItem(ref: String, player: Player?): ItemStack? = instance.getHookItem(ref, player, 1)
    }

    fun register(plugin: org.bukkit.plugin.Plugin, module: ItemModule, vararg aliases: String) {
        registerByOwner(plugin.name, module, *aliases)
    }

    fun registerByOwner(ownerKey: String, module: ItemModule, vararg aliases: String) {
        for (raw in aliases) {
            byAlias[raw.lowercase(Locale.ROOT)] = RegisteredModule(ownerKey, module)
        }
    }

    fun register(source: ItemSource, vararg aliases: String) {
        registerByOwner(source.id, ItemSourceModuleAdapter(source), *aliases)
    }

    fun unregister(plugin: org.bukkit.plugin.Plugin) {
        unregister(plugin.name)
    }

    fun unregister(ownerKey: String) {
        val remove = byAlias.filterValues { it.ownerKey.equals(ownerKey, ignoreCase = true) }.keys
        remove.forEach { byAlias.remove(it) }
    }

    fun clear() {
        byAlias.clear()
    }

    fun aliases(): Set<String> = byAlias.keys.toSet()

    data class ResolveResult(
        val success: Boolean,
        val item: ItemStack? = null,
        val code: String = "OK",
        val message: String = "",
        val source: String = "",
        val recoverable: Boolean = true,
        val suggestion: String = "",
        val traceId: String = "",
    )

    fun getHookItem(ref: String, player: Player?, amount: Int = 1): ItemStack? {
        val parsed = ItemReferenceParser.parse(ref) ?: return null
        val stack = resolveStack(parsed, player).item ?: return null
        stack.amount = amount.coerceAtLeast(1)
        return stack
    }

    fun diagnose(ref: String, player: Player? = null): DiagnosticResult<ItemStack> {
        val parsed = ItemReferenceParser.diagnose(ref)
        if (!parsed.success || parsed.value == null) {
            return DiagnosticResult.fail(
                code = parsed.code,
                message = parsed.message,
                source = parsed.source,
                suggestion = parsed.suggestion,
            )
        }
        val resolved = resolveStack(parsed.value, player)
        if (!resolved.success || resolved.item == null) {
            return DiagnosticResult.fail(
                code = resolved.code,
                message = resolved.message,
                source = resolved.source,
                recoverable = resolved.recoverable,
                suggestion = resolved.suggestion,
            )
        }
        return DiagnosticResult.ok(resolved.item, resolved.message, resolved.source)
    }

    fun resolve(ref: String, player: Player? = null, amount: Int = 1): ResolveResult {
        val parsed = ItemReferenceParser.parse(ref)
            ?: return ResolveResult(false, code = "PARSE_FAILED", message = "物品引用解析失败", suggestion = "检查引用格式，例如 mm-xxx / qinhitems:id / vanilla:IRON_INGOT")
        val resolved = resolveStack(parsed, player)
        val stack = resolved.item ?: return resolved
        stack.amount = amount.coerceAtLeast(1)
        return resolved.copy(item = stack)
    }

    private fun resolveStack(parsed: ItemReferenceParser.Parsed, player: Player?): ResolveResult {
        val alias = parsed.alias.lowercase(Locale.ROOT)

        when (alias) {
            "material", "type", "vanilla" -> {
                return vanillaMaterial(parsed.itemId)?.let { ResolveResult(true, it, source = "vanilla") }
                    ?: ResolveResult(false, code = "MATERIAL_NOT_FOUND", message = "无法匹配原版物品: ${parsed.itemId}", source = "vanilla", suggestion = "确认材料名是否为 Bukkit Material 枚举名")
            }
        }

        byAlias[alias]?.let { reg ->
            val built = if (parsed.paramsJson != null) {
                reg.module.buildWithParams(player, parsed.itemId, parsed.paramsJson)
            } else {
                reg.module.build(player, parsed.itemId)
            }
            if (built != null) return ResolveResult(true, built, source = reg.ownerKey)
            return ResolveResult(false, code = "MODULE_BUILD_FAILED", message = "模块无法构建物品: $alias:${parsed.itemId}", source = reg.ownerKey, suggestion = "检查模块是否已注册、参数是否正确、以及物品 ID 是否存在")
        }

        val sourceType = ItemSourceType.fromId(alias) ?: return ResolveResult(false, code = "SOURCE_NOT_FOUND", message = "未找到物品源: $alias", source = alias, suggestion = "确认别名是否已注册，或是否需要先调用 registerBuiltinSources()")
        val item = ItemSourceManager.getItem(sourceType.id, parsed.itemId, 1)
        return if (item != null) ResolveResult(true, item, source = sourceType.id)
        else ResolveResult(false, code = "ITEM_NOT_FOUND", message = "物品源未返回结果: ${sourceType.id}:${parsed.itemId}", source = sourceType.id, suggestion = "检查后端插件是否可用，或该物品是否存在")
    }

    private fun vanillaMaterial(spec: String): ItemStack? {
        val matName = spec.substringBefore(':').uppercase(Locale.ROOT)
        val material = Material.matchMaterial(matName) ?: return null
        return ItemStack(material, 1)
    }

    /** 将已注册的 ItemSource 同步为 ItemModule 别名 */
    fun registerBuiltinSources() {
        for (type in ItemSourceType.entries) {
            val source = ItemSourceManager.getSource(type.id) ?: continue
            val aliases = (listOf(type.id) + type.aliases).distinct().toTypedArray()
            register(source, *aliases)
        }
    }
}
