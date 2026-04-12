package com.kcb.kiosk

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable

class SupabaseClient private constructor() {

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://qbricrnjchbdyseeuwif.supabase.co", 
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFicmljcm5qY2hiZHlzZWV1d2lmIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3NDIwNTQ0NSwiZXhwIjoyMDg5NzgxNDQ1fQ.Zd2Qouvw-4myu89zgaWqq5nV2DBYMdjk93aQ6scA_uY"
    ) {
        install(Postgrest)
    }

    // Para sa Validation ng PIN sa Lock Screen
    suspend fun validatePin(pin: String): RentalPin? {
        return try {
            val response = client.postgrest["credits"].select(columns = Columns.ALL) {
                filter {
                    eq("pin", pin)
                    eq("status", "active")
                }
            }
            response.decodeSingleOrNull<RentalPin>()
        } catch (e: Exception) { null }
    }

    // Para i-update ang status kapag ginamit na
    suspend fun usePin(pin: String) {
        try {
            client.postgrest["credits"].update({
                set("status", "used")
            }) {
                filter { eq("pin", pin) }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Para sa AdminActivity (Create Feature)
    suspend fun createNewPin(pin: String, seconds: Long) {
        try {
            val newData = RentalPin(pin, seconds, "active")
            client.postgrest["credits"].insert(newData)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Para sa AdminActivity (Extend Feature)
    suspend fun extendPinTime(pin: String, extraSeconds: Long): Boolean {
        return try {
            val current = client.postgrest["credits"].select(columns = Columns.ALL) {
                filter { eq("pin", pin) }
            }.decodeSingleOrNull<RentalPin>()

            if (current != null) {
                val newTotal = current.seconds_left + extraSeconds
                client.postgrest["credits"].update({
                    set("seconds_left", newTotal)
                }) {
                    filter { eq("pin", pin) }
                }
                true
            } else false
        } catch (e: Exception) { false }
    }

    companion object {
        @Volatile
        private var instance: com.kcb.kiosk.SupabaseClient? = null
        fun getInstance(): com.kcb.kiosk.SupabaseClient {
            return instance ?: synchronized(this) {
                instance ?: SupabaseClient().also { instance = it }
            }
        }
    }
}

@Serializable
data class RentalPin(
    val pin: String,
    val seconds_left: Long,
    val status: String
)