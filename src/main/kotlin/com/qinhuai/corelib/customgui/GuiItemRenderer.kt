package com.qinhuai.corelib.customgui

import com.qinhuai.corelib.item.ItemSourceManager
import com.qinhuai.corelib.util.TextUtil
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object GuiItemRenderer {
    private val miniMessage = MiniMessage.miniMessage()
    private val legacyAmpersand = LegacyComponentSerializer.legacyAmpersand()

    fun buildFromTemplate(
        template: CustomGuiItem,
        player: Player,
        gui: CustomGui,
        entry: GuiPaginationEntry? = null,
        itemIndex: Int = 0
    ): ItemStack {
        val base = resolveBaseItem(template, player, gui, entry, itemIndex) ?: template.baseItem.clone()
        base.amount = template.amount
        val meta = base.itemMeta ?: return base

        template.name?.let { meta.displayName(toComponent(replaceAll(it, player, gui, entry, itemIndex))) }
        val lore = template.lore
            .map { replaceAll(it, player, gui, entry, itemIndex) }
            .filter { it.isNotBlank() }
        if (lore.isNotEmpty()) {
            meta.lore(lore.map { toComponent(it) })
        }

        val cmd = entry?.placeholders?.get("custom_model_data")?.toIntOrNull() ?: template.customModelData
        cmd?.let { meta.setCustomModelData(it) }

        base.itemMeta = meta
        return base
    }

    fun resolveBaseItem(
        template: CustomGuiItem,
        player: Player,
        gui: CustomGui,
        entry: GuiPaginationEntry?,
        itemIndex: Int
    ): ItemStack? {
        entry?.displayItem?.let { return it.clone() }
        val ref = template.itemReference ?: return null
        if (ref.contains("{")) {
            val resolved = replaceAll(ref, player, gui, entry, itemIndex)
            return ItemSourceManager.parseItemReference(resolved)
        }
        return ItemSourceManager.parseItemReference(ref)
    }

    fun replaceAll(
        text: String,
        player: Player,
        gui: CustomGui,
        entry: GuiPaginationEntry? = null,
        itemIndex: Int = 0
    ): String {
        var result = text
        entry?.placeholders?.forEach { (k, v) -> result = result.replace("{$k}", v) }
        val data: Any? = entry
        result = PlaceholderManager.replace(result, player, gui, data, itemIndex)
        return result
    }

    private fun toComponent(text: String): net.kyori.adventure.text.Component {
        val hasMini = text.contains("<") && text.contains(">")
        return if (hasMini) {
            try {
                miniMessage.deserialize(text).decoration(TextDecoration.ITALIC, false)
            } catch (_: Exception) {
                legacyAmpersand.deserialize(TextUtil.colored(text)).decoration(TextDecoration.ITALIC, false)
            }
        } else {
            legacyAmpersand.deserialize(TextUtil.colored(text)).decoration(TextDecoration.ITALIC, false)
        }
    }
}
