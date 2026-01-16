package com.example.railchatbot.di

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.client.OpenAI
import com.example.railchatbot.BuildConfig
import com.example.railchatbot.data.remote.ai.LlmClient
import com.example.railchatbot.data.remote.ai.OpenAiLlmClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideOpenAI(): OpenAI = OpenAI(
        token = BuildConfig.OPENAI_API_KEY,
        timeout = Timeout(socket = 60.seconds)
    )

    @Provides
    @Singleton
    fun provideLlmClient(openAI: OpenAI): LlmClient = OpenAiLlmClient(openAI)
}
