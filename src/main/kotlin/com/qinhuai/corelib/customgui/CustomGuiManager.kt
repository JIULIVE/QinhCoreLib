package com.qinhuai.corelib.customgui

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import java.io.File

object CustomGuiManager {
    private val guiConfigs = mutableMapOf<String, CustomGuiConfig>()
    private var guiFolder: File? = null
    
    fun init(plugin: com.qinhuai.corelib.QinhCoreLib) {
        guiFolder = File(plugin.dataFolder, "guis")
        
        if (!guiFolder!!.exists()) {
            guiFolder!!.mkdirs()
            createExampleGui()
        }
        
        loadAllGuis()
        
        plugin.server.pluginManager.registerEvents(CustomGuiListener, plugin)
    }

    fun loadedGuiCount(): Int = guiConfigs.size
    
    fun loadAllGuis() {
        guiConfigs.clear()
        
        guiFolder?.listFiles { _, name -> name.endsWith(".yml") }?.forEach { file ->
            try {
                val config = YamlConfiguration.loadConfiguration(file)
                val id = file.nameWithoutExtension
                
                for (key in config.getKeys(false)) {
                    val section = config.getConfigurationSection(key) ?: continue
                    val guiConfig = CustomGuiConfig.fromConfig(key, section)
                    guiConfigs[guiConfig.id] = guiConfig
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun reloadAllGuis() {
        loadAllGuis()
    }
    
    fun getGuiConfig(id: String): CustomGuiConfig? {
        return guiConfigs[id]
    }
    
    fun openGui(
        player: Player, 
        id: String,
        placeholderProvider: CustomGuiPlaceholderProvider? = null,
        actionHandler: CustomGuiActionHandler? = null,
    ): CustomGui? {
        val config = getGuiConfig(id) ?: return null
        val gui = CustomGui(config, player)
        gui.placeholderProvider = placeholderProvider
        gui.actionHandler = actionHandler
        gui.open()
        return gui
    }
    
    fun openGuiFromConfig(
        player: Player,
        section: org.bukkit.configuration.ConfigurationSection,
        guiId: String = "custom",
        sessionData: Map<String, Any> = emptyMap(),
        placeholderProvider: CustomGuiPlaceholderProvider? = null,
        actionHandler: CustomGuiActionHandler? = null,
    ): CustomGui {
        val config = CustomGuiConfig.fromConfig(guiId, section)
        val gui = CustomGui(config, player)
        sessionData.forEach { (k, v) -> gui.setData(k, v) }
        gui.placeholderProvider = placeholderProvider
        gui.actionHandler = actionHandler
        gui.open()
        return gui
    }

    /**
     * 打开由代码绘制的动态 GUI（与 YAML 配置的 [openGui] 共用同一套监听与点击节流）。
     */
    fun openDynamic(
        player: Player,
        rows: Int,
        title: String,
        openSound: String? = null,
        closeSound: String? = null,
        render: DynamicGuiRenderer,
    ): DynamicGui {
        val gui = DynamicGui(player, rows, title, openSound, closeSound, render)
        gui.open()
        return gui
    }

    fun openDynamicFromConfig(
        player: Player,
        section: org.bukkit.configuration.ConfigurationSection,
        guiId: String = "dynamic",
        render: DynamicGuiRenderer,
    ): DynamicGui {
        val title = section.getString("title", "GUI") ?: "GUI"
        val rows = section.getInt("rows", 6)
        val openSound = section.getString("open-sound")
        val closeSound = section.getString("close-sound")
        return openDynamic(player, rows, title, openSound, closeSound, render)
    }
    
    fun registerPlaceholder(key: String, provider: (Player) -> String) {
        PlaceholderManager.registerPlaceholder(key, provider)
    }
    
    private fun createExampleGui() {
        val exampleFile = File(guiFolder, "example.yml")
        if (!exampleFile.exists()) {
            exampleFile.writeText("""
# 示例GUI配置文件
# 参考 TrMenu 风格

example_gui:
  title: "&6示例菜单"
  rows: 6
  update-interval: 20
  
  items:
    # 边框
    0-8,9,17,18,26,27,35,36,44,45-53:
      material: BLACK_STAINED_GLASS_PANE
      name: " "
      priority: 1
      
    # 玩家头颅
    13:
      material: PLAYER_HEAD
      name: "&a欢迎, {player}!"
      lore:
        - "&7当前世界: {world}"
        - "&7在线人数: {online}/{max_players}"
        - "&e点击刷新!"
      click-actions:
        refresh:
          click-types: [LEFT, RIGHT]
          type: refresh
          
    # 关闭按钮
    49:
      material: BARRIER
      name: "&c关闭菜单"
      lore:
        - "&7点击关闭"
      click-actions:
        close:
          click-types: [LEFT]
          type: close
          sound: ENTITY_VILLAGER_NO,1,1
          
    # 传送按钮
    22:
      material: COMPASS
      name: "&b传送到主城"
      lore:
        - "&7点击传送到主城"
      view-requirement:
        type: permission
        value: qinhcorelib.teleport
      click-actions:
        teleport:
          click-types: [LEFT]
          type: teleport
          value: 0,100,0,world
          sound: ENTITY_ENDERMAN_TELEPORT,1,1
          
    # 给予物品按钮
    31:
      material: DIAMOND
      name: "&b领取钻石"
      lore:
        - "&7点击领取一颗钻石"
      click-actions:
        give:
          click-types: [LEFT]
          type: give_item
          value: DIAMOND:1
        message:
          click-types: [LEFT]
          type: message
          value: "&a你领取了一颗钻石!"
        sound:
          click-types: [LEFT]
          type: sound
          value: ENTITY_PLAYER_LEVELUP,1,1

    # 经济动作示例（需 Vault / ExcellentEconomy / PlayerPoints）
    40:
      material: GOLD_INGOT
      name: "&6领取 100 铜钱"
      click-actions:
        pay:
          click-types: [LEFT]
          type: give_money
          value: "100:money"
        msg:
          click-types: [LEFT]
          type: message
          value: "&a已发放 100 铜钱"
    41:
      material: IRON_INGOT
      name: "&7扣除 10 铜钱"
      click-actions:
        charge:
          click-types: [LEFT]
          type: take_money
          value: "10:money|&c铜钱不足"

# 分页示例 - 在线玩家列表
players_gui:
  title: "&6在线玩家 - 第{page}/{max_page}页"
  rows: 6
  
  pagination:
    source-type: online_players
    item-slots: 10-16,19-25,28-34
    previous-page-slot: 45
    next-page-slot: 53
    page-info-slot: 49
    
    item-template:
      material: PLAYER_HEAD
      name: "&b{item_name}"
      lore:
        - "&7UUID: {item_uuid}"
        - "&e点击传送!"
      click-actions:
        teleport:
          click-types: [LEFT]
          type: message
          value: "&c需要在代码中实现传送逻辑!"
  
  items:
    # 边框
    0-8,9,17,18,26,27,35,36,44:
      material: GRAY_STAINED_GLASS_PANE
      name: " "
      
    # 上一页按钮
    45:
      material: ARROW
      name: "&c上一页"
      
    # 下一页按钮
    53:
      material: ARROW
      name: "&a下一页"
      
    # 页面信息
    49:
      material: PAPER
      name: "&b第{page}/{max_page}页"
      lore:
        - "&7共 {online} 名玩家在线"
""".trimIndent())
        }
    }
}
