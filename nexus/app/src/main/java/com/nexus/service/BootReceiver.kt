package com.nexus.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Arranca el indexador periódico
            IndexingWorker.schedule(context)
            // Arranca el servidor de red local (para compartir en WiFi sin abrir la app)
            NexusNetworkService.start(context)
        }
    }
