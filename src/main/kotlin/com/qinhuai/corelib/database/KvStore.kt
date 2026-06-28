package com.qinhuai.corelib.database

import java.util.UUID

object KvStore {

    private const val GLOBAL = "__global__"
    private var initialized = false

    fun ensureTable() {
        if (initialized) return
        DatabaseManager.update(
            "CREATE TABLE IF NOT EXISTS qcl_kv (" +
                "namespace VARCHAR(64) NOT NULL, " +
                "owner VARCHAR(64) NOT NULL, " +
                "k VARCHAR(128) NOT NULL, " +
                "v TEXT, " +
                "PRIMARY KEY (namespace, owner, k))",
        )
        initialized = true
    }

    private fun ownerKey(owner: UUID?): String = owner?.toString() ?: GLOBAL

    fun put(namespace: String, owner: UUID?, key: String, value: String) {
        ensureTable()
        if (DatabaseManager.isMySQL()) {
            DatabaseManager.update(
                "INSERT INTO qcl_kv (namespace, owner, k, v) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE v = VALUES(v)",
                namespace, ownerKey(owner), key, value,
            )
        } else {
            DatabaseManager.update(
                "INSERT INTO qcl_kv (namespace, owner, k, v) VALUES (?, ?, ?, ?) " +
                    "ON CONFLICT(namespace, owner, k) DO UPDATE SET v = excluded.v",
                namespace, ownerKey(owner), key, value,
            )
        }
    }

    fun get(namespace: String, owner: UUID?, key: String): String? {
        ensureTable()
        return DatabaseManager.query(
            "SELECT v FROM qcl_kv WHERE namespace = ? AND owner = ? AND k = ?",
            listOf(namespace, ownerKey(owner), key),
        ) { it.getString("v") }.firstOrNull()
    }

    fun remove(namespace: String, owner: UUID?, key: String) {
        ensureTable()
        DatabaseManager.update(
            "DELETE FROM qcl_kv WHERE namespace = ? AND owner = ? AND k = ?",
            namespace, ownerKey(owner), key,
        )
    }

    fun all(namespace: String, owner: UUID?): Map<String, String> {
        ensureTable()
        val out = LinkedHashMap<String, String>()
        DatabaseManager.query(
            "SELECT k, v FROM qcl_kv WHERE namespace = ? AND owner = ?",
            listOf(namespace, ownerKey(owner)),
        ) { it.getString("k") to (it.getString("v") ?: "") }.forEach { out[it.first] = it.second }
        return out
    }
}
