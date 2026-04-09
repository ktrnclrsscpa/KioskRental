package com.kcb.kiosk

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
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

    // Function para i-validate ang PIN
    suspend fun validatePin(pinValue: String): PinRes? {
        // Explicitly fetch all columns and filter by PIN as String
        val result = client.postgrest.from("credits")
            .select(columns = Columns.ALL) {
                eq("pin", pinValue)
            }
        
        val list = result.decodeList<PinRes>()
        return list.firstOrNull()
    }

    // Function para mag-update ng oras (kailangan ito mamaya sa timer)
    suspend fun updatePinTime(pinValue: String, newSeconds: Long): Boolean {
        return try {
            client.postgrest.from("credits").update({
                set("seconds_left", newSeconds)
            }) {
                eq("pin", pinValue)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}