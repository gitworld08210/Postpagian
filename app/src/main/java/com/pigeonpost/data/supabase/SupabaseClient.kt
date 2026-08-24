package com.pigeonpost.data.supabase

import com.pigeonpost.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/**
 * Creates and configures the Supabase client with all required modules:
 * - Auth (GoTrue) for user authentication
 * - Postgrest for database operations
 * - Realtime for live message subscriptions
 * - Storage for file uploads (pigeon avatars, etc.)
 *
 * Credentials are read from BuildConfig fields, which are populated from
 * gradle.properties (or local.properties for local overrides).
 */
fun createPigeonPostSupabaseClient(): SupabaseClient {
    return createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
        install(Storage)
    }
}
