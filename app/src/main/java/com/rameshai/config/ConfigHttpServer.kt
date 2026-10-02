package com.rameshai.config

import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

/**
 * Tiny loopback-only HTTP server that lets `tools/config.sh`, running in Termux
 * ON THE SAME PHONE, read and update runtime configuration without a PC and
 * without ADB — it just curls http://127.0.0.1:8765.
 *
 * Security properties:
 *  - Binds explicitly to 127.0.0.1, so it is NEVER reachable from the network,
 *    Wi-Fi, or any other device — only processes on this phone (like Termux)
 *    can reach it.
 *  - GET /config returns the config with the API key MASKED (never the raw key).
 *  - POST /config only accepts a known field whitelist and never logs the body.
 *  - Runs only while the RAMESH AI process is alive (app open or foreground
 *    service running) — it is not a persistent always-on daemon.
 *
 * This is intentionally minimal (no external HTTP library) to keep the app
 * lightweight, per the "avoid unnecessary libraries" requirement.
 */
class ConfigHttpServer(private val configRepository: ConfigRepository) {

    private var serverSocket: ServerSocket? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val connectionPool = Executors.newCachedThreadPool()

    fun start(port: Int = DEFAULT_PORT) {
        if (serverSocket != null) return
        executor.submit {
            try {
                serverSocket = ServerSocket(port, 50, InetAddress.getByName("127.0.0.1"))
                while (true) {
                    val client = serverSocket?.accept() ?: break
                    connectionPool.submit { handle(client) }
                }
            } catch (e: Exception) {
                Log.w(TAG, "config server stopped: ${e.message}")
            }
        }
    }

    fun stop() {
        serverSocket?.close()
        serverSocket = null
    }

    private fun handle(client: Socket) {
        client.use { socket ->
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0]
            val path = parts[1]

            var contentLength = 0
            var line: String?
            while (true) {
                line = reader.readLine()
                if (line.isNullOrBlank()) break
                if (line.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            val body = if (contentLength > 0) {
                val buffer = CharArray(contentLength)
                reader.read(buffer, 0, contentLength)
                String(buffer)
            } else ""

            val response = when {
                path == "/health" -> jsonResponse(200, JSONObject().put("status", "ok"))
                path == "/config" && method == "GET" -> jsonResponse(200, maskedConfigJson())
                path == "/config" && method == "POST" -> handleUpdate(body)
                else -> jsonResponse(404, JSONObject().put("error", "not found"))
            }
            socket.getOutputStream().write(response.toByteArray())
            socket.getOutputStream().flush()
        }
    }

    private fun handleUpdate(body: String): String {
        return try {
            val update = JSONObject(body)
            configRepository.update { current ->
                current.copy(
                    assistantName = update.optString("assistantName", current.assistantName),
                    wakePhrase = update.optString("wakePhrase", current.wakePhrase),
                    language = update.optString("language", current.language),
                    aiProvider = update.optString("aiProvider", current.aiProvider),
                    aiBaseUrl = update.optString("aiBaseUrl", current.aiBaseUrl),
                    aiApiKey = update.optString("aiApiKey", current.aiApiKey),
                    aiModel = update.optString("aiModel", current.aiModel),
                    systemPrompt = update.optString("systemPrompt", current.systemPrompt),
                    newsProvider = update.optString("newsProvider", current.newsProvider),
                    newsApiKey = update.optString("newsApiKey", current.newsApiKey),
                    debugMode = update.optBoolean("debugMode", current.debugMode)
                )
            }
            jsonResponse(200, maskedConfigJson())
        } catch (e: Exception) {
            jsonResponse(400, JSONObject().put("error", "invalid json"))
        }
    }

    private fun maskedConfigJson(): JSONObject {
        val c = configRepository.current()
        return c.toJson().apply { put("aiApiKey", c.maskedApiKey()) }
    }

    private fun jsonResponse(code: Int, body: JSONObject): String {
        val text = body.toString()
        val statusText = if (code == 200) "OK" else if (code == 404) "Not Found" else "Bad Request"
        return "HTTP/1.1 $code $statusText\r\n" +
            "Content-Type: application/json\r\n" +
            "Content-Length: ${text.toByteArray().size}\r\n" +
            "Connection: close\r\n\r\n" +
            text
    }

    companion object {
        const val DEFAULT_PORT = 8765
        private const val TAG = "ConfigHttpServer"
    }
}
