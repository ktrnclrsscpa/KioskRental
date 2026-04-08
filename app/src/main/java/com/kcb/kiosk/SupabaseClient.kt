package com.kcb.kiosk

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.eq
import kotlinx.serialization.Serializable

@Serializable
data class PinRes(
    val isValid: Boolean = false,
    val seconds_left: Long = 0,
    val pin: String = ""
)

class SupabaseClient {
    private val client = createSupabaseClient(
        supabaseUrl = "https://qbrjcrnjchbdyseeuwif.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFicmljcm5qY2hiZHlzZWV1d2lmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQyMDU0NDUsImV4cCI6MjA4OTc4MTQ0NX0.5sJqi3fZc4VIFQAIw1QptHt7MlGdnkn5SVxYdRu4f7Q" 
    ) {
        install(Postgrest)
    }

    companion object {
        @Volatile
        private var instance: com.kcb.kiosk.SupabaseClient? = null
        fun getInstance() = instance ?: synchronized(this) {
            instance ?: com.kcb.kiosk.SupabaseClient().also { instance = it }
        }
    }

    suspend fun validatePin(pinValue: String): PinRes {
        return try {
            client.from("credits").select().decodeSingle<PinRes>()
        } catch (e: Exception) {
            PinRes(false, 0)
        }
    }
}