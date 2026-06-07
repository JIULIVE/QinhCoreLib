package com.qinhuai.corelib.script

import java.io.File
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

class ScriptRepository {

    private val roots = ConcurrentHashMap<String, File>()

    fun registerRoot(namespace: String, directory: File) {
        if (!directory.exists()) {
            directory.mkdirs()
        }
        roots[namespace.lowercase()] = directory
    }

    fun unregisterRoot(namespace: String) {
        roots.remove(namespace.lowercase())
    }

    fun registeredNamespaces(): Set<String> = roots.keys.toSet()

    fun find(logicalPath: String): ScriptSource? {
        val normalized = normalizePath(logicalPath) ?: return null
        val (namespace, relative) = splitNamespace(normalized)
        if (namespace != null) {
            return readFromRoot(namespace, relative)
        }
        for (ns in listOf("global") + roots.keys.filter { it != "global" }) {
            readFromRoot(ns, normalized)?.let { return it }
        }
        return null
    }

    fun scan(): List<String> {
        val result = linkedSetOf<String>()
        roots.forEach { (namespace, dir) ->
            if (!dir.isDirectory) return@forEach
            dir.toPath().walk().filter { it.isRegularFile() && it.name.endsWith(".js", ignoreCase = true) }
                .forEach { path ->
                    val relative = path.relativeTo(dir.toPath()).toString().replace('\\', '/')
                    result.add("$namespace:$relative")
                }
        }
        return result.sorted()
    }

    private fun readFromRoot(namespace: String, relativePath: String): ScriptSource? {
        val root = roots[namespace.lowercase()] ?: return null
        val file = File(root, relativePath)
        if (!file.isFile || !file.name.endsWith(".js", ignoreCase = true)) {
            return null
        }
        if (!file.canonicalPath.startsWith(root.canonicalPath)) {
            return null
        }
        val content = runCatching { Files.readString(file.toPath()) }.getOrNull() ?: return null
        val logical = "$namespace:${relativePath.replace('\\', '/')}"
        return ScriptSource(logical, content)
    }

    private fun splitNamespace(path: String): Pair<String?, String> {
        val index = path.indexOf(':')
        if (index <= 0) return null to path
        return path.substring(0, index).lowercase() to path.substring(index + 1)
    }

    private fun normalizePath(raw: String): String? {
        val trimmed = raw.trim().replace('\\', '/')
        if (trimmed.isBlank() || trimmed.contains("..")) {
            return null
        }
        return trimmed.removePrefix("/")
    }
}
