package com.qinhuai.corelib.database

import com.qinhuai.corelib.QinhCoreLib
import com.qinhuai.corelib.debug.BridgeStatus
import com.qinhuai.corelib.debug.BridgeStatusRegistry
import com.qinhuai.corelib.debug.DiagnosticResult
import com.qinhuai.corelib.lang.Lang
import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.*

abstract class DatabaseConfig {
    abstract val type: DatabaseType
    abstract val host: String
    abstract val port: Int
    abstract val database: String
    abstract val username: String
    abstract val password: String
}

enum class DatabaseType {
    SQLITE, MYSQL
}

abstract class QclDatabase {
    abstract fun init()
    abstract fun getConnection(owner: UUID? = null): Connection
    abstract fun close()

    fun serializeLocation(loc: Location): String {
        return "${loc.world?.name ?: "world"},${loc.x},${loc.y},${loc.z},${loc.yaw},${loc.pitch}"
    }

    fun deserializeLocation(s: String?): Location? {
        if (s.isNullOrEmpty()) return null
        val parts = s.split(",")
        if (parts.size < 4) return null
        val world = org.bukkit.Bukkit.getWorld(parts[0]) ?: return null
        return Location(
            world,
            parts[1].toDouble(),
            parts[2].toDouble(),
            parts[3].toDouble(),
            parts.getOrNull(4)?.toFloat() ?: 0f,
            parts.getOrNull(5)?.toFloat() ?: 0f
        )
    }

    fun serializeLocations(locs: List<Location>): String {
        return locs.joinToString(";") { serializeLocation(it) }
    }

    fun deserializeLocations(s: String?): List<Location> {
        if (s.isNullOrEmpty()) return emptyList()
        return s.split(";").mapNotNull { deserializeLocation(it) }
    }

    fun serializeInventory(inv: List<ItemStack?>): String {
        return try {
            val payload = inv.map { stack -> stack ?: ItemStack.empty() }
            Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(payload))
        } catch (e: Exception) {
            ""
        }
    }

    fun deserializeInventory(s: String, size: Int): MutableList<ItemStack?> {
        val target = MutableList<ItemStack?>(size) { null }
        if (s.isEmpty()) return target
        return try {
            val items = ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(s))
            items.forEachIndexed { index, stack ->
                if (index < size) {
                    target[index] = stack.takeUnless { it.isEmpty }
                }
            }
            target
        } catch (_: Exception) {
            deserializeInventoryLegacy(s, size)
        }
    }

    @Suppress("DEPRECATION")
    private fun deserializeInventoryLegacy(s: String, size: Int): MutableList<ItemStack?> {
        val target = MutableList<ItemStack?>(size) { null }
        try {
            val inputStream = java.io.ByteArrayInputStream(Base64.getDecoder().decode(s))
            val dataInput = org.bukkit.util.io.BukkitObjectInputStream(inputStream)
            val readSize = dataInput.readInt()
            for (i in 0 until readSize) {
                val item = dataInput.readObject() as? ItemStack
                if (i < target.size) {
                    target[i] = item
                }
            }
            dataInput.close()
        } catch (e: Exception) {
            QinhCoreLib.instance.logger.warning(Lang.get("database-manager.restore-inventory-failed", "error" to e.message))
        }
        return target
    }
}

class SQLiteDatabase(
    private val dataFolder: File
) : QclDatabase() {

    init {
        if (!dataFolder.exists()) dataFolder.mkdirs()
    }

    override fun init() {
    }

    override fun getConnection(owner: UUID?): Connection {
        val fileName = if (owner == null) "global.db" else "${owner}.db"
        val file = File(dataFolder, fileName)
        val url = "jdbc:sqlite:${file.absolutePath}"
        return DriverManager.getConnection(url)
    }

    override fun close() {
    }

    fun bridgeStatus(): BridgeStatus = BridgeStatus(
        name = "SQLite",
        available = true,
        enabled = true,
        source = "Database",
        message = Lang.get("database-manager.sqlite-available"),
        recoverable = true,
    )

    fun diagnose(): DiagnosticResult<BridgeStatus> = DiagnosticResult.ok(bridgeStatus(), source = "database")

    fun getAllDatabaseFiles(): List<File> {
        return dataFolder.listFiles { f -> f.extension == "db" }?.toList() ?: emptyList()
    }
}

class MySQLDatabase(
    private val host: String,
    private val port: Int,
    private val database: String,
    private val username: String,
    private val password: String
) : QclDatabase() {

    private val url: String
        get() = "jdbc:mysql://$host:$port/$database?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"

    override fun init() {
    }

    override fun getConnection(owner: UUID?): Connection {
        return DriverManager.getConnection(url, username, password)
    }

    override fun close() {
    }

    fun bridgeStatus(): BridgeStatus = BridgeStatus(
        name = "MySQL",
        available = true,
        enabled = true,
        source = "Database",
        message = Lang.get("database-manager.mysql-available"),
        recoverable = true,
    )

    fun diagnose(): DiagnosticResult<BridgeStatus> = DiagnosticResult.ok(bridgeStatus(), source = "database")
}

object DatabaseManager {
    private var database: QclDatabase? = null
    private var type: DatabaseType = DatabaseType.SQLITE

    fun init(config: DatabaseConfig) {
        type = config.type
        database = when (config.type) {
            DatabaseType.SQLITE -> SQLiteDatabase(File(QinhCoreLib.instance.dataFolder, "data"))
            DatabaseType.MYSQL -> MySQLDatabase(
                config.host,
                config.port,
                config.database,
                config.username,
                config.password
            )
        }
        database?.init()
        BridgeStatusRegistry.register(currentBridgeStatus())
    }

    fun get(): QclDatabase = database ?: throw IllegalStateException("Database not initialized")

    fun getType(): DatabaseType = type

    fun isMySQL(): Boolean = type == DatabaseType.MYSQL

    fun isSQLite(): Boolean = type == DatabaseType.SQLITE

    fun bridgeStatus(): BridgeStatus = currentBridgeStatus()

    fun diagnose(): DiagnosticResult<BridgeStatus> = DiagnosticResult.ok(currentBridgeStatus(), source = "database")

    private fun currentBridgeStatus(): BridgeStatus = when (type) {
        DatabaseType.SQLITE -> BridgeStatus(
            name = "SQLite",
            available = database != null,
            enabled = database != null,
            source = "Database",
            message = if (database != null) Lang.get("database-manager.sqlite-initialized") else Lang.get("database-manager.sqlite-not-initialized"),
            recoverable = true,
        )
        DatabaseType.MYSQL -> BridgeStatus(
            name = "MySQL",
            available = database != null,
            enabled = database != null,
            source = "Database",
            message = if (database != null) Lang.get("database-manager.mysql-initialized") else Lang.get("database-manager.mysql-not-initialized"),
            recoverable = true,
        )
    }
}
