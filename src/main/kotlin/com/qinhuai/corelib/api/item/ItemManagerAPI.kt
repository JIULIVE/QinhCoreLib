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
import com.qinhuai.corelib.lang.Lang
import java.util.Locale

class ItemManagerAPI private constructor() {

    data class RegisteredModule(
        val ownerKey: String,
        val module: ItemModule,
    )

    private val byAlias = linkedMapOf<String, RegisteredModule>()

    companion object {
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

    fun matches(stack: ItemStack, ref: String): Boolean = ItemSourceManager.matchesReference(stack, ref)

    fun identify(stack: ItemStack): Pair<String, String>? = ItemSourceManager.identify(stack)

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
            ?: return ResolveResult(false, code = "PARSE_FAILED", message = Lang.get("item-manager-api.parse-failed"), suggestion = Lang.get("item-manager-api.parse-failed-suggestion"))
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
                    ?: ResolveResult(false, code = "MATERIAL_NOT_FOUND", message = Lang.get("item-manager-api.material-not-found", "id" to parsed.itemId), source = "vanilla", suggestion = Lang.get("item-manager-api.material-not-found-suggestion"))
            }
        }

        byAlias[alias]?.let { reg ->
            val built = if (parsed.paramsJson != null) {
                reg.module.buildWithParams(player, parsed.itemId, parsed.paramsJson)
            } else {
                reg.module.build(player, parsed.itemId)
            }
            if (built != null) return ResolveResult(true, built, source = reg.ownerKey)
            return ResolveResult(false, code = "MODULE_BUILD_FAILED", message = Lang.get("item-manager-api.module-build-failed", "alias" to alias, "id" to parsed.itemId), source = reg.ownerKey, suggestion = Lang.get("item-manager-api.module-build-failed-suggestion"))
        }

        val sourceType = ItemSourceType.fromId(alias) ?: return ResolveResult(false, code = "SOURCE_NOT_FOUND", message = Lang.get("item-manager-api.source-not-found", "alias" to alias), source = alias, suggestion = Lang.get("item-manager-api.source-not-found-suggestion"))
        val item = ItemSourceManager.getItem(sourceType.id, parsed.itemId, 1)
        return if (item != null) ResolveResult(true, item, source = sourceType.id)
        else ResolveResult(false, code = "ITEM_NOT_FOUND", message = Lang.get("item-manager-api.item-not-found", "source" to sourceType.id, "id" to parsed.itemId), source = sourceType.id, suggestion = Lang.get("item-manager-api.item-not-found-suggestion"))
    }

    private fun vanillaMaterial(spec: String): ItemStack? {
        val matName = spec.substringBefore(':').uppercase(Locale.ROOT)
        val material = Material.matchMaterial(matName) ?: return null
        return ItemStack(material, 1)
    }

    fun registerBuiltinSources() {
        for (type in ItemSourceType.entries) {
            val source = ItemSourceManager.getSource(type.id) ?: continue
            val aliases = (listOf(type.id) + type.aliases).distinct().toTypedArray()
            register(source, *aliases)
        }
    }
}
