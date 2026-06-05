package com.example.proyectofinal.data.lan

// === Descubrimiento de dispositivos en la LAN usando NSD (Network Service Discovery) ===
// ArenaGG se anuncia como servicio "_arenagg._tcp." y descubre otros dispositivos
// que ejecutan la misma app en la red local. Cada dispositivo ejecuta un servidor TCP
// cuyo puerto se transmite vía NSD para la comunicación directa.

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket

// Representa un dispositivo descubierto en la LAN: nombre visible, dirección IP, puerto y usuario
data class LanDevice(
    val name: String,
    val host: String,
    val port: Int,
    val username: String = ""
)

// Helper principal que orquesta el descubrimiento NSD, el servidor y el cliente TCP
class LanDiscoveryHelper(private val context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    // Tipo de servicio personalizado para identificar ArenaGG en la red
    private val SERVICE_TYPE = "_arenagg._tcp."
    // Bloqueo multicast necesario para recibir respuestas de descubrimiento en algunas redes WiFi
    private var multicastLock: WifiManager.MulticastLock? = null

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private var lanServer: LanServer? = null
    private val lanClient = LanClient()
    private var registeredPort = 0

    // Callbacks que conectan este helper con el ViewModel
    var onRequestData: () -> String = { "[]" }          // Devuelve partidas locales como JSON
    var onRequestUser: () -> String = { "Unknown" }     // Devuelve nombre de usuario local
    var onRequestLobby: () -> String = { "{}" }          // Devuelve lobby local como JSON
    var onLobbyUpdate: (String) -> Unit = {}             // Recibe actualización de lobby remoto
    var onDataReceived: (String) -> Unit = {}             // Recibe datos de partida remotos
    var onChatReceived: (String, String) -> Unit = { _, _ -> }  // Recibe mensajes de chat (from, text)

    // StateFlows observables para la UI
    private val _devices = MutableStateFlow<List<LanDevice>>(emptyList())
    val devices: StateFlow<List<LanDevice>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Mapa temporal de dispositivos descubiertos (nombre -> LanDevice) para evitar duplicados
    private val foundDevices = mutableMapOf<String, LanDevice>()
    // IP local para filtrar nuestro propio dispositivo al resolver servicios
    private val localIp = getLocalIp()
    private var registeredServiceName: String = ""
    // Obtiene la dirección IPv4 local, ignorando loopback e interfaces caídas
    private fun getLocalIp(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return ""
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress ?: ""
                    }
                }
            }
        } catch (_: Exception) {}
        return ""
    }

    // Registra este dispositivo como servicio NSD en la red y arranca el servidor TCP
    fun registerService(userName: String? = null) {
        // 1. Asignar un puerto libre del SO para el servidor TCP
        try {
            val testSocket = ServerSocket(0)
            registeredPort = testSocket.localPort
            testSocket.close()
        } catch (e: Exception) {
            Log.e("NSD", "Error getting port", e)
            _statusMessage.value = "Error: ${e.message}"
            return
        }

        // 2. Iniciar servidor en ese puerto para atender peticiones de otros dispositivos
        lanServer?.stop()
        lanServer = LanServer(registeredPort, onRequestData, onRequestUser, onRequestLobby, onLobbyUpdate, onDataReceived, onChatReceived)
        lanServer?.start()
        Log.d("NSD", "Servidor TCP iniciado en puerto $registeredPort")

        // 3. Publicar el servicio en la red NSD para que otros dispositivos nos encuentren
        val serviceName = userName ?: onRequestUser()
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = serviceName
            serviceType = SERVICE_TYPE
            port = registeredPort
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                registeredServiceName = info.serviceName
                Log.d("NSD", "OK: servicio registrado '${info.serviceName}' puerto=$registeredPort")
                _statusMessage.value = "Registrado: ${info.serviceName}"
            }
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e("NSD", "FALLO registro: $errorCode")
                _statusMessage.value = "Error registro: $errorCode"
            }
            override fun onServiceUnregistered(info: NsdServiceInfo) {}
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e("NSD", "Excepción registro", e)
            _statusMessage.value = "Excepción: ${e.message}"
        }
    }

    // Inicia el escaneo de la red local para encontrar otros dispositivos ArenaGG
    fun startDiscovery() {
        foundDevices.clear()
        _devices.value = emptyList()
        _isScanning.value = true
        _statusMessage.value = "Escaneando..."

        // Adquiere el bloqueo multicast para poder recibir respuestas NSD
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifiManager.createMulticastLock("ArenaGGLock").apply {
            setReferenceCounted(true)
            acquire()
        }

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d("NSD", "OK: descubrimiento iniciado")
                _statusMessage.value = "Escaneando red local..."
            }
            override fun onServiceFound(service: NsdServiceInfo) {
                val foundName = service.serviceName
                Log.d("NSD", "Encontrado: '$foundName' tipo='${service.serviceType}'")

                // Ignora nuestro propio servicio para no conectarse a sí mismo
                if (foundName == registeredServiceName) {
                    Log.d("NSD", "Saltando propio (nombre): $foundName")
                    return
                }

                // Resuelve el servicio NSD para obtener IP y puerto reales
                val resolveListener = object : NsdManager.ResolveListener {
                    override fun onResolveFailed(s: NsdServiceInfo, errorCode: Int) {
                        Log.e("NSD", "FALLO resolve '${s.serviceName}': $errorCode")
                    }
                    override fun onServiceResolved(s: NsdServiceInfo) {
                        val addr = s.host?.hostAddress
                        Log.d("NSD", "RESUELTO: '${s.serviceName}' @ $addr:${s.port}")
                        // Filtra la propia IP para evitar mostrarse a sí mismo
                        if (addr != null && addr != localIp) {
                            val device = LanDevice(s.serviceName, addr, s.port)
                            foundDevices[s.serviceName] = device
                            _devices.value = foundDevices.values.toList()
                        }
                    }
                }

                try {
                    // API de resolución cambió en Android 14 (UPSIDE_DOWN_CAKE)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        nsdManager.resolveService(service, context.mainExecutor, resolveListener)
                    } else {
                        @Suppress("DEPRECATION")
                        nsdManager.resolveService(service, resolveListener)
                    }
                } catch (e: Exception) {
                    Log.e("NSD", "Excepción resolve", e)
                }
            }
            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d("NSD", "Perdido: '${service.serviceName}'")
                foundDevices.remove(service.serviceName)
                _devices.value = foundDevices.values.toList()
            }
            override fun onDiscoveryStopped(serviceType: String) {
                _isScanning.value = false
            }
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                _isScanning.value = false
                _statusMessage.value = "Error escaneo: código $errorCode"
                Log.e("NSD", "FALLO inicio: $errorCode")
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("NSD", "FALLO stop: $errorCode")
            }
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            _isScanning.value = false
            _statusMessage.value = "Excepción: ${e.message}"
            Log.e("NSD", "Excepción descubrimiento", e)
        }
    }

    // Detiene el escaneo de la red
    fun stopDiscovery() {
        try { discoveryListener?.let { nsdManager.stopServiceDiscovery(it) } } catch (_: Exception) {}
        _isScanning.value = false
    }

    // Da de baja el servicio NSD
    fun unregisterService() {
        try { registrationListener?.let { nsdManager.unregisterService(it) } } catch (_: Exception) {}
    }

    // Libera todos los recursos: descubre, desregistra, detiene servidor y suelta multicast
    fun cleanup() {
        stopDiscovery()
        unregisterService()
        lanServer?.stop()
        try { multicastLock?.release() } catch (_: Exception) {}
    }

    // Métodos auxiliares que delegan en el LanClient para comunicarse con un dispositivo específico
    fun fetchFromDevice(device: LanDevice, callback: (String?) -> Unit) {
        lanClient.requestMatches(device.host, device.port, callback)
    }

    fun fetchUsername(device: LanDevice, callback: (String?) -> Unit) {
        lanClient.fetchUser(device.host, device.port, callback)
    }

    fun sendMatchToDevice(device: LanDevice, matchJson: String, callback: (Boolean) -> Unit) {
        lanClient.sendMatch(device.host, device.port, matchJson, callback)
    }

    fun fetchLobby(device: LanDevice, callback: (String?) -> Unit) {
        lanClient.fetchLobby(device.host, device.port, callback)
    }

    fun sendLobbyUpdate(device: LanDevice, lobbyJson: String, callback: (Boolean) -> Unit) {
        lanClient.sendLobbyUpdate(device.host, device.port, lobbyJson, callback)
    }

    fun sendChat(device: LanDevice, from: String, text: String, callback: (Boolean) -> Unit) {
        lanClient.sendChatMsg(device.host, device.port, from, text, callback)
    }
}
