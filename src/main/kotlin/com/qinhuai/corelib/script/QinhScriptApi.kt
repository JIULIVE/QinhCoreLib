package com.qinhuai.corelib.script

import com.qinhuai.corelib.economy.EconomyBridge
import com.qinhuai.corelib.item.ItemSourceManager
import com.qinhuai.corelib.placeholder.PapiBridge
import com.qinhuai.corelib.scheduler.TaskScheduler
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.graalvm.polyglot.HostAccess
import java.util.concurrent.CompletableFuture

class QinhScriptContextApi(
    private val context: ScriptContext,
) {
    @HostAccess.Export
    fun player(): Player? = context.player

    @HostAccess.Export
    fun pluginName(): String = context.plugin.name

    @HostAccess.Export
    fun get(key: String): Any? = context.variables[key]

    @HostAccess.Export
    fun set(key: String, value: Any?) {
        if (value == null) {
            context.variables.remove(key)
        } else {
            context.variables[key] = value
        }
    }

    @HostAccess.Export
    fun vars(): Map<String, Any> = context.variables.toMap()
}

class QinhScriptApi(
    private val context: ScriptContext,
    private val scriptPath: String,
) {
    val ctx: QinhScriptContextApi = QinhScriptContextApi(context)

    @HostAccess.Export
    fun logInfo(message: String) {
        context.plugin.logger.info("[QCL-JS/$scriptPath] $message")
    }

    @HostAccess.Export
    fun logWarn(message: String) {
        context.plugin.logger.warning("[QCL-JS/$scriptPath] $message")
    }

    @HostAccess.Export
    fun logError(message: String) {
        context.plugin.logger.severe("[QCL-JS/$scriptPath] $message")
    }

    @HostAccess.Export
    fun bridgeStatusNames(): List<String> = com.qinhuai.corelib.debug.BridgeStatusRegistry.all().map { it.name }

    @HostAccess.Export
    fun placeholder(text: String): String {
        val player = context.player ?: return text
        return PapiBridge.apply(player, text)
    }

    @HostAccess.Export
    fun economyHas(amount: Double, provider: String?, currency: String?): Boolean {
        val player = context.player ?: return false
        return EconomyBridge.has(player, amount, provider, currency)
    }

    @HostAccess.Export
    fun economyWithdraw(amount: Double, provider: String?, currency: String?): Boolean {
        val player = context.player ?: return false
        return EconomyBridge.withdraw(player, amount, provider, currency).success
    }

    @HostAccess.Export
    fun economyDeposit(amount: Double, provider: String?, currency: String?): Boolean {
        val player = context.player ?: return false
        return EconomyBridge.deposit(player, amount, provider, currency).success
    }

    @HostAccess.Export
    fun itemParse(ref: String): Boolean {
        return ItemSourceManager.parseItemReference(ref) != null
    }

    @HostAccess.Export
    fun itemGive(ref: String, amount: Int): Boolean {
        val player = context.player ?: return false
        val stack = ItemSourceManager.parseItemReference(ref)?.clone() ?: return false
        stack.amount = amount.coerceAtLeast(1)
        player.inventory.addItem(stack)
        return true
    }

    @HostAccess.Export
    fun runSync(task: Runnable) {
        if (Bukkit.isPrimaryThread()) {
            task.run()
            return
        }
        TaskScheduler.runSync(task)
    }

    @HostAccess.Export
    fun runSyncLater(ticks: Long, task: Runnable) {
        TaskScheduler.runSyncLater(ticks.coerceAtLeast(0L), task)
    }

    @HostAccess.Export
    fun runSyncAndWait(task: Runnable): CompletableFuture<Void> {
        if (Bukkit.isPrimaryThread()) {
            task.run()
            return CompletableFuture.completedFuture(null)
        }
        val future = CompletableFuture<Void>()
        TaskScheduler.runSync {
            try {
                task.run()
                future.complete(null)
            } catch (t: Throwable) {
                future.completeExceptionally(t)
            }
        }
        return future
    }
}
