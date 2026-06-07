package com.qinhuai.corelib.scheduler

import com.qinhuai.corelib.QinhCoreLib
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

/**
 * Bukkit 任务调度。公开 API 使用 [Runnable] / [Supplier]，避免子插件与 CoreLib 各自加载 Kotlin 时的 [LinkageError]。
 */
object TaskScheduler {

    fun runSync(block: Runnable): BukkitTask? {
        return Bukkit.getScheduler().runTask(QinhCoreLib.instance, block)
    }

    fun runAsync(block: Runnable): BukkitTask? {
        return Bukkit.getScheduler().runTaskAsynchronously(QinhCoreLib.instance, block)
    }

    fun runSyncLater(delay: Long, block: Runnable): BukkitTask? {
        return Bukkit.getScheduler().runTaskLater(QinhCoreLib.instance, block, delay)
    }

    fun runAsyncLater(delay: Long, block: Runnable): BukkitTask? {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(QinhCoreLib.instance, block, delay)
    }

    fun runSyncRepeating(delay: Long, period: Long, block: Runnable): BukkitTask? {
        return Bukkit.getScheduler().runTaskTimer(QinhCoreLib.instance, block, delay, period)
    }

    fun runAsyncRepeating(delay: Long, period: Long, block: Runnable): BukkitTask? {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(QinhCoreLib.instance, block, delay, period)
    }

    fun <T> supplySync(block: Supplier<T>): CompletableFuture<T> {
        val future = CompletableFuture<T>()
        Bukkit.getScheduler().runTask(QinhCoreLib.instance, Runnable {
            try {
                future.complete(block.get())
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        })
        return future
    }

    fun <T> supplyAsync(block: Supplier<T>): CompletableFuture<T> {
        val future = CompletableFuture<T>()
        Bukkit.getScheduler().runTaskAsynchronously(QinhCoreLib.instance, Runnable {
            try {
                future.complete(block.get())
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        })
        return future
    }
}

class ThrottledExecutor(private val throttleMs: Long) {
    private val lastExecuteTime = mutableMapOf<String, Long>()

    fun execute(key: String, block: Runnable) {
        val now = System.currentTimeMillis()
        val last = lastExecuteTime[key] ?: 0L

        if (now - last >= throttleMs) {
            lastExecuteTime[key] = now
            block.run()
        }
    }

    fun clear() {
        lastExecuteTime.clear()
    }
}

class CooldownManager {
    private val cooldowns = mutableMapOf<String, Long>()

    fun hasCooldown(key: String): Boolean {
        val now = System.currentTimeMillis()
        return cooldowns[key]?.let { it > now } ?: false
    }

    fun getRemaining(key: String): Long {
        val now = System.currentTimeMillis()
        return cooldowns[key]?.let { (it - now).coerceAtLeast(0L) } ?: 0L
    }

    fun setCooldown(key: String, duration: Long, unit: TimeUnit = TimeUnit.MILLISECONDS) {
        val now = System.currentTimeMillis()
        cooldowns[key] = now + unit.toMillis(duration)
    }

    fun removeCooldown(key: String) {
        cooldowns.remove(key)
    }

    fun clear() {
        cooldowns.clear()
    }
}
