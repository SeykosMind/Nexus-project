package com.nexus.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nexus.data.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("nexus_settings")

@Singleton
class SettingsRepository @Inject constructor(private val context: Context) {

    private val gson = Gson()

    private object Keys {
        val API_HOST     = stringPreferencesKey("api_host")
        val API_PORT     = stringPreferencesKey("api_port")
        val MODEL_NAME   = stringPreferencesKey("model_name")
        val WATCHED      = stringPreferencesKey("watched_folders")
        val AUTO_SYNC    = booleanPreferencesKey("auto_sync")
        val INCL_IMAGES  = booleanPreferencesKey("include_images")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val foldersJson = prefs[Keys.WATCHED] ?: ""
        val folders: List<String> = if (foldersJson.isEmpty()) {
            listOf("/storage/emulated/0/Documents", "/storage/emulated/0/Downloads")
        } else {
            gson.fromJson(foldersJson, object : TypeToken<List<String>>() {}.type)
        }
        AppSettings(
            apiHost       = prefs[Keys.API_HOST]   ?: "127.0.0.1",
            apiPort       = prefs[Keys.API_PORT]   ?: "8080",
            modelName     = prefs[Keys.MODEL_NAME] ?: "gemma",
            watchedFolders = folders,
            autoSync      = prefs[Keys.AUTO_SYNC]  ?: true,
            includeImages = prefs[Keys.INCL_IMAGES] ?: false
        )
    }

    suspend fun getSettings() = settingsFlow.first()

    suspend fun save(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.API_HOST]   = settings.apiHost
            prefs[Keys.API_PORT]   = settings.apiPort
            prefs[Keys.MODEL_NAME] = settings.modelName
            prefs[Keys.WATCHED]    = gson.toJson(settings.watchedFolders)
            prefs[Keys.AUTO_SYNC]  = settings.autoSync
            prefs[Keys.INCL_IMAGES] = settings.includeImages
        }
    }
}
