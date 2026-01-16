package com.example.railchatbot.di

import com.example.railchatbot.BuildConfig
import com.example.railchatbot.data.remote.backend.BackendClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Hilt module for backend client dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object BackendModule {

    @Provides
    @Singleton
    fun provideBackendClient(
        httpClient: HttpClient,
        json: Json
    ): BackendClient = BackendClient(
        httpClient = httpClient,
        json = json,
        backendUrl = BuildConfig.BACKEND_URL
    )
}
