package com.kcb.kiosk

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
// ITO ANG IMPORT NA NAWALA KAYA NAG-ERROR ANG 'eq':
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlinx.serialization.Serializable

class SupabaseClient private constructor() {

    private val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://qbricrnjchbdyseeuwif.supabase.co", // Palitan mo ito ng actual URL mo
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFicmljcm5qY2hiZHlzZWV1d2lmIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3NDIwNTQ0NSwiZXhwIjoyMDg5NzgxNDQ1fQ.Zd2Qouvw-4myu89zgaWqq5nV2DBYMdjk93aQ6scA_uY" // Palitan mo ito ng actual Key mo
    ) {
        install(Postgrest)
    }

    suspend fun validatePin(pin: String): RentalPin? {
        return try {
            val result = client.from("credits")
                .select {
                    filter {
                        // Dito ginagamit yung 'eq'
                        eq("pin", pin)
                    }
                }
                .decodeSingleOrNull<RentalPin>()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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
    val seconds_left: Long
)
