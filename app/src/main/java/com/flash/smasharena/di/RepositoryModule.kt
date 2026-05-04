package com.flash.smasharena.di

import com.flash.smasharena.data.repository.AuthRepositoryImpl
import com.flash.smasharena.data.repository.SlotRepositoryImpl
import com.flash.smasharena.data.repository.UserRepositoryImpl
import com.flash.smasharena.domain.repository.AuthRepository
import com.flash.smasharena.domain.repository.SlotRepository
import com.flash.smasharena.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds @Singleton
    abstract fun bindSlotRepository(impl: SlotRepositoryImpl): SlotRepository
}
