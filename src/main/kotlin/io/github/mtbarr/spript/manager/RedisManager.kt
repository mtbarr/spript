package io.github.mtbarr.spript.manager

import org.openjdk.nashorn.api.scripting.ScriptObjectMirror
import redis.clients.jedis.JedisPooled
import redis.clients.jedis.Response
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class RedisManager {
    private val executor = Executors.newVirtualThreadPerTaskExecutor()
    private val clients = ConcurrentHashMap<String, JedisPooled>()

    fun connect(uri: String): JedisPooled {
        return clients.computeIfAbsent(uri) { JedisPooled(URI.create(uri)) }
    }

    fun disconnect(client: JedisPooled) {
        val entry = clients.entries.firstOrNull { it.value == client } ?: return
        clients.remove(entry.key)?.close()
    }

    fun closeAll() {
        clients.values.forEach { it.close() }
        clients.clear()
    }

    fun get(client: JedisPooled, key: String): CompletableFuture<String?> = async { client.get(key) }

    fun set(client: JedisPooled, key: String, value: String): CompletableFuture<String> = async { client.set(key, value) }

    fun setEx(client: JedisPooled, key: String, seconds: Long, value: String): CompletableFuture<String> = async {
        client.setex(key, seconds, value)
    }

    fun delete(client: JedisPooled, keys: Any): CompletableFuture<Long> = async {
        client.del(*toStringArray(keys))
    }

    fun exists(client: JedisPooled, key: String): CompletableFuture<Boolean> = async { client.exists(key) }

    fun expire(client: JedisPooled, key: String, seconds: Long): CompletableFuture<Long> = async {
        client.expire(key, seconds)
    }

    fun ttl(client: JedisPooled, key: String): CompletableFuture<Long> = async { client.ttl(key) }

    fun incr(client: JedisPooled, key: String): CompletableFuture<Long> = async { client.incr(key) }

    fun decr(client: JedisPooled, key: String): CompletableFuture<Long> = async { client.decr(key) }

    fun hGet(client: JedisPooled, key: String, field: String): CompletableFuture<String?> = async { client.hget(key, field) }

    fun hSet(client: JedisPooled, key: String, field: String, value: String): CompletableFuture<Long> = async {
        client.hset(key, field, value)
    }

    fun hGetAll(client: JedisPooled, key: String): CompletableFuture<Map<String, String>> = async { client.hgetAll(key) }

    fun lPush(client: JedisPooled, key: String, values: Any): CompletableFuture<Long> = async {
        client.lpush(key, *toStringArray(values))
    }

    fun rPush(client: JedisPooled, key: String, values: Any): CompletableFuture<Long> = async {
        client.rpush(key, *toStringArray(values))
    }

    fun lRange(client: JedisPooled, key: String, start: Long, stop: Long): CompletableFuture<List<String>> = async {
        client.lrange(key, start, stop)
    }

    fun publish(client: JedisPooled, channel: String, message: String): CompletableFuture<Long> = async {
        client.publish(channel, message)
    }

    fun pipeline(client: JedisPooled, operations: Any): CompletableFuture<List<Any?>> = async {
        client.pipelined().use { pipeline ->
            val responses = toList(operations).map { operation ->
                val op = operation as? ScriptObjectMirror
                    ?: throw IllegalArgumentException("Redis pipeline operations must be JavaScript objects")
                val command = op["command"]?.toString()?.lowercase()
                    ?: throw IllegalArgumentException("Redis pipeline operation missing command")
                val args = toList(op["args"] ?: emptyList<Any>())

                when (command) {
                    "get" -> pipeline.get(arg(args, 0))
                    "set" -> pipeline.set(arg(args, 0), arg(args, 1))
                    "setex" -> pipeline.setex(arg(args, 0), longArg(args, 1), arg(args, 2))
                    "delete", "del" -> pipeline.del(*stringArgs(args, 0))
                    "exists" -> pipeline.exists(arg(args, 0))
                    "expire" -> pipeline.expire(arg(args, 0), longArg(args, 1))
                    "ttl" -> pipeline.ttl(arg(args, 0))
                    "incr" -> pipeline.incr(arg(args, 0))
                    "decr" -> pipeline.decr(arg(args, 0))
                    "hget" -> pipeline.hget(arg(args, 0), arg(args, 1))
                    "hset" -> pipeline.hset(arg(args, 0), arg(args, 1), arg(args, 2))
                    "hgetall" -> pipeline.hgetAll(arg(args, 0))
                    "lpush" -> pipeline.lpush(arg(args, 0), *stringArgs(args, 1))
                    "rpush" -> pipeline.rpush(arg(args, 0), *stringArgs(args, 1))
                    "lrange" -> pipeline.lrange(arg(args, 0), longArg(args, 1), longArg(args, 2))
                    "publish" -> pipeline.publish(arg(args, 0), arg(args, 1))
                    else -> throw IllegalArgumentException("Unsupported Redis pipeline command: $command")
                }
            }
            pipeline.sync()
            responses.map { responseValue(it) }
        }
    }

    private fun <T> async(task: () -> T): CompletableFuture<T> {
        return CompletableFuture.supplyAsync(task, executor)
    }

    private fun toStringArray(value: Any): Array<String> {
        return when {
            value is ScriptObjectMirror && value.isArray -> (0 until value.size).map { value.getSlot(it).toString() }.toTypedArray()
            value is Iterable<*> -> value.map { it.toString() }.toTypedArray()
            value is Array<*> -> value.map { it.toString() }.toTypedArray()
            else -> arrayOf(value.toString())
        }
    }

    private fun toList(value: Any): List<Any?> {
        return when {
            value is ScriptObjectMirror && value.isArray -> (0 until value.size).map { value.getSlot(it) }
            value is Iterable<*> -> value.toList()
            value is Array<*> -> value.toList()
            else -> listOf(value)
        }
    }

    private fun arg(args: List<Any?>, index: Int): String {
        return args.getOrNull(index)?.toString()
            ?: throw IllegalArgumentException("Redis pipeline command missing argument at index $index")
    }

    private fun longArg(args: List<Any?>, index: Int): Long {
        return (args.getOrNull(index) as? Number)?.toLong() ?: arg(args, index).toLong()
    }

    private fun stringArgs(args: List<Any?>, start: Int): Array<String> {
        val values = args.drop(start)
        if (values.size == 1 && values[0] != null) {
            val single = values[0]!!
            if (single is ScriptObjectMirror && single.isArray || single is Iterable<*> || single is Array<*>) {
                return toStringArray(single)
            }
        }
        return values.map { it.toString() }.toTypedArray()
    }

    private fun responseValue(response: Response<*>): Any? {
        val value = response.get() ?: return null
        return when (value) {
            is Map<*, *> -> value.mapKeys { it.key.toString() }
            is Iterable<*> -> value.toList()
            else -> value
        }
    }
}
