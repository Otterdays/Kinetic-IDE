package com.tabletaide.ide.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object BinderModule {
    // Llm clients (Anthropic, Gemini, OpenAI, Grok, OpenRouter) are auto-provided via @Singleton.
    // LlmClientResolver selects per provider + saved model.
}
