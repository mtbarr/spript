package io.github.mtbarr.spript.manager

import io.github.cdimascio.dotenv.Dotenv
import java.io.File

class DotenvManager(private val scriptsFolder: File) {
    private var dotenv = loadDotenv()

    fun reload() {
        dotenv = loadDotenv()
    }

    fun get(key: String, defaultValue: String? = null): String? {
        return dotenv.get(key) ?: System.getenv(key) ?: defaultValue
    }

    fun require(key: String): String {
        return get(key) ?: throw IllegalArgumentException("Environment variable is required: $key")
    }

    fun has(key: String): Boolean {
        return get(key) != null
    }

    private fun loadDotenv(): Dotenv {
        return Dotenv.configure()
            .directory(scriptsFolder.absolutePath)
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load()
    }
}
