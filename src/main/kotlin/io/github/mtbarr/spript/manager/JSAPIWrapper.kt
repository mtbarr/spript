package io.github.mtbarr.spript.manager

import io.github.mtbarr.spript.JavaScriptEnginePlugin
import io.github.mtbarr.spript.util.MiniMessageUtil
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandMap
import org.bukkit.command.CommandSender
import org.bukkit.command.SimpleCommandMap
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.EventExecutor
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror

class JSAPIWrapper(val plugin: JavaScriptEnginePlugin) {

    var currentSourceFile: String? = null

    private val registeredListeners = mutableListOf<RegisteredListener>()
    private val registeredCommands = mutableMapOf<String, RegisteredCommand>()
    private val commandMap: CommandMap? = initializeCommandMap()

    fun sendMessage(audience: Audience, message: String) {
        audience.sendMessage(MiniMessageUtil.miniMessage(message))
    }

    fun component(message: String): Component {
        return MiniMessageUtil.miniMessage(message)
    }

    fun replaceComponent(
        component: Component,
        target: String,
        replacement: String
    ): Component {
        val config = TextReplacementConfig.builder()
            .matchLiteral(target)
            .replacement(replacement)
            .build()
        return component.replaceText(config)
    }

    fun registerListener(eventClass: Class<out Event>, callback: Any, priority: Any? = null) {
        val eventPriority = when (priority) {
            is EventPriority -> priority
            is String -> parseEventPriority(priority)
            else -> EventPriority.NORMAL
        }
        val listener = DummyListener()
        val executor = EventExecutor { _, event ->
            if (!eventClass.isInstance(event)) return@EventExecutor
            try {
                invokeCallback(callback, event)
            } catch (e: Exception) {
                plugin.logger.severe("Erro ao executar listener JavaScript: \${e.message}")
                e.printStackTrace()
            }
        }

        Bukkit.getPluginManager().registerEvent(
            eventClass,
            listener,
            eventPriority,
            executor,
            plugin
        )

        registeredListeners.add(RegisteredListener(listener, eventClass, currentSourceFile))
    }

    private fun parseEventPriority(priority: String?): EventPriority {
        if (priority == null) return EventPriority.NORMAL
        return try {
            EventPriority.valueOf(priority.uppercase())
        } catch (e: IllegalArgumentException) {
            EventPriority.NORMAL
        }
    }

    private fun invokeCallback(callback: Any, event: Event) {
        if (callback !is ScriptObjectMirror) return

        if (callback.isFunction) {
            callback.call(null, event)
        } else if (callback.hasMember("handleEvent")) {
            callback.callMember("handleEvent", event)
        }
    }

    fun registerCommand(
        name: String,
        callback: ScriptObjectMirror,
        description: String? = null,
        usage: String? = null,
        aliases: List<String>? = null,
        tabCompleter: ScriptObjectMirror? = null
    ) {
        if (commandMap == null) {
            plugin.logger.severe("CommandMap não disponível, comando não registrado: \$name")
            return
        }

        val command = JSCommand(name, callback, tabCompleter, plugin)
        description?.let { command.description = it }
        usage?.let { command.usage = it }
        aliases?.let { command.aliases = it }

        commandMap.register("jsplugin", command)
        registeredCommands[command.name] = RegisteredCommand(command, currentSourceFile)
        plugin.logger.info("Comando JavaScript registrado: /\$name")
    }

    fun getSystemProperty(path: String, defaultValue: String? = null): String? {
        return System.getProperty(path, defaultValue)
    }

    fun runTask(task: Runnable) {
        Bukkit.getScheduler().runTask(plugin, task)
    }

    fun runTaskAsync(task: Runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task)
    }

    fun runTaskLater(task: Runnable, delay: Long) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delay)
    }

    fun runTaskLaterAsync(task: Runnable, delay: Long) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay)
    }

    fun runTaskTimer(task: Runnable, delay: Long, period: Long) {
        Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period)
    }

    fun runTaskTimerAsync(task: Runnable, delay: Long, period: Long) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period)
    }

    fun cleanup() {
        unregisterAllListeners()
        unregisterAllCommands()
    }

    fun cleanup(sourceFile: String) {
        unregisterListenersFromFile(sourceFile)
        unregisterCommandsFromFile(sourceFile)
    }

    private fun unregisterAllListeners() {
        for (rl in registeredListeners) {
            HandlerList.unregisterAll(rl.listener)
        }
        registeredListeners.clear()
    }

    private fun unregisterListenersFromFile(sourceFile: String) {
        val toRemove = registeredListeners.filter { it.sourceFile == sourceFile }
        for (rl in toRemove) {
            HandlerList.unregisterAll(rl.listener)
        }
        registeredListeners.removeAll { it.sourceFile == sourceFile }
    }

    private fun unregisterAllCommands() {
        if (commandMap == null) return

        for (rc in registeredCommands.values) {
            removeCommandFromKnownCommands(rc.command.name)
            rc.command.unregister(commandMap)
        }
        registeredCommands.clear()
    }

    private fun unregisterCommandsFromFile(sourceFile: String) {
        if (commandMap == null) return

        val toRemove = registeredCommands.entries.filter { it.value.sourceFile == sourceFile }
        for ((_, rc) in toRemove) {
            removeCommandFromKnownCommands(rc.command.name)
            rc.command.unregister(commandMap)
            registeredCommands.remove(rc.command.name)
        }
    }

    private fun initializeCommandMap(): CommandMap? {
        return try {
            val field = Bukkit.getServer().javaClass.getDeclaredField("commandMap")
            field.isAccessible = true
            field.get(Bukkit.getServer()) as CommandMap
        } catch (e: Exception) {
            plugin.logger.severe("Erro ao obter CommandMap: \${e.message}")
            null
        }
    }

    private fun removeCommandFromKnownCommands(commandName: String) {
        try {
            val knownCommandsField = SimpleCommandMap::class.java.getDeclaredField("knownCommands")
            knownCommandsField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val knownCommands = knownCommandsField.get(commandMap) as MutableMap<String, Command>
            knownCommands.remove(commandName)
        } catch (e: Exception) {
            plugin.logger.warning("Erro ao remover comando dos knownCommands: \${e.message}")
        }
    }

    class JSCommand(
        name: String,
        private val callback: ScriptObjectMirror,
        private val tabCompleter: ScriptObjectMirror?,
        private val plugin: JavaScriptEnginePlugin
    ) : Command(name) {
        override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
            return try {
                callback.call(null, sender, commandLabel, args)
                true
            } catch (e: Exception) {
                sender.sendMessage("§cErro ao executar comando: \${e.message}")
                plugin.logger.severe("Erro ao executar comando JavaScript: \${e.message}")
                e.printStackTrace()
                false
            }
        }

        override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): List<String> {
            if (tabCompleter == null || !tabCompleter.isFunction) {
                return super.tabComplete(sender, alias, args)
            }
            return try {
                val result = tabCompleter.call(null, sender, alias, args)
                if (result is Collection<*>) {
                    result.map { it.toString() }
                } else if (result is Array<*>) {
                    result.map { it.toString() }
                } else if (result is ScriptObjectMirror && result.isArray) {
                    val list = mutableListOf<String>()
                    for (i in 0 until result.size) {
                        list.add(result.getSlot(i).toString())
                    }
                    list
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                plugin.logger.warning("Erro no tab-completer do comando \$name: \${e.message}")
                emptyList()
            }
        }
    }

    private class DummyListener : Listener

    private data class RegisteredListener(
        val listener: Listener,
        val eventClass: Class<out Event>,
        val sourceFile: String?
    )

    private data class RegisteredCommand(
        val command: Command,
        val sourceFile: String?
    )
}
