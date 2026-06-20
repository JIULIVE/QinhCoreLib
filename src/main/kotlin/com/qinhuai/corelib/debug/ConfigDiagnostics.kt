package com.qinhuai.corelib.debug

import com.qinhuai.corelib.config.ConfigManager
import com.qinhuai.corelib.lang.Lang
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object ConfigDiagnostics {
    fun status(): List<BridgeStatus> {
        return ConfigManagerSnapshot.paths().map { path ->
            val cfg = ConfigManager.getConfig(path)
            BridgeStatus(
                name = "Config:$path",
                available = cfg != null,
                enabled = cfg != null,
                source = "Config",
                message = if (cfg != null) Lang.get("common.loaded") else Lang.get("config-diagnostics.not-loaded"),
                recoverable = true,
            )
        }
    }

    fun diagnose(): DiagnosticResult<List<BridgeStatus>> = DiagnosticResult.ok(status(), source = "config")
}

object ConfigManagerSnapshot {
    private val knownPaths = linkedSetOf<String>()

    fun remember(path: String) {
        knownPaths += path
    }

    fun paths(): List<String> = knownPaths.toList()
}
