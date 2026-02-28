package com.nexus.di

import android.content.Context
import androidx.room.Room
import com.nexus.data.local.DocumentDao
import com.nexus.data.local.LocalAiService
import com.nexus.data.local.NexusDatabase
import com.nexus.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideContext(@ApplicationContext ctx: Context): Context = ctx

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): NexusDatabase =
        Room.databaseBuilder(ctx, NexusDatabase::class.java, NexusDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton
    fun provideDocumentDao(db: NexusDatabase): DocumentDao = db.documentDao()

    @Provides @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    @Provides @Singleton
    fun provideLocalAiService(
        okHttpClient: OkHttpClient,
        settingsRepo: SettingsRepository
    ): LocalAiService {
        val settings = runBlocking { settingsRepo.getSettings() }
        return Retrofit.Builder()
            .baseUrl("http://${settings.apiHost}:${settings.apiPort}/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LocalAiService::class.java)
    }
}
