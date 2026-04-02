package com.kcb.kiosk

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseClient private constructor() {
    
    private val client = createSupabaseClient(
        supabaseUrl = "https://qbricrnjchbdyseeuwif.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFicmljcm5qY2hiZHlzZWV1d2lmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQyMDU0NDUsImV4cCI6MjA4OTc4MTQ0NX0.5sJqi3fZc4VIFQAIw1QptHt7MlGdnkn5SVxYdRu4f7Q"
    ) {
        install(Postgrest)
    }
    
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            client.postgrest["admin_settings"].select {
                limit(1)
            }.decodeList<Map<String, Any>>()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun validatePin(pin: String): PinValidationResult = withContext(Dispatchers.IO) {
        try {
            val result = client.postgrest["credits"]
                .select {
                    filter { eq("pin", pin) }
                }
                .decodeSingleOrNull<Map<String, Any>>()
            
            if (result != null) {
                val seconds = (result["seconds_left"] as? Number)?.toInt() ?: 0
                PinValidationResult(true, seconds, null)
            } else {
                PinValidationResult(false, 0, "PIN not found")
            }
        } catch (e: Exception) {
            PinValidationResult(false, 0, e.message ?: "Network error")
        }
    }
    
    suspend fun generatePin(customPin: String?, seconds: Int): String? = withContext(Dispatchers.IO) {
        try {
            val pin = customPin ?: (100000..999999).random().toString()
            
            client.postgrest["credits"].insert(
                mapOf(
                    "pin" to pin,
                    "seconds_left" to seconds
                )
            )
            pin
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getActivePins(): List<PinData> = withContext(Dispatchers.IO) {
        try {
            val results = client.postgrest["credits"]
                .select {
                    filter { gt("seconds_left", 0) }
                    order("created_at") { isDescending = true }
                }
                .decodeList<Map<String, Any>>()
            
            results.map {
                PinData(
                    pin = it["pin"] as String,
                    secondsLeft = (it["seconds_left"] as Number).toInt()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun getWhitelistApps(): List<String> = withContext(Dispatchers.IO) {
        try {
            val result = client.postgrest["admin_settings"]
                .select {
                    filter { eq("setting_key", "whitelist_apps") }
                }
                .decodeSingleOrNull<Map<String, Any>>()
            
            if (result != null) {
                val value = result["setting_value"] as? String ?: ""
                value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun updateWhitelistApps(apps: List<String>): Boolean = withContext(Dispatchers.IO) {
        try {
            val appsString = apps.joinToString(",")
            
            // Upsert - update or insert
            client.postgrest["admin_settings"].upsert(
                mapOf(
                    "setting_key" to "whitelist_apps",
                    "setting_value" to appsString,
                    "updated_at" to System.currentTimeMillis()
                )
            ) {
                onConflict("setting_key")
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
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
data class PinData(val pin: String, val secondsLeft: Int)
