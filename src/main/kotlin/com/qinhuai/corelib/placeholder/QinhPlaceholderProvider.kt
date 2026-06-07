package com.qinhuai.corelib.placeholder

import org.bukkit.OfflinePlayer

/**
 * 秦淮系列插件向 PlaceholderAPI 注册占位符的接口。
 * 子插件实现此接口，通过 [PapiBridge.register] 注册，无需依赖 PAPI Maven 坐标。
 */
interface QinhPlaceholderProvider {
    val identifier: String
    fun onRequest(player: OfflinePlayer?, params: String): String?
}
