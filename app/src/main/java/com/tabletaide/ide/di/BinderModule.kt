package com.tabletaide.ide.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object BinderModule {
    // AnthropicClientImpl and GeminiClientImpl are auto-provided via @Singleton.
    // AgentViewModel injects both and selects per provider setting.
}
