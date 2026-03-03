package com.nexus.data.repository

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nexus.data.model.DocumentResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class NetworkDevice(
    val name: String,
    val host: String,
    val port: Int,
    val docCount: Int = 0,
    val isSharing: Boolean = false
)

@Singleton
class NetworkRepository @Inject constructor(private val context: Context) {

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _discoveredDevices = MutableStateFlow<List<NetworkDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<NetworkDevice>> = _discoveredDevices

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering

    private var nsdManager: NsdManager? = null
    private val foundDevices = mutableMapOf<String, NetworkDevice>()

    // ── Descubrir dispositivos NEXUS en el WiFi ───────────────────────────────
    fun startDiscovery() {
        _isDiscovering.value = true
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        nsdManager?.discoverServices(
            "_nexus._tcp.",
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        )
    }

    fun stopDiscovery() {
        _isDiscovering.value = false
        try { nsdManager?.stopServiceDiscovery(discoveryListener) } catch (e: Exception) {}
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(type: String) {}
        override fun onServiceFound(info: NsdServiceInfo) {
            if (info.serviceType.contains("_nexus._tcp")) {
                nsdManager?.resolveService(info, resolveListener())
            }
        }
        override fun onServiceLost(info: NsdServiceInfo) {
            foundDevices.remove(info.serviceName)
            _discoveredDevices.value = foundDevices.values.toList()
        }
        override fun onDiscoveryStopped(type: String) { _isDiscovering.value = false }
        override fun onStartDiscoveryFailed(type: String, code: Int) { _isDiscovering.value = false }
        override fun onStopDiscoveryFailed(type: String, code: Int) {}
    }

    private fun resolveListener() = object : NsdManager.ResolveListener {
        override fun onServiceResolved(info: NsdServiceInfo) {
            val host = info.host?.hostAddress ?: return
            val port = info.port
            scope.launch {
                val ping = pingDevice(host, port) ?: return@launch
                val device = NetworkDevice(
                    name = ping["device"]?.toString() ?: info.serviceName,
                    host = host,
                    port = port,
                    docCount = ping["docs"]?.toString()?.toDoubleOrNull()?.toInt() ?: 0,
                    isSharing = ping["sharing"]?.toString()?.toBoolean() ?: false
                )
                foundDevices[info.serviceName] = device
                _discoveredDevices.value = foundDevices.values.toList()
            }
        }
        override fun onResolveFailed(info: NsdServiceInfo, code: Int) {
            Log.e("NEXUS_NET", "Resolve falló: $code")
        }
    }

    // ── Obtener docs de un dispositivo remoto (solo carpeta compartida) ────────
    suspend fun fetchRemoteDocs(device: NetworkDevice): List<Map<String, Any>> =
        withContext(Dispatchers.IO) {
            try {
                val json = httpGet("http://${device.host}:${device.port}/docs")
                val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                Log.e("NEXUS_NET", "fetchRemoteDocs: ${e.message}")
                emptyList()
            }
        }

    // ── Obtener el contenido de texto de un doc remoto ─────────────────────────
    suspend fun fetchRemoteContent(device: NetworkDevice, remotePath: String): String =
        withContext(Dispatchers.IO) {
            try {
                val encoded = java.net.URLEncoder.encode(remotePath, "UTF-8")
                val json = httpGet("http://${device.host}:${device.port}/content?path=$encoded")
                val map: Map<String, String> = gson.fromJson(
                    json, object : TypeToken<Map<String, String>>() {}.type
                )
                map["content"] ?: ""
            } catch (e: Exception) { "" }
        }

    // ── Buscar en todos los dispositivos encontrados ───────────────────────────
    suspend fun searchAllDevices(query: String): List<DocumentResult> =
        withContext(Dispatchers.IO) {
            _discoveredDevices.value
                .filter { it.isSharing }
                .map { device ->
                    async {
                        try {
                            fetchRemoteDocs(device)
                                .filter {
                                    it["name"]?.toString()
                                        ?.contains(query, ignoreCase = true) == true
                                }
                                .map {
                                    DocumentResult(
                                        name = it["name"]?.toString() ?: "",
                                        path = it["path"]?.toString() ?: "",
                                        extension = it["extension"]?.toString() ?: "",
                                        snippet = "📡 ${device.name}",
                                        score = 0.8f,
                                        deviceHost = device.host
                                    )
                                }
                        } catch (e: Exception) { emptyList() }
                    }
                }.awaitAll().flatten()
        }

    private fun pingDevice(host: String, port: Int): Map<String, Any>? = try {
        val json = httpGet("http://$host:$port/ping", timeoutMs = 2000)
        gson.fromJson(json, object : TypeToken<Map<String, Any>>() {}.type)
    } catch (e: Exception) { null }

    private fun httpGet(url: String, timeoutMs: Int = 4000): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = timeoutMs
        conn.readTimeout = timeoutMs
        conn.requestMethod = "GET"
        return conn.inputStream.bufferedReader().readText().also { conn.disconnect() }
    }
}
