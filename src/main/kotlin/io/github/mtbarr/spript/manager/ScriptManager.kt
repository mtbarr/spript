package io.github.mtbarr.spript.manager

import io.github.mtbarr.spript.JavaScriptEnginePlugin
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory
import java.io.File
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.regex.Pattern
import javax.script.Compilable
import javax.script.ScriptEngine

class ScriptManager(
    val plugin: JavaScriptEnginePlugin,
    val scriptsFolder: File
) {
    companion object {
        private val REQUIRE_PATTERN = Pattern.compile("require\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)")
    }

    val engine: ScriptEngine
    val moduleCache = mutableMapOf<String, Any>()
    private val javaTypeCache = ConcurrentHashMap<String, Any>()
    val apiWrapper = JSAPIWrapper(plugin)
    val httpManager = HttpManager()
    val sqlManager = SQLManager()
    val mongoManager = MongoManager()
    val redisManager = RedisManager()
    val dotenvManager = DotenvManager(scriptsFolder)

    init {
        configureNashornPerformance()
        val factory = NashornScriptEngineFactory()
        engine = factory.getScriptEngine(
            "--optimistic-types",
            "--persistent-code-cache",
            "--class-cache-size=512",
            "--unstable-relink-threshold=32",
            "--language=es6"
        )
        injectGlobalAPI()
    }

    private fun configureNashornPerformance() {
        val codeCacheDir = File(plugin.dataFolder, ".nashorn-code-cache")
        val typeCacheDir = File(plugin.dataFolder, ".nashorn-type-cache")
        codeCacheDir.mkdirs()
        typeCacheDir.mkdirs()

        System.setProperty("nashorn.persistent.code.cache", codeCacheDir.absolutePath)
        System.setProperty("nashorn.typeInfo.maxFiles", "20000")
        System.setProperty("nashorn.typeInfo.cacheDir", typeCacheDir.absolutePath)
    }

    private fun injectGlobalAPI() {
        try {
            engine.put("\$scriptManager", this)
            val compilableEngine = engine as Compilable
            
            // Injeta o regenerator-runtime para suporte a async/await no Nashorn
            val regeneratorStream = plugin.getResource("regenerator.js")
            if (regeneratorStream != null) {
                val regeneratorCode = String(regeneratorStream.readBytes())
                compilableEngine.compile(regeneratorCode).eval()
            }
            
            compilableEngine.compile("function require(modulePath) { return \$scriptManager.requireModule(modulePath); } function javaType(className) { return \$scriptManager.javaType(className); }").eval()
            engine.put("api", apiWrapper)
            engine.put("\$cache", plugin.cacheManager)
            engine.put("\$http", httpManager)
            engine.put("\$sql", sqlManager)
            engine.put("\$mongo", mongoManager)
            engine.put("\$redis", redisManager)
            engine.put("\$dotenv", dotenvManager)
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Erro ao injetar API global", e)
        }
    }

    fun requireModule(path: String): Any? {
        var modulePath = path
        if (!modulePath.endsWith(".js")) {
            modulePath += ".js"
        }

        if (moduleCache.containsKey(modulePath)) {
            return moduleCache[modulePath]
        }

        val moduleFile = File(scriptsFolder, modulePath)
        require(moduleFile.exists()) { "Módulo não encontrado: \$modulePath" }

        val code = Files.readString(moduleFile.toPath())
        val compilableEngine = engine as Compilable
        
        compilableEngine.compile("var exports = {};").eval()
        compilableEngine.compile(code).eval()

        val exports = engine.get("exports") ?: Any()
        moduleCache[modulePath] = exports
        return exports
    }

    fun javaType(className: String): Any {
        return javaTypeCache.computeIfAbsent(className) {
            engine.eval("Java.type(${quoteJsString(className)})")
        }
    }

    private fun quoteJsString(value: String): String {
        return buildString {
            append('"')
            for (char in value) {
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
            append('"')
        }
    }

    fun loadAllScripts() {
        val stdlibFile = File(scriptsFolder, "stdlib.js")
        if (stdlibFile.exists() && !moduleCache.containsKey("stdlib.js")) {
            apiWrapper.currentSourceFile = "stdlib.js"
            loadScript(stdlibFile)
            apiWrapper.currentSourceFile = null
            moduleCache["stdlib.js"] = engine.get("exports") ?: Any()
        }

        val scriptFiles = scriptsFolder.listFiles { _, name -> name.endsWith(".js") && name != "stdlib.js" }
        if (scriptFiles.isNullOrEmpty()) {
            plugin.logger.info("Nenhum script JavaScript encontrado.")
            return
        }

        val sortedScripts = topologicalSort(scriptFiles)
        for (scriptFile in sortedScripts) {
            if (moduleCache.containsKey(scriptFile.name)) continue
            apiWrapper.currentSourceFile = scriptFile.name
            loadScript(scriptFile)
            apiWrapper.currentSourceFile = null
        }
        plugin.logger.info("\${sortedScripts.size} script(s) JavaScript carregado(s).")
    }

    fun reloadScript(fileName: String) {
        val targetFile = File(scriptsFolder, fileName)
        if (!targetFile.exists()) {
            throw IllegalArgumentException("Script '$fileName' não encontrado")
        }
        if (fileName == "stdlib.js") {
            plugin.logger.warning("stdlib.js não pode ser recarregado individualmente. Use /jsreload sem argumentos.")
            return
        }

        // 1. Build full dependency map from all scripts
        val allFiles = scriptsFolder.listFiles { _, name -> name.endsWith(".js") }
            ?.filter { it.name != "stdlib.js" }
            ?.associateBy { it.name } ?: emptyMap()
        val allDeps = allFiles.mapValues { (_, file) ->
            extractDependencies(Files.readString(file.toPath()))
        }

        // 2. Find transitive dependencies of the target file
        val toReload = mutableSetOf(fileName)
        findTransitiveDeps(fileName, allDeps, toReload)

        // 3. Clear module cache for these files
        for (f in toReload) {
            moduleCache.remove(f)
        }

        // 4. Unregister old listeners/commands from these files
        for (f in toReload) {
            apiWrapper.cleanup(f)
        }

        // 5. Sort in dependency order (dependencies first)
        val sorted = topologicalSortSubset(toReload, allDeps)
        plugin.logger.info("Recarregando scripts: ${sorted.joinToString(", ")}")

        // 6. Re-execute in dependency order
        for (f in sorted) {
            val file = allFiles[f] ?: continue
            apiWrapper.currentSourceFile = f
            loadScript(file)
            apiWrapper.currentSourceFile = null
        }
        plugin.logger.info("Script '$fileName' e suas dependências recarregados.")
    }

    private fun findTransitiveDeps(
        fileName: String,
        allDeps: Map<String, Set<String>>,
        result: MutableSet<String>
    ) {
        val deps = allDeps[fileName] ?: return
        for (dep in deps) {
            if (dep !in result && allDeps.containsKey(dep)) {
                result.add(dep)
                findTransitiveDeps(dep, allDeps, result)
            }
        }
    }

    private fun topologicalSortSubset(
        files: Set<String>,
        allDeps: Map<String, Set<String>>
    ): List<String> {
        val result = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        val visiting = mutableSetOf<String>()

        fun dfs(fileName: String) {
            if (fileName in visiting) throw RuntimeException("Dependência circular: $fileName")
            if (fileName in visited) return
            visiting.add(fileName)
            for (dep in (allDeps[fileName] ?: emptySet())) {
                if (dep in files) {
                    dfs(dep)
                }
            }
            visiting.remove(fileName)
            visited.add(fileName)
            result.add(fileName)
        }

        for (f in files) {
            dfs(f)
        }

        return result
    }

    private fun topologicalSort(scriptFiles: Array<File>): List<File> {
        val dependencies = mutableMapOf<String, Set<String>>()
        val fileMap = mutableMapOf<String, File>()

        for (file in scriptFiles) {
            fileMap[file.name] = file
            val content = Files.readString(file.toPath())
            dependencies[file.name] = extractDependencies(content)
        }

        val sorted = mutableListOf<File>()
        val visited = mutableSetOf<String>()
        val visiting = mutableSetOf<String>()

        for (fileName in dependencies.keys) {
            if (fileName !in visited) {
                topologicalSortDFS(fileName, dependencies, visited, visiting, sorted, fileMap)
            }
        }
        return sorted
    }

    private fun topologicalSortDFS(
        fileName: String,
        dependencies: Map<String, Set<String>>,
        visited: MutableSet<String>,
        visiting: MutableSet<String>,
        sorted: MutableList<File>,
        fileMap: Map<String, File>
    ) {
        if (fileName in visiting) throw RuntimeException("Dependência circular detectada envolvendo: \$fileName")
        if (fileName in visited) return

        visiting.add(fileName)
        dependencies[fileName]?.forEach { dep ->
            if (dependencies.containsKey(dep)) {
                topologicalSortDFS(dep, dependencies, visited, visiting, sorted, fileMap)
            }
        }

        visiting.remove(fileName)
        visited.add(fileName)
        fileMap[fileName]?.let { sorted.add(it) }
    }

    private fun extractDependencies(content: String): Set<String> {
        val deps = mutableSetOf<String>()
        val matcher = REQUIRE_PATTERN.matcher(content)
        while (matcher.find()) {
            var dep = matcher.group(1)
            if (!dep.endsWith(".js")) dep += ".js"
            deps.add(dep)
        }
        return deps
    }

    private fun loadScript(scriptFile: File) {
        val code = Files.readString(scriptFile.toPath())
        plugin.logger.info("Carregando script: \${scriptFile.name}")
        try {
            val compilableEngine = engine as Compilable
            val compiledScript = compilableEngine.compile(code)
            compiledScript.eval()
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Erro ao carregar script \${scriptFile.name}", e)
            throw e
        }
    }

    fun unloadAllScripts() {
        HandlerList.unregisterAll(plugin)
        Bukkit.getScheduler().cancelTasks(plugin)
        apiWrapper.cleanup()
        mongoManager.closeAll()
        redisManager.closeAll()
        plugin.cacheManager.clearEphemeral()
        moduleCache.clear()
        plugin.logger.info("Scripts JavaScript descarregados.")
    }

    fun reloadAllScripts() {
        unloadAllScripts()
        dotenvManager.reload()
        injectGlobalAPI()
        loadAllScripts()
    }
}
