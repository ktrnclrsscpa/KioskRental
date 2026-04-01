package com.kcb.kiosk

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseClient private constructor() {
    private val client by lazy {
        createSupabaseClient(
            supabaseUrl = "https://qbricrnjchbdyseeuwif.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFicmljcm5qY2hiZHlzZWV1d2lmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQyMDU0NDUsImV4cCI6MjA4OTc4MTQ0NX0.5sJqi3fZc4VIFQAIw1QptHt7MlGdnkn5SVxYdRu4f7Q"
        ) {
            install(Postgrest)
        }
    }

    suspend fun validatePin(pin: String): PinValidationResult = withContext(Dispatchers.IO) {
        try {
            // Use a simpler approach: get the response as a list
            val results = client.postgrest["credits"]
                .select {
                    filter { eq("pin", pin) }
                }
                .decodeList<Map<String, Any>>()
            
            if (results.isNotEmpty()) {
                val row = results[0]
                val seconds = (row["seconds_left"] as? Number)?.toInt() ?: 0
                PinValidationResult(true, seconds, null)
            } else {
                PinValidationResult(false, 0, "PIN not found")
            }
        } catch (e: Exception) {
            PinValidationResult(false, 0, e.message ?: "Unknown error")
        }
    }

    suspend fun getWhitelistApps(): List<String> = withContext(Dispatchers.IO) {
        listOf(
            "com.google.android.youtube",
            "com.roblox.client",
            "com.gcash.gcash"
        )
    }

    companion object {
        private var instance: SupabaseClient? = null
        fun getInstance(): SupabaseClient {
            if (instance == null) instance = SupabaseClient()
            return instance!!
        }
    }
}

data class PinValidationResult(val isValid: Boolean, val secondsLeft: Int, val error: String?)
