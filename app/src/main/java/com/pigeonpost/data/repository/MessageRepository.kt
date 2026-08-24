package com.pigeonpost.data.repository

import android.util.Log
import com.pigeonpost.data.model.Conversation
import com.pigeonpost.data.model.Message
import com.pigeonpost.data.model.MessageStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository handling message CRUD operations and real-time subscriptions via Supabase.
 */
@Singleton
class MessageRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {

    companion object {
        private const val TAG = "MessageRepository"
        private const val MESSAGES_TABLE = "messages"
        private const val CONVERSATIONS_TABLE = "conversations"
    }

    /**
     * Send a new message (insert into messages table).
     */
    suspend fun sendMessage(message: Message): Result<Message> {
        return try {
            val result = supabaseClient.postgrest[MESSAGES_TABLE]
                .insert(message) {
                    select()
                }
                .decodeSingle<Message>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get a single message by its ID.
     */
    suspend fun getMessageById(messageId: String): Message? {
        return try {
            val messages = supabaseClient.postgrest[MESSAGES_TABLE]
                .select() {
                    filter {
                        eq("id", messageId)
                    }
                }
                .decodeList<Message>()
            messages.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get all messages for a conversation between two users.
     */
    suspend fun getMessages(userId: String, otherUserId: String): Result<List<Message>> {
        return try {
            val messages = supabaseClient.postgrest[MESSAGES_TABLE]
                .select() {
                    filter {
                        or {
                            and {
                                eq("sender_id", userId)
                                eq("receiver_id", otherUserId)
                            }
                            and {
                                eq("sender_id", otherUserId)
                                eq("receiver_id", userId)
                            }
                        }
                    }
                }
                .decodeList<Message>()
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update message status (FLYING -> DELIVERED or LOST).
     */
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus): Result<Unit> {
        return try {
            supabaseClient.postgrest[MESSAGES_TABLE]
                .update({
                    set("status", status.name)
                }) {
                    filter {
                        eq("id", messageId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update pigeon's current position during flight.
     */
    suspend fun updatePigeonPosition(
        messageId: String,
        lat: Double,
        lng: Double
    ): Result<Unit> {
        return try {
            supabaseClient.postgrest[MESSAGES_TABLE]
                .update({
                    set("pigeon_current_lat", lat)
                    set("pigeon_current_lng", lng)
                }) {
                    filter {
                        eq("id", messageId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all conversations for the current user.
     */
    suspend fun getConversations(userId: String): Result<List<Conversation>> {
        return try {
            val conversations = supabaseClient.postgrest[CONVERSATIONS_TABLE]
                .select() {
                    filter {
                        or {
                            eq("user_id_1", userId)
                            eq("user_id_2", userId)
                        }
                    }
                }
                .decodeList<Conversation>()
            Result.success(conversations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Subscribe to real-time message updates for a specific user.
     * Returns a Flow that emits new messages as they arrive.
     * Errors are logged and re-thrown so the UI layer can react accordingly.
     */
    fun observeMessages(userId: String): Flow<Message> = flow {
        val channel = supabaseClient.realtime.channel("messages-$userId")
        val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = MESSAGES_TABLE
            filter = "receiver_id=eq.$userId"
        }

        channel.subscribe()
        Log.d(TAG, "Realtime channel subscribed for user: $userId")

        changeFlow.collect { change ->
            try {
                val message = change.decodeRecord<Message>()
                emit(message)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode realtime message record", e)
            }
        }
    }.catch { e ->
        Log.e(TAG, "Realtime subscription failed for user: $userId", e)
        throw e
    }
}
