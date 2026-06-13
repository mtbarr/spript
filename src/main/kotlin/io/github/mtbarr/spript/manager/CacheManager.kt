package io.github.mtbarr.spript.manager

import java.util.concurrent.ConcurrentHashMap

class CacheManager {
    private val persistentCache: MutableMap<String, Any> = ConcurrentHashMap()
    private val ephemeralCache: MutableMap<String, Any> = ConcurrentHashMap()

    fun set(key: String, value: Any, persistent: Boolean = false) {
        if (persistent) {
            persistentCache[key] = value
            ephemeralCache.remove(key) // Evita colisões de chaves e buscas duplicadas
        } else {
            ephemeralCache[key] = value
            persistentCache.remove(key)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        val value = ephemeralCache[key] ?: persistentCache[key]
        return value as? T
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getOrDefault(key: String, defaultValue: T): T {
        val value = ephemeralCache[key] ?: persistentCache[key]
        return (value as? T) ?: defaultValue
    }

    fun delete(key: String) {
        // Se já removeu do efêmero, não gasta CPU buscando no persistente
        if (ephemeralCache.remove(key) == null) {
            persistentCache.remove(key)
        }
    }

    fun clearEphemeral() {
        ephemeralCache.clear()
    }
}
