package com.example.railchatbot.di

import com.example.railchatbot.BuildConfig
import com.example.railchatbot.data.remote.mcp.McpClientWrapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object McpModule {

    @Provides
    @Singleton
    fun provideMcpClientWrapper(
        httpClient: HttpClient,
        json: Json
    ): McpClientWrapper = McpClientWrapper(
        httpClient = httpClient,
        json = json,
        serverUrl = BuildConfig.MCP_SERVER_URL,
        apiHost = BuildConfig.RAPIDAPI_HOST,
        apiKey = BuildConfig.RAPIDAPI_KEY
    )
}
