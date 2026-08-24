package com.pigeonpost.data.repository

import android.util.Log
import com.pigeonpost.data.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for reading and updating messenger profiles (the `profiles` table).
 *
 * RLS lets any authenticated user SELECT every profile, which is what makes the
 * recipient picker possible, while INSERT/UPDATE are restricted to one's own row.
 */
@Singleton
class UserRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {

    companion object {
        private const val TAG = "UserRepository"
        private const val PROFILES_TABLE = "profiles"
    }

    /**
     * Fetch every registered messenger except the current user.
     * Used to populate the "New Correspondence" recipient picker.
     */
    suspend fun getAllUsersExcept(currentUserId: String): Result<List<User>> {
        return try {
            val profiles = supabaseClient.postgrest[PROFILES_TABLE]
                .select()
                .decodeList<User>()
            Result.success(profiles.filter { it.id != currentUserId })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load profiles", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch a single profile by id. Needed for the recipient's display name and
     * their last known coordinates so a real flight distance can be computed.
     * Returns null when the profile is missing or unreadable.
     */
    suspend fun getUserById(userId: String): User? {
        return try {
            supabaseClient.postgrest[PROFILES_TABLE]
                .select() {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeList<User>()
                .firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load profile for id=$userId", e)
            null
        }
    }

    /**
     * Persist the current user's latest coordinates on their own profile so the
     * other party can compute a real return distance.
     */
    suspend fun updateMyLocation(
        userId: String,
        latitude: Double,
        longitude: Double
    ): Result<Unit> {
        return try {
            supabaseClient.postgrest[PROFILES_TABLE]
                .update({
                    set("last_latitude", latitude)
                    set("last_longitude", longitude)
                }) {
                    filter {
                        eq("id", userId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update location for id=$userId", e)
            Result.failure(e)
        }
    }
}
