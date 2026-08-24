package com.pigeonpost.di

import android.content.Context
import com.pigeonpost.data.repository.AuthRepository
import com.pigeonpost.data.repository.MessageRepository
import com.pigeonpost.data.repository.UserRepository
import com.pigeonpost.data.supabase.createPigeonPostSupabaseClient
import com.pigeonpost.domain.DefaultLocationProvider
import com.pigeonpost.domain.DeliverySimulator
import com.pigeonpost.domain.LocationProvider
import com.pigeonpost.domain.PigeonDeliveryCalculator
import com.pigeonpost.utils.NotificationHelper
import com.pigeonpost.utils.SoundManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createPigeonPostSupabaseClient()
    }

    @Provides
    @Singleton
    fun provideAuthRepository(supabaseClient: SupabaseClient): AuthRepository {
        return AuthRepository(supabaseClient)
    }

    @Provides
    @Singleton
    fun provideMessageRepository(supabaseClient: SupabaseClient): MessageRepository {
        return MessageRepository(supabaseClient)
    }

    @Provides
    @Singleton
    fun provideUserRepository(supabaseClient: SupabaseClient): UserRepository {
        return UserRepository(supabaseClient)
    }

    @Provides
    @Singleton
    fun providePigeonDeliveryCalculator(): PigeonDeliveryCalculator {
        return PigeonDeliveryCalculator()
    }

    @Provides
    @Singleton
    fun provideDeliverySimulator(calculator: PigeonDeliveryCalculator): DeliverySimulator {
        return DeliverySimulator(calculator)
    }

    @Provides
    @Singleton
    fun provideLocationProvider(): LocationProvider {
        return DefaultLocationProvider()
    }

    @Provides
    @Singleton
    fun provideSoundManager(@ApplicationContext context: Context): SoundManager {
        return SoundManager(context)
    }

    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper {
        return NotificationHelper(context)
    }
}
