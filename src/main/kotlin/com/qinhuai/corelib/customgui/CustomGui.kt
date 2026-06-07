package com.qinhuai.corelib.customgui

import com.qinhuai.corelib.item.ItemSourceManager
import com.qinhuai.corelib.util.TextUtil
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class CustomGui(
    val config: CustomGuiConfig,
    val player: Player,
    private val data: MutableMap<String, Any> = mutableMapOf(),
    var placeholderProvider: CustomGuiPlaceholderProvider? = null,
    var actionHandler: CustomGuiActionHandler? = null,
) {
    private var inventory: Inventory? = null
    private var currentPage = 0
    private val playerItems = mutableMapOf<Int, CustomGuiItem>()
    private var updateTask: Int? = null
    private val paginationData = mutableListOf<Any>()
    
    fun open() {
        val titleComponent = TextUtil.toComponent(replacePlaceholders(config.title))
        inventory = org.bukkit.Bukkit.createInventory(null, config.rows * 9, titleComponent)
        
        loadPaginationData()
        render()
        
        player.openInventory(inventory!!)
        CustomGuiListener.register(this)
        
        config.openSound?.let { playSound(it) }
        
        if (config.updateInterval > 0) {
            startAutoUpdate()
        }
    }
    
    fun refresh() {
        render()
    }
    
    fun close() {
        stopAutoUpdate()
        CustomGuiListener.unregister(this)
        config.closeSound?.let { playSound(it) }
    }
    
    fun handleClick(slot: Int, clickType: org.bukkit.event.inventory.ClickType, isShift: Boolean) {
        val pagination = config.pagination
        
        if (pagination != null) {
            if (slot == pagination.previousPageSlot && currentPage > 0) {
                currentPage--
                render()
                return
            }
            if (slot == pagination.nextPageSlot && currentPage < getMaxPage() - 1) {
                currentPage++
                render()
                return
            }
        }
        
        val item = playerItems[slot] ?: return
        
        if (item.viewRequirement != null) {
            if (!ConditionChecker.check(player, item.viewRequirement)) {
                return
            }
        }
        
        if (item.layoutAction != null || item.layoutActionLeft != null || item.layoutActionRight != null) {
            handleLayoutAction(item, clickType)
            return
        }
        
        ActionExecutor.execute(player, this, item.clickActions, clickType, isShift)
    }
    
    private fun handleLayoutAction(item: CustomGuiItem, clickType: org.bukkit.event.inventory.ClickType) {
        val isLeft = clickType.isLeftClick
        val isRight = clickType.isRightClick
        val isMiddle = clickType == org.bukkit.event.inventory.ClickType.MIDDLE
        
        val action = when {
            isLeft && item.layoutActionLeft != null -> item.layoutActionLeft
            isRight && item.layoutActionRight != null -> item.layoutActionRight
            item.layoutAction != null -> item.layoutAction
            else -> null
        }
        
        action?.let {
            actionHandler?.onAction(it, player, clickType)
        }
    }
    
    fun getCurrentPage(): Int = currentPage
    
    fun setCurrentPage(page: Int) {
        currentPage = page.coerceIn(0, getMaxPage() - 1)
        render()
    }
    
    fun getMaxPage(): Int {
        val pagination = config.pagination ?: return 1
        if (paginationData.isEmpty() || pagination.itemSlots.isEmpty()) return 1
        return (paginationData.size + pagination.itemSlots.size - 1) / pagination.itemSlots.size
    }
    
    fun getPaginationData(): List<Any> = paginationData
    
    fun setPaginationData(data: List<Any>) {
        paginationData.clear()
        paginationData.addAll(data)
        currentPage = 0
        render()
    }
    
    fun reloadPaginationData() {
        paginationData.clear()
        loadPaginationData()
    }

    private fun loadPaginationData() {
        val pagination = config.pagination ?: return
        
        when (pagination.sourceType.lowercase()) {
            "online_players" -> {
                paginationData.addAll(org.bukkit.Bukkit.getOnlinePlayers())
            }
            else -> {
                val provider = GuiDataProviderRegistry.getListProvider(pagination.sourceType)
                if (provider != null) {
                    paginationData.addAll(provider.loadEntries(player, this, pagination.sourceValue))
                } else {
                    org.bukkit.Bukkit.getLogger().warning(
                        "[QinhCoreLib] 未注册的分页数据源: ${pagination.sourceType} (value=${pagination.sourceValue})"
                    )
                }
            }
        }
    }
    
    private fun resolveItemStack(itemConfig: CustomGuiItem): ItemStack {
        val resolved = itemConfig.itemReference
            ?.takeIf { !it.contains("{") }
            ?.let { ItemSourceManager.parseItemReference(it) }
            ?: itemConfig.baseItem
        val itemStack = resolved.clone()
        itemStack.amount = itemConfig.amount
        return itemStack
    }

    private fun render() {
        val inv = inventory ?: return
        inv.clear()
        playerItems.clear()
        
        val pagination = config.pagination
        
        for ((slot, itemConfig) in config.items) {
            if (slot < 0 || slot >= inv.size) continue
            
            if (itemConfig.viewRequirement != null) {
                if (!ConditionChecker.check(player, itemConfig.viewRequirement)) {
                    continue
                }
            }
            
            val itemStack = resolveItemStack(itemConfig)
            val meta = itemStack.itemMeta
            
            if (meta != null) {
                val name = itemConfig.name?.let { replacePlaceholders(it) }
                val lore = replacePlaceholdersList(itemConfig.lore).filter { it.isNotBlank() }
                TextUtil.applyItemDisplay(meta, name, lore)
                itemConfig.customModelData?.let { meta.setCustomModelData(it) }
                itemStack.itemMeta = meta
            }
            
            inv.setItem(slot, itemStack)
            playerItems[slot] = itemConfig
        }
        
        if (pagination != null && paginationData.isNotEmpty()) {
            renderPaginationItems(pagination)
        }

        renderDynamicSlots()
    }

    private fun renderDynamicSlots() {
        val inv = inventory ?: return
        for (slotConfig in config.dynamicSlots) {
            val provider = GuiDataProviderRegistry.getSlotProvider(slotConfig.sourceType) ?: continue
            val stack = provider.loadItem(player, this, slotConfig.sourceValue) ?: continue
            val template = slotConfig.template
            val finalStack = if (template != null) {
                val entry = GuiPaginationEntry(displayItem = stack)
                GuiItemRenderer.buildFromTemplate(template, player, this, entry, slotConfig.slot)
            } else {
                stack
            }
            if (slotConfig.slot < 0 || slotConfig.slot >= inv.size) continue
            inv.setItem(slotConfig.slot, finalStack)
            template?.let { playerItems[slotConfig.slot] = it }
        }
    }
    
    private fun replacePlaceholders(text: String): String {
        var result = text
        
        placeholderProvider?.let { provider ->
            val pattern = Regex("\\{([^}]+)\\}")
            result = pattern.replace(result) { match ->
                val key = match.groupValues[1]
                provider.resolve(key) ?: match.value
            }
        }

        data.forEach { (key, value) ->
            result = result.replace("{$key}", value.toString())
        }
        
        result = PlaceholderManager.replace(result, player, this)
        
        return result
    }
    
    private fun replacePlaceholdersList(list: List<String>): List<String> {
        return list.map { replacePlaceholders(it) }
    }
    
    private fun renderPaginationItems(pagination: PaginationConfig) {
        val inv = inventory ?: return
        val template = pagination.itemTemplate ?: return
        val startIndex = currentPage * pagination.itemSlots.size
        
        for (i in pagination.itemSlots.indices) {
            val dataIndex = startIndex + i
            if (dataIndex >= paginationData.size) break
            
            val slot = pagination.itemSlots[i]
            if (slot < 0 || slot >= inv.size) continue
            
            val data = paginationData[dataIndex]
            val entry = data as? GuiPaginationEntry
            val itemStack = GuiItemRenderer.buildFromTemplate(template, player, this, entry, dataIndex)
            inv.setItem(slot, itemStack)

            val action = entry?.action ?: entry?.leftAction
                ?: pagination.defaultItemAction
                ?: template.layoutAction
            playerItems[slot] = template.withLayoutActions(
                action = action,
                left = entry?.leftAction ?: template.layoutActionLeft,
                right = entry?.rightAction ?: template.layoutActionRight
            )
            if (entry != null) {
                setData("entry_$slot", entry)
            }
        }
    }
    
    private fun playSound(soundStr: String) {
        val parts = soundStr.split(",")
        val soundName = parts.getOrNull(0) ?: "ENTITY_PLAYER_LEVELUP"
        val volume = parts.getOrNull(1)?.toFloatOrNull() ?: 1.0f
        val pitch = parts.getOrNull(2)?.toFloatOrNull() ?: 1.0f
        
        com.qinhuai.corelib.util.ServerCompat.resolveSound(soundName)?.let { sound ->
            player.playSound(player.location, sound, volume, pitch)
        }
    }
    
    private fun startAutoUpdate() {
        val task = com.qinhuai.corelib.scheduler.TaskScheduler.runSyncRepeating(0L, config.updateInterval) {
            refresh()
        }
        updateTask = task?.taskId
    }
    
    private fun stopAutoUpdate() {
        updateTask?.let { 
            org.bukkit.Bukkit.getScheduler().cancelTask(it)
        }
    }
    
    fun getData(key: String): Any? = data[key]
    
    fun setData(key: String, value: Any) {
        data[key] = value
    }
    
    fun getInventory(): Inventory? = inventory
}
