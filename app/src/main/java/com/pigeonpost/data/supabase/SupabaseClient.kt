package com.pigeonpost.data.supabase

import com.pigeonpost.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.plugins.HttpTimeout

/**
 * Creates and configures the Supabase client with all required modules:
 * - Auth (GoTrue) for user authentication
 * - Postgrest for database operations
 * - Realtime for live message subscriptions
 * - Storage for file uploads (pigeon avatars, etc.)
 *
 * Credentials are read from BuildConfig fields, which are populated from
 * gradle.properties (or local.properties for local overrides).
 *
 * HTTP timeouts are configured generously (60s request/socket, 30s connect)
 * to accommodate high-latency connections (e.g. India to ap-northeast-1).
 */
@OptIn(SupabaseInternal::class)
fun createPigeonPostSupabaseClient(): SupabaseClient {
    return createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        httpConfig {
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 60_000
            }
        }
        install(Auth)
        install(Postgrest)
        install(Realtime)
        install(Storage)
    }
}
