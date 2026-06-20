package com.qinhuai.corelib.placeholder

import org.bukkit.OfflinePlayer

interface QinhPlaceholderProvider {
    val identifier: String
    fun onRequest(player: OfflinePlayer?, params: String): String?
}
