package com.qinhuai.corelib.attribute

import com.qinhuai.corelib.lang.Lang
import com.qinhuai.corelib.util.TextUtil
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta

object AttributeBook {

    fun open(player: Player) {
        val book = ItemStack(Material.WRITTEN_BOOK)
        val meta = book.itemMeta as? BookMeta ?: return
        meta.title(TextUtil.toComponent(Lang.get("attribute-book.title")))
        meta.author(TextUtil.toComponent(Lang.get("attribute-book.author")))

        val byCat = AttributeRegistry.all().groupBy { it.category }
        val pages = mutableListOf<Component>()

        val cover = StringBuilder()
        cover.append(Lang.get("attribute-book.cover-header", "name" to player.name))
        cover.append(Lang.get("attribute-book.cover-combat-power", "value" to fmt(AttributeService.combatPower(player))))
        cover.append(Lang.get("attribute-book.cover-backend", "id" to AttributeService.activeId()))
        cover.append(Lang.get("attribute-book.cover-counts", "categories" to byCat.size, "attributes" to AttributeRegistry.all().size))
        cover.append(Lang.get("attribute-book.cover-legend"))
        pages.add(TextUtil.toComponent(cover.toString()))

        for ((cat, defs) in byCat) {
            val sb = StringBuilder()
            sb.append("§0§l$cat\n§8§m                \n")
            for (def in defs) {
                val v = AttributeService.displayValue(player, def)
                if (v != 0.0) {
                    sb.append("§2${def.displayName}§0: §1${fmt(v)}\n")
                } else {
                    sb.append("§8${def.displayName}: 0\n")
                }
            }
            pages.add(TextUtil.toComponent(sb.toString()))
        }

        meta.pages(pages)
        book.itemMeta = meta
        player.openBook(book)
    }

    private fun fmt(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.2f", v)
}
