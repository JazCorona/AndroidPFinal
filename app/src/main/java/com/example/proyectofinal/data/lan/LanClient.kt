package com.example.proyectofinal.data.lan

// === Cliente TCP para comunicarse con otros dispositivos en la LAN ===
// Envía comandos de texto a través de sockets TCP a un servidor remoto.
// Protocolo: comandos de una línea (GET_MATCHES, GET_USER, GET_LOBBY, etc.)
// y respuestas de una línea.

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class LanClient {
    // Corrutina lanzada en el hilo de I/O para no bloquear la UI
    private val scope = CoroutineScope(Dispatchers.IO)

    // Solicita la lista de partidas del dispositivo remoto
    fun requestMatches(host: String, port: Int, callback: (String?) -> Unit) {
        scope.launch {
            try {
                val socket = Socket(host, port)
                val output = PrintWriter(socket.getOutputStream(), true)
                val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                output.println("GET_MATCHES")
                callback(input.readLine())
                socket.close()
            } catch (e: Exception) { Log.e("LAN_CLIENT", "Error: ${e.message}"); callback(null) }
        }
    }

    // Obtiene el nombre de usuario del dispositivo remoto
    fun fetchUser(host: String, port: Int, callback: (String?) -> Unit) {
        scope.launch {
            try {
                val socket = Socket(host, port)
                val output = PrintWriter(socket.getOutputStream(), true)
                val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                output.println("GET_USER")
                callback(input.readLine())
                socket.close()
            } catch (e: Exception) { Log.e("LAN_CLIENT", "Error: ${e.message}"); callback(null) }
        }
    }

    // Obtiene el estado actual del lobby del dispositivo remoto
    fun fetchLobby(host: String, port: Int, callback: (String?) -> Unit) {
        scope.launch {
            try {
                val socket = Socket(host, port)
                val output = PrintWriter(socket.getOutputStream(), true)
                val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                output.println("GET_LOBBY")
                callback(input.readLine())
                socket.close()
            } catch (e: Exception) { Log.e("LAN_CLIENT", "Error: ${e.message}"); callback(null) }
        }
    }

    // Envía una actualización del lobby al dispositivo remoto (cambios de equipo, inicio, etc.)
    fun sendLobbyUpdate(host: String, port: Int, lobbyJson: String, callback: (Boolean) -> Unit) {
        scope.launch {
            try {
                val socket = Socket(host, port)
                val output = PrintWriter(socket.getOutputStream(), true)
                val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                output.println("LOBBY_UPDATE:$lobbyJson")
                callback(input.readLine() == "OK")
                socket.close()
            } catch (e: Exception) { Log.e("LAN_CLIENT", "Error: ${e.message}"); callback(false) }
        }
    }

    // Envía un mensaje de chat en formato JSON al dispositivo remoto
    fun sendChatMsg(host: String, port: Int, from: String, text: String, callback: (Boolean) -> Unit) {
        scope.launch {
            try {
                val socket = Socket(host, port)
                val output = PrintWriter(socket.getOutputStream(), true)
                val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                // Escapa comillas dobles en el contenido para evitar JSON inválido
                val json = """{"from":"${from.replace("\"", "\\\"")}","text":"${text.replace("\"", "\\\"")}"}"""
                output.println("CHAT_MSG:$json")
                callback(input.readLine() == "OK")
                socket.close()
            } catch (e: Exception) { Log.e("LAN_CLIENT", "Error: ${e.message}"); callback(false) }
        }
    }

    // Envía una partida completa como JSON al dispositivo remoto
    fun sendMatch(host: String, port: Int, matchJson: String, callback: (Boolean) -> Unit) {
        scope.launch {
            try {
                val socket = Socket(host, port)
                val output = PrintWriter(socket.getOutputStream(), true)
                val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                output.println("POST_MATCH:$matchJson")
                // El servidor responde "OK" si todo salió bien
                callback(input.readLine() == "OK")
                socket.close()
            } catch (e: Exception) { Log.e("LAN_CLIENT", "Error: ${e.message}"); callback(false) }
        }
    }
}
