package io.github.mtbarr.spript

import io.github.mtbarr.spript.manager.CacheManager
import io.github.mtbarr.spript.manager.ScriptManager
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.logging.Level

class JavaScriptEnginePlugin : JavaPlugin() {
    private var scriptManager: ScriptManager? = null
    val cacheManager = CacheManager()

    override fun onEnable() {
        val scriptsFolder = File(dataFolder, "scripts")
        if (!scriptsFolder.exists()) {
            scriptsFolder.mkdirs()
        }

        val stdlibFile = File(scriptsFolder, "stdlib.js")
        if (!stdlibFile.exists()) {
            saveResource("scripts/stdlib.js", false)
        }

        scriptManager = ScriptManager(this, scriptsFolder)

        try {
            scriptManager?.loadAllScripts()
            logger.info("JavaScript Plugin Loader habilitado!")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Erro ao carregar scripts JavaScript", e)
        }
    }

    override fun onDisable() {
        scriptManager?.unloadAllScripts()
        logger.info("JavaScript Plugin Loader desabilitado!")
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (command.name.equals("jsreload", ignoreCase = true)) {
            try {
                if (args.isNotEmpty()) {
                    val fileName = if (args[0].endsWith(".js")) args[0] else "${args[0]}.js"
                    scriptManager?.reloadScript(fileName)
                    sender.sendMessage("§aScript '$fileName' e suas dependências recarregados!")
                } else {
                    scriptManager?.reloadAllScripts()
                    sender.sendMessage("§aTodos os scripts JavaScript recarregados!")
                }
                return true
            } catch (e: Exception) {
                sender.sendMessage("§cErro ao recarregar scripts: \${e.message}")
                logger.log(Level.SEVERE, "Erro ao recarregar scripts", e)
                return true
            }
        }
        return false
    }
}
