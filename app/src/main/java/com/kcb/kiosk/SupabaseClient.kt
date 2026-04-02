package com.kcb.kiosk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray

class SupabaseClient private constructor() {

    private val supabaseUrl = "https://qbricrnjchbdyseeuwif.supabase.co"
    private val apiKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFicmljcm5qY2hiZHlzZWV1d2lmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQyMDU0NDUsImV4cCI6MjA4OTc4MTQ0NX0.5sJqi3fZc4VIFQAIw1QptHt7MlGdnkn5SVxYdRu4f7Q"

    suspend fun validatePin(pin: String): PinValidationResult = withContext(Dispatchers.IO) {
        try {
            val url = URL("$supabaseUrl/rest/v1/credits?pin=eq.$pin")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("apikey", apiKey)
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                if (jsonArray.length() > 0) {
                    val jsonObject = jsonArray.getJSONObject(0)
                    val seconds = jsonObject.getInt("seconds_left")
                    PinValidationResult(true, seconds, null)
                } else {
                    PinValidationResult(false, 0, "PIN not found")
                }
            } else {
                PinValidationResult(false, 0, "HTTP error: $responseCode")
            }
        } catch (e: Exception) {
            PinValidationResult(false, 0, e.message ?: "Network error")
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
