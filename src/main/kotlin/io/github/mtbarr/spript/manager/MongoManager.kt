package io.github.mtbarr.spript.manager

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.bson.types.ObjectId
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class MongoManager {
    private val executor = Executors.newVirtualThreadPerTaskExecutor()
    private val clients = ConcurrentHashMap<String, MongoClient>()

    fun connect(uri: String): MongoClient {
        return clients.computeIfAbsent(uri) { MongoClients.create(uri) }
    }

    fun disconnect(client: MongoClient) {
        val entry = clients.entries.firstOrNull { it.value == client } ?: return
        clients.remove(entry.key)?.close()
    }

    fun closeAll() {
        clients.values.forEach { it.close() }
        clients.clear()
    }

    fun database(client: MongoClient, databaseName: String): MongoDatabase {
        return client.getDatabase(databaseName)
    }

    fun collection(client: MongoClient, databaseName: String, collectionName: String): MongoCollection<Document> {
        return database(client, databaseName).getCollection(collectionName)
    }

    fun find(collection: MongoCollection<Document>, filter: Any?): CompletableFuture<List<Map<String, Any?>>> {
        return CompletableFuture.supplyAsync({
            collection.find(toDocument(filter)).map { toJsMap(it) }.toList()
        }, executor)
    }

    fun findOne(collection: MongoCollection<Document>, filter: Any?): CompletableFuture<Map<String, Any?>?> {
        return CompletableFuture.supplyAsync({
            collection.find(toDocument(filter)).first()?.let { toJsMap(it) }
        }, executor)
    }

    fun insertOne(collection: MongoCollection<Document>, document: Any): CompletableFuture<String?> {
        return CompletableFuture.supplyAsync({
            collection.insertOne(toDocument(document)).insertedId?.let { id ->
                if (id.isObjectId) id.asObjectId().value.toHexString() else id.toString()
            }
        }, executor)
    }

    fun updateOne(collection: MongoCollection<Document>, filter: Any, update: Any): CompletableFuture<Long> {
        return CompletableFuture.supplyAsync({
            collection.updateOne(toDocument(filter), toDocument(update)).modifiedCount
        }, executor)
    }

    fun deleteOne(collection: MongoCollection<Document>, filter: Any): CompletableFuture<Long> {
        return CompletableFuture.supplyAsync({
            collection.deleteOne(toDocument(filter)).deletedCount
        }, executor)
    }

    private fun toDocument(value: Any?): Document {
        return when (val converted = toBsonValue(value)) {
            null -> Document()
            is Document -> converted
            is Map<*, *> -> Document(converted.mapKeys { it.key.toString() })
            else -> throw IllegalArgumentException("Mongo document must be a JavaScript object")
        }
    }

    private fun toBsonValue(value: Any?): Any? {
        return when {
            value == null -> null
            value is ScriptObjectMirror && value.isArray -> (0 until value.size).map { toBsonValue(value.getSlot(it)) }
            value is ScriptObjectMirror -> {
                val doc = Document()
                for (entry in value.entries) {
                    doc[entry.key] = toBsonValue(entry.value)
                }
                doc
            }
            value is Map<*, *> -> Document(value.entries.associate { it.key.toString() to toBsonValue(it.value) })
            value is Iterable<*> -> value.map { toBsonValue(it) }
            value is Array<*> -> value.map { toBsonValue(it) }
            else -> value
        }
    }

    private fun toJsMap(document: Document): Map<String, Any?> {
        return document.entries.associate { it.key to toJsValue(it.value) }
    }

    private fun toJsValue(value: Any?): Any? {
        return when (value) {
            is Document -> toJsMap(value)
            is ObjectId -> value.toHexString()
            is Iterable<*> -> value.map { toJsValue(it) }
            is Array<*> -> value.map { toJsValue(it) }
            else -> value
        }
    }
}
