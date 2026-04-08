package com.kcb.kiosk

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlinx.serialization.Serializable

@Serializable
data class PinRes(
    val isValid: Boolean = false,
    val seconds_left: Long = 0,
    val pin: String = ""
)

class SupabaseClient {
    private val client = createSupabaseClient(
        supabaseUrl = "YOUR_SUPABASE_URL",
        supabaseKey = "YOUR_SUPABASE_KEY"
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
