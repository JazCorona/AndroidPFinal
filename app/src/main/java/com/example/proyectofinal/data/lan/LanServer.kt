package com.example.proyectofinal.data.lan

// === Servidor TCP para recibir conexiones de otros dispositivos en la LAN ===
// Escucha en un puerto específico y responde a comandos de texto
// (GET_MATCHES, GET_USER, GET_LOBBY, LOBBY_UPDATE, CHAT_MSG, POST_MATCH).
// Cada cliente se maneja en una corrutina independiente en Dispatchers.IO.

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

class LanServer(
    // Callbacks que el ViewModel inyecta para responder a peticiones remotas
    private val port: Int,
    private val onRequestData: () -> String,        // Devuelve las partidas locales como JSON
    private val onRequestUser: () -> String,         // Devuelve el nombre de usuario local
    private val onRequestLobby: () -> String,        // Devuelve el estado actual del lobby como JSON
    private val onLobbyUpdate: (String) -> Unit,     // Recibe actualizaciones del lobby remoto
    private val onDataReceived: (String) -> Unit,    // Recibe datos de partida del remoto
    private val onChatReceived: (String, String) -> Unit = { _, _ -> }  // Recibe mensajes de chat (from, text)
) {
    private var serverSocket: ServerSocket? = null
    private var running = false
    private val scope = CoroutineScope(Dispatchers.IO)

    // Inicia el servidor TCP en el puerto asignado, aceptando conexiones entrantes en un bucle
    fun start() {
        running = true
        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                Log.d("LAN_SERVER", "Escuchando en puerto $port")
                while (running) {
                    val client = serverSocket?.accept() ?: break
                    handleClient(client)
                }
            } catch (e: Exception) {
                Log.e("LAN_SERVER", "Error: ${e.message}")
            }
        }
    }

    // Procesa una conexión entrante: lee el comando, ejecuta la acción y responde
    private fun handleClient(socket: Socket) {
        scope.launch {
            try {
                val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                val output = PrintWriter(socket.getOutputStream(), true)

                val request = input.readLine()
                Log.d("LAN_SERVER", "Recibido: $request")

                // Protocolo basado en prefijos de comando:
                when {
                    request == "GET_MATCHES" -> {
                        output.println(onRequestData())
                    }
                    request == "GET_USER" -> {
                        output.println(onRequestUser())
                    }
                    request == "GET_LOBBY" -> {
                        output.println(onRequestLobby())
                    }
                    request?.startsWith("LOBBY_UPDATE:") == true -> {
                        val lobbyJson = request.removePrefix("LOBBY_UPDATE:")
                        onLobbyUpdate(lobbyJson)
                        output.println("OK")
                    }
                    request?.startsWith("CHAT_MSG:") == true -> {
                        // El mensaje llega como JSON: {"from":"...","text":"..."}
                        val json = request.removePrefix("CHAT_MSG:")
                        try {
                            val obj = org.json.JSONObject(json)
                            val from = obj.getString("from")
                            val text = obj.getString("text")
                            onChatReceived(from, text)
                        } catch (_: Exception) {}
                        output.println("OK")
                    }
                    request?.startsWith("POST_MATCH:") == true -> {
                        onDataReceived(request.removePrefix("POST_MATCH:"))
                        output.println("OK")
                    }
                }

                socket.close()
            } catch (e: Exception) {
                Log.e("LAN_SERVER", "Error client: ${e.message}")
                try { socket.close() } catch (_: Exception) {}
            }
        }
    }

    // Detiene el servidor cerrando el socket principal
    fun stop() {
        running = false
        try { serverSocket?.close() } catch (_: Exception) {}
    }
}
