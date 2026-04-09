package com.kcb.kiosk

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class PinRes(
    @SerialName("pin") val pin: String = "",
    @SerialName("seconds_left") val seconds_left: Long = 0,
    @SerialName("amount") val amount: Double = 0.0,
    @SerialName("is_extension") val is_extension: Boolean = false
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
        private var instance: SupabaseClient? = null
        fun getInstance() = instance ?: synchronized(this) {
            instance ?: SupabaseClient().also { instance = it }
        }
    }

    suspend fun validatePin(pinValue: String): PinRes {
        return try {
            // FIX: Inalis ang 'filter { }' block
            val results = client.postgrest.from("credits").select {
                eq("pin", pinValue)
            }.decodeList<PinRes>()
            
            results.firstOrNull() ?: PinRes()
        } catch (e: Exception) {
            PinRes()
        }
    }

    suspend fun getCurrentRemainingSeconds(pinValue: String): Long {
        return try {
            val res = validatePin(pinValue)
            res.seconds_left
        } catch (e: Exception) {
            0L
        }
    }

    suspend fun updatePinTime(pinValue: String, newSeconds: Long, amount: Double, isExtension: Boolean): Boolean {
        return try {
            // FIX: Inalis ang 'filter { }' block
            client.postgrest.from("credits").update({
                set("seconds_left", newSeconds)
                set("amount", amount)
                set("is_extension", isExtension)
            }) {
                eq("pin", pinValue)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}