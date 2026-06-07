package com.qinhuai.corelib.customgui

import com.qinhuai.corelib.item.ItemSourceManager
import com.qinhuai.corelib.util.TextUtil
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

enum class GuiFormat {
    ITEMS,
    LAYOUT_ICONS
}

data class DynamicSlotConfig(
    val slot: Int,
    val sourceType: String,
    val sourceValue: String,
    val template: CustomGuiItem? = null
) {
    companion object {
        fun fromConfig(slot: Int, section: ConfigurationSection): DynamicSlotConfig {
            val template = section.getConfigurationSection("template")?.let { CustomGuiItem.fromConfig(it) }
            return DynamicSlotConfig(
                slot = slot,
                sourceType = section.getString("source-type", "") ?: "",
                sourceValue = section.getString("source-value", "") ?: "",
                template = template
            )
        }
    }
}

data class CustomGuiConfig(
    val id: String,
    val title: String,
    val rows: Int,
    val items: Map<Int, CustomGuiItem>,
    val updateInterval: Long = 0,
    val openSound: String? = null,
    val closeSound: String? = null,
    val pagination: PaginationConfig? = null,
    val dynamicSlots: List<DynamicSlotConfig> = emptyList(),
    val format: GuiFormat = GuiFormat.ITEMS
) {
    companion object {
        fun fromConfig(id: String, section: ConfigurationSection): CustomGuiConfig {
            val title = section.getString("title", "GUI") ?: "GUI"
            val rows = section.getInt("rows", 6)
            val updateInterval = section.getLong("update-interval", 0)
            val openSound = section.getString("open-sound")
            val closeSound = section.getString("close-sound")
            
            val pagination = section.getConfigurationSection("pagination")?.let {
                PaginationConfig.fromConfig(it)
            }

            val dynamicSlots = mutableListOf<DynamicSlotConfig>()
            section.getConfigurationSection("dynamic-items")?.let { dyn ->
                for (key in dyn.getKeys(false)) {
                    val slot = key.toIntOrNull() ?: continue
                    dyn.getConfigurationSection(key)?.let { slotSection ->
                        dynamicSlots.add(DynamicSlotConfig.fromConfig(slot, slotSection))
                    }
                }
            }
            
            val format = if (section.contains("layout")) GuiFormat.LAYOUT_ICONS else GuiFormat.ITEMS
            
            val items = mutableMapOf<Int, CustomGuiItem>()
            
            if (format == GuiFormat.LAYOUT_ICONS) {
                val layout = section.getStringList("layout")
                val iconsSection = section.getConfigurationSection("icons")
                
                if (iconsSection != null) {
                    for (charKey in iconsSection.getKeys(false)) {
                        val iconSection = iconsSection.getConfigurationSection(charKey) ?: continue
                        val char = charKey.firstOrNull() ?: continue
                        
                        val item = CustomGuiItem.fromLayoutIconConfig(iconSection)
                        
                        layout.forEachIndexed { row, line ->
                            line.forEachIndexed { col, c ->
                                if (c == char) {
                                    val slot = row * 9 + col
                                    items[slot] = item
                                }
                            }
                        }
                    }
                }
            } else {
                val itemsSection = section.getConfigurationSection("items")
                
                if (itemsSection != null) {
                    for (key in itemsSection.getKeys(false)) {
                        val itemSection = itemsSection.getConfigurationSection(key) ?: continue
                        
                        val slots = parseSlots(key)
                        val item = CustomGuiItem.fromConfig(itemSection)
                        
                        for (slot in slots) {
                            items[slot] = item
                        }
                    }
                }
            }
            
            return CustomGuiConfig(
                id = id,
                title = title,
                rows = rows,
                items = items,
                updateInterval = updateInterval,
                openSound = openSound,
                closeSound = closeSound,
                pagination = pagination,
                dynamicSlots = dynamicSlots,
                format = format
            )
        }
        
        fun parseSlots(slotStr: String): List<Int> {
            val result = mutableListOf<Int>()
            for (part in slotStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }) {
                if (part.contains("-")) {
                    val rangeParts = part.split("-", limit = 2)
                    val start = rangeParts.getOrNull(0)?.toIntOrNull() ?: continue
                    val end = rangeParts.getOrNull(1)?.toIntOrNull() ?: continue
                    for (i in start..end) {
                        result.add(i)
                    }
                } else {
                    part.toIntOrNull()?.let { result.add(it) }
                }
            }
            return result
        }
    }
}

data class CustomGuiItem(
    val baseItem: ItemStack,
    val name: String?,
    val lore: List<String>,
    val itemReference: String? = null,
    val customModelData: Int? = null,
    val amount: Int = 1,
    val priority: Int = 0,
    val viewRequirement: ViewRequirement? = null,
    val clickActions: List<ClickAction> = emptyList(),
    val updateActions: List<UpdateAction> = emptyList(),
    val layoutAction: String? = null,
    val layoutActionLeft: String? = null,
    val layoutActionRight: String? = null
) {
    fun withLayoutActions(
        action: String? = layoutAction,
        left: String? = layoutActionLeft,
        right: String? = layoutActionRight
    ): CustomGuiItem = copy(
        layoutAction = action,
        layoutActionLeft = left,
        layoutActionRight = right
    )
    fun build(): ItemStack {
        val item = baseItem.clone()
        item.amount = amount
        val meta = item.itemMeta
        
        if (meta != null) {
            TextUtil.applyItemDisplay(meta, name, lore)
            customModelData?.let { meta.setCustomModelData(it) }
            item.itemMeta = meta
        }
        
        return item
    }
    
    companion object {
        private fun materialStack(section: ConfigurationSection, amount: Int): ItemStack {
            val materialName = (section.getString("material", "STONE") ?: "STONE").uppercase()
            return try {
                ItemStack(Material.valueOf(materialName), amount)
            } catch (_: Exception) {
                ItemStack(Material.STONE, amount)
            }
        }

        private fun resolveBaseItem(section: ConfigurationSection, amount: Int): ItemStack {
            val itemReference = section.getString("item")
            val fallback = materialStack(section, amount)
            if (itemReference != null && !itemReference.contains("{")) {
                return ItemSourceManager.parseItemReference(itemReference) ?: fallback
            }
            if (section.contains("item")) {
                return ItemStack(Material.PAPER, amount)
            }
            return fallback
        }

        fun fromConfig(section: ConfigurationSection): CustomGuiItem {
            val amount = section.getInt("amount", 1)
            val priority = section.getInt("priority", 0)
            
            val itemReference = section.getString("item")
            val baseItem = resolveBaseItem(section, amount)
            
            val name = section.getString("name")
            val lore = section.getStringList("lore")
            val customModelData = if (section.contains("custom-model-data")) section.getInt("custom-model-data") else null
            
            val viewRequirement = section.getConfigurationSection("view-requirement")?.let {
                ViewRequirement.fromConfig(it)
            }
            
            val clickActions = mutableListOf<ClickAction>()
            section.getConfigurationSection("click-actions")?.let { actionsSection ->
                for (key in actionsSection.getKeys(false)) {
                    actionsSection.getConfigurationSection(key)?.let { actionSection ->
                        clickActions.add(ClickAction.fromConfig(key, actionSection))
                    }
                }
            }
            
            val updateActions = mutableListOf<UpdateAction>()
            section.getConfigurationSection("update-actions")?.let { actionsSection ->
                for (key in actionsSection.getKeys(false)) {
                    actionsSection.getConfigurationSection(key)?.let { actionSection ->
                        updateActions.add(UpdateAction.fromConfig(key, actionSection))
                    }
                }
            }
            
            return CustomGuiItem(
                baseItem = baseItem,
                name = name,
                lore = lore,
                itemReference = itemReference,
                customModelData = customModelData,
                amount = amount,
                priority = priority,
                viewRequirement = viewRequirement,
                clickActions = clickActions,
                updateActions = updateActions
            )
        }
        
        fun fromLayoutIconConfig(section: ConfigurationSection): CustomGuiItem {
            val displaySection = section.getConfigurationSection("display") ?: section
            val amount = displaySection.getInt("amount", 1)
            val itemReference = displaySection.getString("item")
            
            val baseItem = resolveBaseItem(displaySection, amount)
            
            val name = displaySection.getString("name")
            val lore = displaySection.getStringList("lore")
            val customModelData = if (displaySection.contains("custom-model-data")) displaySection.getInt("custom-model-data") else null
            
            val layoutAction = section.getString("action")
            val layoutActionLeft = section.getString("action_left")
            val layoutActionRight = section.getString("action_right")
            
            return CustomGuiItem(
                baseItem = baseItem,
                name = name,
                lore = lore,
                itemReference = itemReference,
                customModelData = customModelData,
                amount = amount,
                priority = 0,
                viewRequirement = null,
                clickActions = emptyList(),
                updateActions = emptyList(),
                layoutAction = layoutAction,
                layoutActionLeft = layoutActionLeft,
                layoutActionRight = layoutActionRight
            )
        }
    }
}

data class ViewRequirement(
    val type: String,
    val value: String,
    val negate: Boolean = false
) {
    companion object {
        fun fromConfig(section: ConfigurationSection): ViewRequirement {
            return ViewRequirement(
                type = section.getString("type", "permission") ?: "permission",
                value = section.getString("value", "") ?: "",
                negate = section.getBoolean("negate", false)
            )
        }
    }
}

data class ClickAction(
    val type: String,
    val clickTypes: List<String>,
    val value: String,
    val shift: Boolean = false
) {
    companion object {
        fun fromConfig(type: String, section: ConfigurationSection): ClickAction {
            return ClickAction(
                type = type,
                clickTypes = section.getStringList("click-types").ifEmpty { listOf("LEFT") },
                value = section.getString("value", "") ?: "",
                shift = section.getBoolean("shift", false)
            )
        }
    }
}

data class UpdateAction(
    val type: String,
    val value: String
) {
    companion object {
        fun fromConfig(type: String, section: ConfigurationSection): UpdateAction {
            return UpdateAction(
                type = type,
                value = section.getString("value", "") ?: ""
            )
        }
    }
}

data class PaginationConfig(
    val sourceType: String,
    val sourceValue: String,
    val itemSlots: List<Int>,
    val previousPageSlot: Int,
    val nextPageSlot: Int,
    val pageInfoSlot: Int? = null,
    val itemTemplate: CustomGuiItem? = null,
    val defaultItemAction: String? = null
) {
    companion object {
        fun fromConfig(section: ConfigurationSection): PaginationConfig? {
            if (!section.contains("source-type")) return null
            
            val itemTemplate = section.getConfigurationSection("item-template")?.let {
                CustomGuiItem.fromConfig(it)
            }
            
            return PaginationConfig(
                sourceType = section.getString("source-type", "") ?: "",
                sourceValue = section.getString("source-value", "") ?: "",
                itemSlots = CustomGuiConfig.parseSlots(section.getString("item-slots", "") ?: ""),
                previousPageSlot = section.getInt("previous-page-slot", -1),
                nextPageSlot = section.getInt("next-page-slot", -1),
                pageInfoSlot = if (section.contains("page-info-slot")) section.getInt("page-info-slot") else null,
                itemTemplate = itemTemplate,
                defaultItemAction = section.getString("default-action")
            )
        }
    }
}
