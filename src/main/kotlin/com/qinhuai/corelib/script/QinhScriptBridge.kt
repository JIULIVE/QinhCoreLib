package com.qinhuai.corelib.script

import com.qinhuai.corelib.QinhCoreLib
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object QinhScriptBridge {

    private val repository = ScriptRepository()
    private val pluginScriptDirs = ConcurrentHashMap<String, File>()
    private val sourceCache = ConcurrentHashMap<String, ScriptSource>()
    private var engine: GraalJavaScriptEngine? = null
    private var config: QinhScriptConfig = QinhScriptConfig()
    private var initialized = false

    fun init(plugin: Plugin = QinhCoreLib.instance) {
        engine?.close()
        engine = null
        sourceCache.clear()
        config = QinhScriptConfig.fromPlugin()
        if (!config.enabled || !GraalJavaScriptEngine.isRuntimeAvailable()) {
            initialized = true
            return
        }
        engine = GraalJavaScriptEngine(config)
        registerCoreRoots(plugin)
        pluginScriptDirs.forEach { (namespace, dir) ->
            repository.registerRoot(namespace, dir)
        }
        ensureExampleScripts(plugin)
        initialized = true
    }

    fun shutdown() {
        engine?.close()
        engine = null
        initialized = false
    }

    fun isAvailable(): Boolean = initialized && config.enabled && engine != null

    fun reload(): Int {
        val plugin = QinhCoreLib.instance
        init(plugin)
        return if (isAvailable()) repository.scan().size else 0
    }

    fun loadedScripts(): List<String> = if (isAvailable()) repository.scan() else emptyList()

    fun registerRoot(namespace: String, directory: File) {
        repository.registerRoot(namespace, directory)
        sourceCache.clear()
    }

    fun unregisterRoot(namespace: String) {
        repository.unregisterRoot(namespace)
        sourceCache.clear()
    }

    fun registerPluginScripts(plugin: Plugin, namespace: String) {
        val dir = File(plugin.dataFolder, "scripts")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val key = namespace.lowercase()
        pluginScriptDirs[key] = dir
        registerRoot(key, dir)
    }

    fun unregisterPluginScripts(namespace: String) {
        val key = namespace.lowercase()
        pluginScriptDirs.remove(key)
        unregisterRoot(key)
    }

    fun execute(reference: String, context: ScriptContext): ScriptExecutionResult {
        if (!isAvailable()) {
            return ScriptDiagnostics.unavailable().let { ScriptExecutionResult.fail(it.message, it.code, it.suggestion) }
        }
        val call = ScriptRefParser.parse(reference, config.defaultFunction)
        if (call.logicalPath.isBlank()) {
            return ScriptDiagnostics.parseFailed(reference).let { ScriptExecutionResult.fail(it.message, it.code, it.suggestion) }
        }
        val source = sourceCache[call.logicalPath]
            ?: repository.find(call.logicalPath)?.also { sourceCache[call.logicalPath] = it }
            ?: return ScriptDiagnostics.notFound(call.logicalPath).let { ScriptExecutionResult.fail(it.message, it.code, it.suggestion) }
        return engine!!.execute(source, call.functionName, context)
    }

    fun execute(
        reference: String,
        player: Player?,
        plugin: Plugin,
        variables: Map<String, Any> = emptyMap(),
        silent: Boolean = false,
    ): ScriptExecutionResult {
        return execute(
            reference,
            ScriptContext(
                plugin = plugin,
                player = player,
                variables = variables.toMutableMap(),
                silent = silent,
            ),
        )
    }

    fun evaluateBoolean(
        reference: String,
        player: Player,
        plugin: Plugin = QinhCoreLib.instance,
        variables: Map<String, Any> = emptyMap(),
    ): Boolean {
        return execute(reference, player, plugin, variables, silent = true).asBoolean(false)
    }

    private fun registerCoreRoots(plugin: Plugin) {
        repository.registerRoot("global", File(plugin.dataFolder, "scripts/global"))
        repository.registerRoot("qinhcorelib", File(plugin.dataFolder, "scripts"))
    }

    private fun ensureExampleScripts(plugin: Plugin) {
        val target = File(plugin.dataFolder, "scripts/global/example.js")
        if (target.exists()) {
            return
        }
        target.parentFile.mkdirs()
        plugin.getResource("scripts/global/example.js")?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
    }
}
