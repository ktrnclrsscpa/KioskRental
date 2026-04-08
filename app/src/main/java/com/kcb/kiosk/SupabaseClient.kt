package com.kcb.kiosk

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

@Serializable
data class PinRes(
    val isValid: Boolean = false,
    val seconds_left: Long = 0,
    val pin: String = ""
)

class SupabaseClient {
    // SIGURADUHIN NA TAMA ANG URL AT KEY MO DITO
    private val client = createSupabaseClient(
        supabaseUrl = "https://qbricrnjchbdyseeuwif.supabase.co",
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
            client.from("credits").select {
                filter {
                    eq("pin", pinValue)
                }
            }.decodeSingle<PinRes>()
        } catch (e: Exception) {
            PinRes(false, 0)
        }
    }

    suspend fun getCurrentRemainingSeconds(pinValue: String): Long {
        return try {
            val res = client.from("credits").select {
                filter {
                    eq("pin", pinValue)
                }
            }.decodeSingle<PinRes>()
            res.seconds_left
        } catch (e: Exception) {
            0L
        }
    }

    suspend fun updatePinTime(pinValue: String, newSeconds: Long, amount: Double, isExtension: Boolean): Boolean {
        return try {
            client.from("credits").update({
                set("seconds_left", newSeconds)
                set("amount", amount)
                set("is_extension", isExtension)
            }) {
                filter {
                    eq("pin", pinValue)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}