package io.github.mtbarr.spript.manager

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

class SQLManager {

    // Pool dedicado usando Virtual Threads (Java 21+) para escalabilidade máxima sem custo de SO
    private val executor = Executors.newVirtualThreadPerTaskExecutor()

    fun createPool(config: ScriptObjectMirror): HikariDataSource {
        val hkConfig = HikariConfig()
        
        hkConfig.jdbcUrl = config["url"] as? String ?: throw IllegalArgumentException("SQL url is required")
        
        val user = config["user"] as? String
        if (user != null) hkConfig.username = user
        
        val password = config["password"] as? String
        if (password != null) hkConfig.password = password
        
        val poolSize = config["poolSize"] as? Number
        if (poolSize != null) hkConfig.maximumPoolSize = poolSize.toInt()

        return HikariDataSource(hkConfig)
    }

    fun query(dataSource: HikariDataSource, query: String, params: List<Any>?): CompletableFuture<List<Map<String, Any>>> {
        return CompletableFuture.supplyAsync({
            val results = mutableListOf<Map<String, Any>>()
            dataSource.connection.use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    params?.forEachIndexed { index, param ->
                        stmt.setObject(index + 1, param)
                    }
                    stmt.executeQuery().use { rs ->
                        val meta = rs.metaData
                        val cols = meta.columnCount
                        while (rs.next()) {
                            val row = mutableMapOf<String, Any>()
                            for (i in 1..cols) {
                                val value = rs.getObject(i)
                                if (value != null) {
                                    row[meta.getColumnName(i)] = value
                                }
                            }
                            results.add(row)
                        }
                    }
                }
            }
            results
        }, executor)
    }

    fun execute(dataSource: HikariDataSource, query: String, params: List<Any>?): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync({
            dataSource.connection.use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    params?.forEachIndexed { index, param ->
                        stmt.setObject(index + 1, param)
                    }
                    stmt.executeUpdate()
                }
            }
        }, executor)
    }
}
