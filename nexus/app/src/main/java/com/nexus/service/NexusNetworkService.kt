package com.nexus.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.nexus.data.local.NexusDatabase
import com.nexus.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject

@AndroidEntryPoint
class NexusNetworkService : Service() {

    @Inject lateinit var database: NexusDatabase
    @Inject lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    private var nsdManager: NsdManager? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private val gson = Gson()

    companion object {
        const val PORT = 7432
        const val SERVICE_TYPE = "_nexus._tcp."
        const val CHANNEL_ID = "nexus_network"

        fun start(context: Context) =
            context.startForegroundService(Intent(context, NexusNetworkService::class.java))

        fun stop(context: Context) =
            context.stopService(Intent(context, NexusNetworkService::class.java))
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(2, buildNotification())
        acquireMulticastLock()
        startHttpServer()
        registerNsd()
    }

    override fun onDestroy() {
        scope.cancel()
        serverSocket?.close()
        try { nsdManager?.unregisterService(registrationListener) } catch (e: Exception) {}
        multicastLock?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Servidor HTTP ─────────────────────────────────────────────────────────
    private fun startHttpServer() {
        scope.launch {
            try {
                serverSocket = ServerSocket(PORT)
                Log.d("NEXUS_NET", "Servidor HTTP escuchando en puerto $PORT")
                while (isActive) {
                    val client = serverSocket?.accept() ?: break
                    launch { handleClient(client) }
                }
            } catch (e: Exception) {
                Log.e("NEXUS_NET", "Error servidor: ${e.message}")
            }
        }
    }

    private suspend fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)
            val requestLine = reader.readLine() ?: return
            val path = requestLine.split(" ").getOrElse(1) { "/" }

            // Lee la carpeta compartida configurada por el usuario
            val settings = settingsRepository.getSettings()
            val sharedFolder = settings.sharedNetworkFolder

            when {
                // /ping → confirma que este dispositivo tiene NEXUS activo
                path == "/ping" -> {
                    val count = database.documentDao().count()
                    val deviceName = android.os.Build.MODEL
                    sendJson(writer, 200,
                        """{"status":"ok","device":"$deviceName","docs":$count,"sharing":${sharedFolder.isNotBlank()}}""")
                }

                // /docs → lista los documentos de la carpeta compartida solamente
                path.startsWith("/docs") -> {
                    if (sharedFolder.isBlank()) {
                        sendJson(writer, 403, """{"error":"este dispositivo no tiene carpeta compartida configurada"}""")
                        return
                    }
                    // Solo devuelve docs que estén dentro de la carpeta compartida
                    val docs = database.documentDao().getByFolder(sharedFolder)
                    val summary = docs.map {
                        mapOf(
                            "name" to it.name,
                            "extension" to it.extension,
                            "path" to it.path,
                            "sizeBytes" to it.sizeBytes,
                            "lastModified" to it.lastModified
                        )
                    }
                    sendJson(writer, 200, gson.toJson(summary))
                }

                // /content?path=... → devuelve el texto de un doc específico
                path.startsWith("/content") -> {
                    if (sharedFolder.isBlank()) {
                        sendJson(writer, 403, """{"error":"sin carpeta compartida"}""")
                        return
                    }
                    val docPath = java.net.URLDecoder.decode(
                        path.substringAfter("path="), "UTF-8"
                    )
                    // Seguridad: solo sirve archivos que estén dentro de la carpeta compartida
                    if (!docPath.startsWith(sharedFolder)) {
                        sendJson(writer, 403, """{"error":"acceso denegado"}""")
                        return
                    }
                    val doc = database.documentDao().getByPath(docPath)
                    if (doc != null) {
                        sendJson(writer, 200, gson.toJson(mapOf("content" to doc.content)))
                    } else {
                        sendJson(writer, 404, """{"error":"no encontrado"}""")
                    }
                }

                else -> sendJson(writer, 404, """{"error":"endpoint desconocido"}""")
            }
        } catch (e: Exception) {
            Log.e("NEXUS_NET", "Error cliente: ${e.message}")
        } finally {
            socket.close()
        }
    }

    private fun sendJson(writer: PrintWriter, code: Int, body: String) {
        val status = if (code == 200) "200 OK" else "$code Error"
        writer.print("HTTP/1.1 $status\r\n")
        writer.print("Content-Type: application/json\r\n")
        writer.print("Content-Length: ${body.toByteArray().size}\r\n")
        writer.print("Connection: close\r\n\r\n")
        writer.print(body)
        writer.flush()
    }

    // ── mDNS: anuncia este dispositivo en la red WiFi ──────────────────────────
    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(info: NsdServiceInfo) {
            Log.d("NEXUS_NET", "NSD registrado: ${info.serviceName}")
        }
        override fun onRegistrationFailed(info: NsdServiceInfo, code: Int) {
            Log.e("NEXUS_NET", "NSD falló: $code")
        }
        override fun onServiceUnregistered(info: NsdServiceInfo) {}
        override fun onUnregistrationFailed(info: NsdServiceInfo, code: Int) {}
    }

    private fun registerNsd() {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "NEXUS-${android.os.Build.MODEL.replace(" ", "-")}"
            serviceType = SERVICE_TYPE
            port = PORT
        }
        nsdManager = (getSystemService(NSD_SERVICE) as NsdManager).also {
            it.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        }
    }

    private fun acquireMulticastLock() {
        val wifi = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        multicastLock = wifi.createMulticastLock("nexus_multicast").apply {
            setReferenceCounted(true)
            acquire()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "NEXUS Red Local", NotificationManager.IMPORTANCE_LOW
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NEXUS")
            .setContentText("Compartiendo documentos en red local")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setOngoing(true)
            .build()
}
