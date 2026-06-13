package io.github.mtbarr.spript.manager

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

class HttpManager {
    private val client: HttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .connectTimeout(Duration.ofSeconds(10))
        .executor(Executors.newVirtualThreadPerTaskExecutor())
        .build()

    fun get(url: String, headers: Map<String, String>?): CompletableFuture<HttpResponse<String>> {
        val builder = HttpRequest.newBuilder().uri(URI.create(url)).GET()
        headers?.forEach { (k, v) -> builder.header(k, v) }
        
        return client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
    }

    fun post(url: String, body: String, headers: Map<String, String>?): CompletableFuture<HttpResponse<String>> {
        val builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            
        headers?.forEach { (k, v) -> builder.header(k, v) }
        if (headers == null || !headers.keys.any { it.equals("Content-Type", ignoreCase = true) }) {
            builder.header("Content-Type", "application/json")
        }

        return client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
    }
}
