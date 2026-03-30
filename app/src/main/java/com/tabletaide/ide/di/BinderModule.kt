package com.tabletaide.ide.di

import com.tabletaide.ide.agent.AnthropicClient
import com.tabletaide.ide.agent.AnthropicClientImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BinderModule {
    @Binds
    @Singleton
    abstract fun bindAnthropicClient(impl: AnthropicClientImpl): AnthropicClient
}
