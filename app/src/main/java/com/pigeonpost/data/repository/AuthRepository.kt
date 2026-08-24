package com.pigeonpost.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository handling all authentication operations via Supabase Auth (GoTrue).
 */
@Singleton
open class AuthRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {

    private val auth: Auth get() = supabaseClient.auth

    /**
     * Sign up a new user with email and password.
     */
    open suspend fun signUp(email: String, password: String): Result<Unit> {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign in an existing user with email and password.
     */
    open suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign out the current user.
     */
    open suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get the currently authenticated user, or null if not signed in.
     */
    open fun getCurrentUser(): UserInfo? {
        return auth.currentUserOrNull()
    }

    /**
     * Returns true if a user is currently authenticated.
     */
    open fun isAuthenticated(): Boolean {
        return auth.currentUserOrNull() != null
    }

    /**
     * Send a password reset email to the given address.
     */
    open suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            supabaseClient.auth.resetPasswordForEmail(email = email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe authentication state changes.
     */
    fun observeAuthState(): Flow<Boolean> {
        return auth.sessionStatus.map { status ->
            auth.currentUserOrNull() != null
        }
    }
}
