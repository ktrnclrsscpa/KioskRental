package com.kcb.kiosk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject

data class PinData(val pin: String, val secondsLeft: Int)

class SupabaseClient private constructor() {
    private val supabaseUrl = "https://qbricrnjchbdyseeuwif.supabase.co"
    private val apiKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFicmljcm5qY2hiZHlzZWV1d2lmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQyMDU0NDUsImV4cCI6MjA4OTc4MTQ0NX0.5sJqi3fZc4VIFQAIw1QptHt7MlGdnkn5SVxYdRu4f7Q"

    private fun addApiKeyToUrl(urlString: String) = "$urlString${if (urlString.contains("?")) "&" else "?"}apikey=$apiKey"

    suspend fun validatePin(pin: String): MainActivity.PinValidationResult = withContext(Dispatchers.IO) {
        try {
            val encodedPin = URLEncoder.encode(pin, "UTF-8")
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/credits?pin=eq.$encodedPin"))
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                if (jsonArray.length() > 0) {
                    val seconds = jsonArray.getJSONObject(0).getInt("seconds_left")
                    return@withContext MainActivity.PinValidationResult(true, seconds)
                }
            }
            MainActivity.PinValidationResult(false, 0)
        } catch (e: Exception) { MainActivity.PinValidationResult(false, 0) }
    }

    suspend fun updatePinSeconds(pin: String, newSeconds: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val encodedPin = URLEncoder.encode(pin, "UTF-8")
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/credits?pin=eq.$encodedPin"))
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            val jsonBody = "{\"seconds_left\":$newSeconds}"
            conn.outputStream.write(jsonBody.toByteArray())
            conn.responseCode in 200..299
        } catch (e: Exception) { false }
    }

    suspend fun getActivePins(): List<PinData> = withContext(Dispatchers.IO) {
        try {
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/credits?seconds_left=gt.0"))
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                val list = mutableListOf<PinData>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(PinData(obj.getString("pin"), obj.getInt("seconds_left")))
                }
                return@withContext list
            }
            emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun recordExtension(pin: String, minutes: Int, amount: Double) = withContext(Dispatchers.IO) {
        try {
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/session_history"))
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            val jsonBody = JSONObject().apply {
                put("pin", pin); put("minutes", minutes); put("amount", amount); put("status", "extension")
            }.toString()
            conn.outputStream.write(jsonBody.toByteArray())
            conn.responseCode
        } catch (e: Exception) { }
    }

    companion object {
        private var instance: SupabaseClient? = null
        fun getInstance(): SupabaseClient {
            if (instance == null) instance = SupabaseClient()
            return instance!!
        }
    }
}
