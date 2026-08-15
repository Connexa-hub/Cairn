package com.cairn.app.di

import android.content.Context
import com.cairn.app.data.backup.BackupApi
import com.cairn.app.data.local.dao.CallLogDao
import com.cairn.app.data.local.dao.ContactDao
import com.cairn.app.data.local.db.CairnDatabase
import com.cairn.app.data.security.DbKeyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** Base URL is user-configurable (Settings > Backup) — this is the default for a self-hosted Render deploy. */
    private const val DEFAULT_BASE_URL = "https://cairn-backup-backend.onrender.com/"

    @Provides
    @Singleton
    fun provideCairnDatabase(@ApplicationContext context: Context, keyManager: DbKeyManager): CairnDatabase =
        CairnDatabase.getInstance(context, keyManager)

    @Provides
    fun provideContactDao(db: CairnDatabase): ContactDao = db.contactDao()

    @Provides
    fun provideCallLogDao(db: CairnDatabase): CallLogDao = db.callLogDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)   // backups can be large
        .writeTimeout(60, TimeUnit.SECONDS)
        // Deliberately no logging/analytics interceptors.
        .build()

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(DEFAULT_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideBackupApi(retrofit: Retrofit): BackupApi = retrofit.create(BackupApi::class.java)
}
