package com.nexus.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.nexus.data.repository.DocumentRepository
import com.nexus.data.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class IndexingWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val repo: DocumentRepository,
    private val settingsRepo: SettingsRepository
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            val settings = settingsRepo.getSettings()
            settings.watchedFolders.forEach { folder ->
                repo.indexFolder(folder)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "nexus_background_indexing"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<IndexingWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
