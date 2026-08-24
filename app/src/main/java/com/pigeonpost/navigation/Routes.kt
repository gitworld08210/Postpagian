package com.pigeonpost.navigation

/**
 * Defines all navigation routes in the PigeonPost app.
 */
sealed class Routes(val route: String) {
    /** Animated splash screen with pigeon and parchment */
    data object Splash : Routes("splash")

    /** Login/Register screen with medieval theme */
    data object Login : Routes("login")

    /** List of conversations styled as sealed letters */
    data object Conversations : Routes("conversations")

    /** Recipient picker listing all other registered messengers */
    data object NewChat : Routes("new_chat")

    /** Chat screen with parchment message bubbles */
    data object Chat : Routes("chat/{userId}") {
        fun createRoute(userId: String): String = "chat/$userId"
    }

    /** Map showing pigeon's real-time position */
    data object PigeonMap : Routes("pigeon_map/{messageId}") {
        fun createRoute(messageId: String): String = "pigeon_map/$messageId"
    }
}
