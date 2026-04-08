package com.kcb.kiosk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class PinData(val pin: String, val secondsLeft: Int)
data class SessionRecord(val pin: String, val minutes: Int, val amount: Double, val date: String)
data class IncomeStats(val daily: Double, val weekly: Double, val monthly: Double, val yearly: Double, val totalSessions: Int)

class SupabaseClient private constructor() {

    private val supabaseUrl = "https://qbricrnjchbdyseeuwif.supabase.co"
    private val apiKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFicmljcm5qY2hiZHlzZWV1d2lmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQyMDU0NDUsImV4cCI6MjA4OTc4MTQ0NX0.5sJqi3fZc4VIFQAIw1QptHt7MlGdnkn5SVxYdRu4f7Q"

    private val characters = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789"
    
    private fun generateRandomPin(): String = (1..6).map { characters.random() }.joinToString("")

    private fun addApiKeyToUrl(urlString: String): String {
        val separator = if (urlString.contains("?")) "&" else "?"
        return "$urlString${separator}apikey=$apiKey"
    }

    // --- PIN GENERATION ---
    suspend fun generatePin(customPin: String?, seconds: Int, amount: Double): String? = withContext(Dispatchers.IO) {
        try {
            val pin = if (!customPin.isNullOrEmpty()) customPin else generateRandomPin()
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/credits"))
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("apikey", apiKey)
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            
            val jsonBody = JSONObject().apply {
                put("pin", pin)
                put("seconds_left", seconds)
                put("amount", amount)
            }.toString()
            
            connection.outputStream.write(jsonBody.toByteArray())
            val responseCode = connection.responseCode
            connection.disconnect()
            return@withContext if (responseCode in 200..299) pin else null
        } catch (e: Exception) { null }
    }

    // --- PIN VALIDATION ---
    data class PinValidationResult(val isValid: Boolean, val secondsLeft: Int, val amount: Double, val error: String?)

    suspend fun validatePin(pin: String): PinValidationResult = withContext(Dispatchers.IO) {
        try {
            val encodedPin = URLEncoder.encode(pin, "UTF-8")
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/credits?pin=eq.$encodedPin"))
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("apikey", apiKey)
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            
            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                if (jsonArray.length() > 0) {
                    val obj = jsonArray.getJSONObject(0)
                    return@withContext PinValidationResult(true, obj.getInt("seconds_left"), obj.optDouble("amount", 0.0), null)
                }
                return@withContext PinValidationResult(false, 0, 0.0, "PIN not found")
            }
            return@withContext PinValidationResult(false, 0, 0.0, "Error: ${connection.responseCode}")
        } catch (e: Exception) { PinValidationResult(false, 0, 0.0, e.message) }
    }

    // --- UPDATE/EXTEND PIN ---
    suspend fun updatePinSeconds(pin: String, newSeconds: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val encodedPin = URLEncoder.encode(pin, "UTF-8")
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/credits?pin=eq.$encodedPin"))
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PATCH"
            connection.setRequestProperty("apikey", apiKey)
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            
            val jsonBody = JSONObject().apply {
                put("seconds_left", newSeconds)
            }.toString()
            
            connection.outputStream.write(jsonBody.toByteArray())
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode in 200..299
        } catch (e: Exception) { false }
    }

    // --- SESSION & EXTENSION LOGGING ---
    suspend fun recordExtension(pin: String, minutes: Int, amount: Double): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/session_history"))
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("apikey", apiKey)
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            
            val jsonBody = JSONObject().apply {
                put("pin", pin)
                put("minutes", minutes)
                put("amount", amount)
                put("status", "extension")
            }.toString()
            
            connection.outputStream.write(jsonBody.toByteArray())
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode in 200..299
        } catch (e: Exception) { false }
    }

    // --- ADMIN & TELEGRAM ---
    suspend fun sendTelegramNotification(message: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val (token, chatId) = getTelegramConfig()
            if (token.isEmpty() || chatId.isEmpty()) return@withContext false
            val dateTime = SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US).format(Date())
            val cleanMessage = "$message\n📅 $dateTime".replace("*", "")
            val encodedMessage = URLEncoder.encode(cleanMessage, "UTF-8")
            val url = URL("https://api.telegram.org/bot$token/sendMessage?chat_id=$chatId&text=$encodedMessage")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode == 200
        } catch (e: Exception) { false }
    }

    suspend fun getTelegramConfig(): Pair<String, String> = withContext(Dispatchers.IO) {
        // Shared logic to fetch from admin_settings table
        try {
            var token = ""
            var chatId = ""
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/admin_settings"))
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("apikey", apiKey)
            if (conn.responseCode == 200) {
                val json = JSONArray(conn.inputStream.bufferedReader().use { it.readText() })
                for (i in 0 until json.length()) {
                    val obj = json.getJSONObject(i)
                    when (obj.getString("setting_key")) {
                        "telegram_bot_token" -> token = obj.getString("setting_value")
                        "telegram_chat_id" -> chatId = obj.getString("setting_value")
                    }
                }
            }
            Pair(token, chatId)
        } catch (e: Exception) { Pair("", "") }
    }

    // --- OTHER UTILITIES ---
    suspend fun getActivePins(): List<PinData> = withContext(Dispatchers.IO) {
        try {
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/credits?seconds_left=gt.0&order=created_at.desc"))
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("apikey", apiKey)
            if (conn.responseCode == 200) {
                val json = JSONArray(conn.inputStream.bufferedReader().use { it.readText() })
                return@withContext List(json.length()) { i ->
                    val obj = json.getJSONObject(i)
                    PinData(obj.getString("pin"), obj.getInt("seconds_left"))
                }
            }
            emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getIncomeStats(): IncomeStats = withContext(Dispatchers.IO) {
        // Simplified income logic
        try {
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/session_history"))
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("apikey", apiKey)
            if (conn.responseCode == 200) {
                val json = JSONArray(conn.inputStream.bufferedReader().use { it.readText() })
                var total = 0.0
                for (i in 0 until json.length()) total += json.getJSONObject(i).getDouble("amount")
                return@withContext IncomeStats(total, total, total, total, json.length())
            }
            IncomeStats(0.0, 0.0, 0.0, 0.0, 0)
        } catch (e: Exception) { IncomeStats(0.0, 0.0, 0.0, 0.0, 0) }
    }

    companion object {
        private var instance: SupabaseClient? = null
        fun getInstance(): SupabaseClient {
            if (instance == null) instance = SupabaseClient()
            return instance!!
        }
    }
}
