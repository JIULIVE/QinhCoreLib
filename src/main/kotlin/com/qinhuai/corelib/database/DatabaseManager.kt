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
import java.sql.ResultSet
import java.util.Base64

enum class DatabaseType { SQLITE, MYSQL }

class SqlBatch(val sql: String, val rows: List<List<Any?>>)

object DatabaseManager {

    private val lock = Any()
    private var type: DatabaseType = DatabaseType.SQLITE
    private var sqliteConnection: Connection? = null
    private var mysqlUrl: String = ""
    private var mysqlUser: String = ""
    private var mysqlPassword: String = ""
    private var ready: Boolean = false

    fun init() {
        close()
        val cfg = QinhCoreLib.instance.config
        val typeRaw = cfg.getString("database.type", "sqlite")?.trim()?.lowercase() ?: "sqlite"
        type = if (typeRaw == "mysql") DatabaseType.MYSQL else DatabaseType.SQLITE
        runCatching {
            when (type) {
                DatabaseType.SQLITE -> openSqlite(cfg.getString("database.sqlite.data-folder", "data") ?: "data")
                DatabaseType.MYSQL -> openMysql(cfg)
            }
            ready = true
            Lang.log(
                if (type == DatabaseType.MYSQL) Lang.get("database-manager.mysql-initialized")
                else Lang.get("database-manager.sqlite-initialized"),
            )
        }.onFailure {
            ready = false
            QinhCoreLib.instance.logger.severe(Lang.get("database-manager.init-failed", "error" to (it.message ?: it.toString())))
        }
        BridgeStatusRegistry.register(currentBridgeStatus())
    }

    private fun openSqlite(folderName: String) {
        loadDriver("org.sqlite.JDBC")
        val folder = File(QinhCoreLib.instance.dataFolder, folderName)
        if (!folder.exists()) folder.mkdirs()
        val file = File(folder, "qcl.db")
        val conn = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
        conn.autoCommit = true
        conn.createStatement().use { st ->
            st.execute("PRAGMA busy_timeout = 5000")
            st.execute("PRAGMA journal_mode = WAL")
            st.execute("PRAGMA foreign_keys = ON")
        }
        sqliteConnection = conn
    }

    private fun openMysql(cfg: org.bukkit.configuration.file.FileConfiguration) {
        loadDriver("com.mysql.cj.jdbc.Driver")
        val host = cfg.getString("database.mysql.host", "localhost") ?: "localhost"
        val port = cfg.getInt("database.mysql.port", 3306)
        val database = cfg.getString("database.mysql.database", "qinhcorelib") ?: "qinhcorelib"
        mysqlUser = cfg.getString("database.mysql.username", "root") ?: "root"
        mysqlPassword = cfg.getString("database.mysql.password", "") ?: ""
        mysqlUrl = "jdbc:mysql://$host:$port/$database?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8"
        DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPassword).close()
    }

    private fun loadDriver(className: String) {
        runCatching { Class.forName(className, true, javaClass.classLoader) }
    }

    fun isReady(): Boolean = ready

    fun getType(): DatabaseType = type

    fun isMySQL(): Boolean = type == DatabaseType.MYSQL

    fun isSQLite(): Boolean = type == DatabaseType.SQLITE

    fun <T> withConnection(block: (Connection) -> T): T = when (type) {
        DatabaseType.SQLITE -> synchronized(lock) {
            block(sqliteConnection ?: throw IllegalStateException(Lang.get("database-manager.sqlite-not-initialized")))
        }
        DatabaseType.MYSQL -> DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPassword).use(block)
    }

    fun update(sql: String, vararg params: Any?): Int = withConnection { conn ->
        conn.prepareStatement(sql).use { ps ->
            bind(ps, params)
            ps.executeUpdate()
        }
    }

    fun <T> query(sql: String, params: List<Any?> = emptyList(), mapper: (ResultSet) -> T): List<T> = withConnection { conn ->
        conn.prepareStatement(sql).use { ps ->
            bind(ps, params.toTypedArray())
            ps.executeQuery().use { rs ->
                val out = ArrayList<T>()
                while (rs.next()) out.add(mapper(rs))
                out
            }
        }
    }

    private fun bind(ps: java.sql.PreparedStatement, params: Array<out Any?>) {
        params.forEachIndexed { i, value -> ps.setObject(i + 1, value) }
    }

    fun queryRows(sql: String, params: List<Any?> = emptyList()): List<Map<String, Any?>> = withConnection { conn ->
        conn.prepareStatement(sql).use { ps ->
            bind(ps, params.toTypedArray())
            ps.executeQuery().use { rs ->
                val meta = rs.metaData
                val cols = meta.columnCount
                val out = ArrayList<Map<String, Any?>>()
                while (rs.next()) {
                    val row = HashMap<String, Any?>(cols * 2)
                    for (i in 1..cols) row[meta.getColumnLabel(i).lowercase()] = rs.getObject(i)
                    out.add(row)
                }
                out
            }
        }
    }

    fun transaction(statements: List<SqlBatch>) {
        if (statements.isEmpty()) return
        withConnection { conn ->
            val previous = conn.autoCommit
            conn.autoCommit = false
            try {
                for (stmt in statements) {
                    if (stmt.rows.isEmpty()) continue
                    conn.prepareStatement(stmt.sql).use { ps ->
                        for (row in stmt.rows) {
                            bind(ps, row.toTypedArray())
                            ps.addBatch()
                        }
                        ps.executeBatch()
                    }
                }
                conn.commit()
            } catch (e: Exception) {
                runCatching { conn.rollback() }
                throw e
            } finally {
                conn.autoCommit = previous
            }
        }
    }

    fun close() {
        synchronized(lock) {
            runCatching { sqliteConnection?.close() }
            sqliteConnection = null
        }
        ready = false
    }

    fun serializeLocation(loc: Location): String =
        "${loc.world?.name ?: "world"},${loc.x},${loc.y},${loc.z},${loc.yaw},${loc.pitch}"

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
            parts.getOrNull(5)?.toFloat() ?: 0f,
        )
    }

    fun serializeItem(item: ItemStack?): String {
        if (item == null || item.isEmpty) return ""
        return Base64.getEncoder().encodeToString(item.serializeAsBytes())
    }

    fun deserializeItem(s: String?): ItemStack? {
        if (s.isNullOrEmpty()) return null
        return runCatching { ItemStack.deserializeBytes(Base64.getDecoder().decode(s)) }.getOrNull()
    }

    fun serializeInventory(inv: List<ItemStack?>): String = runCatching {
        Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(inv.map { it ?: ItemStack.empty() }))
    }.getOrDefault("")

    fun deserializeInventory(s: String?, size: Int): MutableList<ItemStack?> {
        val target = MutableList<ItemStack?>(size) { null }
        if (s.isNullOrEmpty()) return target
        return runCatching {
            ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(s)).forEachIndexed { i, stack ->
                if (i < size) target[i] = stack.takeUnless { it.isEmpty }
            }
            target
        }.getOrDefault(target)
    }

    fun bridgeStatus(): BridgeStatus = currentBridgeStatus()

    fun diagnose(): DiagnosticResult<BridgeStatus> = DiagnosticResult.ok(currentBridgeStatus(), source = "database")

    private fun currentBridgeStatus(): BridgeStatus = when (type) {
        DatabaseType.SQLITE -> BridgeStatus(
            name = "SQLite",
            available = ready,
            enabled = ready,
            source = "Database",
            message = if (ready) Lang.get("database-manager.sqlite-initialized") else Lang.get("database-manager.sqlite-not-initialized"),
            recoverable = true,
        )
        DatabaseType.MYSQL -> BridgeStatus(
            name = "MySQL",
            available = ready,
            enabled = ready,
            source = "Database",
            message = if (ready) Lang.get("database-manager.mysql-initialized") else Lang.get("database-manager.mysql-not-initialized"),
            recoverable = true,
        )
    }
}
