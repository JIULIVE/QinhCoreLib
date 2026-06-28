package com.qinhuai.corelib.script

import com.qinhuai.corelib.economy.EconomyBridge
import com.qinhuai.corelib.item.ItemSourceManager
import com.qinhuai.corelib.placeholder.PapiBridge
import com.qinhuai.corelib.scheduler.TaskScheduler
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
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
    fun heal(amount: Double): Boolean {
        val player = context.player ?: return false
        if (!amount.isFinite() || amount <= 0.0) return false
        val maxHealth = org.bukkit.Registry.ATTRIBUTE.get(org.bukkit.NamespacedKey.minecraft("max_health"))
            ?.let { player.getAttribute(it)?.value } ?: 20.0
        player.health = (player.health + amount).coerceIn(0.0, maxHealth)
        return true
    }

    @HostAccess.Export
    fun damage(target: Any?, amount: Double): Boolean {
        val victim = target as? LivingEntity ?: return false
        if (!amount.isFinite() || amount <= 0.0) return false
        val source = context.player
        return runCatching {
            if (source != null && source !== victim) victim.damage(amount, source) else victim.damage(amount)
            true
        }.getOrDefault(false)
    }

    @Suppress("DEPRECATION")
    @HostAccess.Export
    fun addPotion(target: Any?, type: String, durationTicks: Int, amplifier: Int): Boolean {
        val entity = target as? LivingEntity ?: return false
        val effectType = PotionEffectType.getByName(type.trim().uppercase()) ?: return false
        return runCatching {
            entity.addPotionEffect(PotionEffect(effectType, durationTicks.coerceAtLeast(1), amplifier.coerceAtLeast(0)))
            true
        }.getOrDefault(false)
    }

    @HostAccess.Export
    fun buff(target: Any?, key: String, amount: Double, operation: String?, durationTicks: Long, source: String?): Boolean {
        val entity = (target as? LivingEntity) ?: context.player ?: return false
        if (key.isBlank() || !amount.isFinite()) return false
        val op = com.qinhuai.corelib.attribute.ModifierOp.parse(operation)
        val src = source?.trim()?.takeIf { it.isNotEmpty() } ?: "qcl_script:$scriptPath"
        com.qinhuai.corelib.attribute.AttributeService.refreshBuff(
            entity, key.trim().lowercase(), amount, src, durationTicks.coerceAtLeast(0L), op,
        )
        return true
    }

    @HostAccess.Export
    fun removeBuff(target: Any?, source: String): Boolean {
        val entity = (target as? LivingEntity) ?: context.player ?: return false
        if (source.isBlank()) return false
        com.qinhuai.corelib.attribute.AttributeService.removeModifierSource(entity, source.trim())
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
