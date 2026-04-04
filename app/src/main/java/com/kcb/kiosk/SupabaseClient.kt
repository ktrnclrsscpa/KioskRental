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

class SupabaseClient private constructor() {

    private val supabaseUrl = "https://qbricrnjchbdyseeuwif.supabase.co"
    private val apiKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFicmljcm5qY2hiZHlzZWV1d2lmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQyMDU0NDUsImV4cCI6MjA4OTc4MTQ0NX0.5sJqi3fZc4VIFQAIw1QptHt7MlGdnkn5SVxYdRu4f7Q"

    private val characters = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789"
    
    private fun generateRandomPin(): String = (1..6).map { characters.random() }.joinToString("")

    private fun addApiKeyToUrl(urlString: String): String {
        val separator = if (urlString.contains("?")) "&" else "?"
        return "$urlString${separator}apikey=$apiKey"
    }

    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/admin_settings?limit=1"))
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("apikey", apiKey)
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode == 200
        } catch (e: Exception) { false }
    }

    suspend fun validatePin(pin: String): PinValidationResult = withContext(Dispatchers.IO) {
        try {
            val encodedPin = URLEncoder.encode(pin, "UTF-8")
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/credits?pin=eq.$encodedPin"))
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("apikey", apiKey)
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                if (jsonArray.length() > 0) {
                    val seconds = jsonArray.getJSONObject(0).getInt("seconds_left")
                    return@withContext PinValidationResult(true, seconds, null)
                } else {
                    return@withContext PinValidationResult(false, 0, "PIN not found")
                }
            } else {
                return@withContext PinValidationResult(false, 0, "HTTP error: $responseCode")
            }
        } catch (e: Exception) {
            return@withContext PinValidationResult(false, 0, e.message ?: "Network error")
        }
    }

    suspend fun generatePin(customPin: String?, seconds: Int): String? = withContext(Dispatchers.IO) {
        try {
            val pin = if (!customPin.isNullOrEmpty()) customPin else generateRandomPin()
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/credits"))
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("apikey", apiKey)
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            val jsonBody = JSONObject().apply { put("pin", pin); put("seconds_left", seconds) }.toString()
            connection.outputStream.write(jsonBody.toByteArray())
            val responseCode = connection.responseCode
            connection.disconnect()
            return@withContext if (responseCode in 200..299) pin else null
        } catch (e: Exception) { null }
    }

    suspend fun getActivePins(): List<PinData> = withContext(Dispatchers.IO) {
        try {
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/credits?seconds_left=gt.0&order=created_at.desc"))
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("apikey", apiKey)
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                val list = mutableListOf<PinData>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(PinData(obj.getString("pin"), obj.getInt("seconds_left")))
                }
                return@withContext list
            } else {
                return@withContext emptyList()
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getWhitelistApps(): List<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/admin_settings?setting_key=eq.whitelist_apps"))
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("apikey", apiKey)
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                if (jsonArray.length() > 0) {
                    val value = jsonArray.getJSONObject(0).getString("setting_value")
                    return@withContext value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                } else {
                    return@withContext emptyList()
                }
            } else {
                return@withContext emptyList()
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun updateWhitelistApps(apps: List<String>): Boolean = withContext(Dispatchers.IO) {
        try {
            val appsString = apps.joinToString(",")
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/admin_settings?setting_key=eq.whitelist_apps"))
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PATCH"
            connection.setRequestProperty("apikey", apiKey)
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            val jsonBody = JSONObject().apply { put("setting_value", appsString); put("updated_at", System.currentTimeMillis()) }.toString()
            connection.outputStream.write(jsonBody.toByteArray())
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode in 200..299
        } catch (e: Exception) { false }
    }

    suspend fun deletePin(pin: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val encodedPin = URLEncoder.encode(pin, "UTF-8")
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/credits?pin=eq.$encodedPin"))
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "DELETE"
            connection.setRequestProperty("apikey", apiKey)
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode in 200..299
        } catch (e: Exception) { false }
    }

    suspend fun extendTime(pin: String, extraMinutes: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val encodedPin = URLEncoder.encode(pin, "UTF-8")
            val getUrl = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/credits?pin=eq.$encodedPin"))
            val getConnection = getUrl.openConnection() as HttpURLConnection
            getConnection.requestMethod = "GET"
            getConnection.setRequestProperty("apikey", apiKey)
            getConnection.setRequestProperty("Authorization", "Bearer $apiKey")
            val currentSeconds = if (getConnection.responseCode == 200) {
                val responseText = getConnection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                if (jsonArray.length() > 0) jsonArray.getJSONObject(0).getInt("seconds_left") else return@withContext false
            } else return@withContext false
            getConnection.disconnect()
            val newSeconds = currentSeconds + (extraMinutes * 60)
            val updateUrl = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/credits?pin=eq.$encodedPin"))
            val updateConnection = updateUrl.openConnection() as HttpURLConnection
            updateConnection.requestMethod = "PATCH"
            updateConnection.setRequestProperty("apikey", apiKey)
            updateConnection.setRequestProperty("Authorization", "Bearer $apiKey")
            updateConnection.setRequestProperty("Content-Type", "application/json")
            updateConnection.doOutput = true
            val jsonBody = "{\"seconds_left\":$newSeconds}"
            updateConnection.outputStream.write(jsonBody.toByteArray())
            val responseCode = updateConnection.responseCode
            updateConnection.disconnect()
            responseCode in 200..299
        } catch (e: Exception) { false }
    }

    suspend fun updateTime(pin: String, seconds: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val encodedPin = URLEncoder.encode(pin, "UTF-8")
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/credits?pin=eq.$encodedPin"))
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PATCH"
            connection.setRequestProperty("apikey", apiKey)
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            val jsonBody = "{\"seconds_left\":$seconds}"
            connection.outputStream.write(jsonBody.toByteArray())
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode in 200..299
        } catch (e: Exception) { false }
    }

    // ==================== PRICING FUNCTIONS ====================

    data class PricingConfig(
        val pricingType: String,
        val priceAmount: Double,
        val durationMinutes: Int,
        val extendPrice: Double,
        val extendDuration: Int
    )

    suspend fun getPricingConfig(): PricingConfig = withContext(Dispatchers.IO) {
        try {
            var pricingType = "fixed"
            var priceAmount = 15.0
            var durationMinutes = 60
            var extendPrice = 10.0
            var extendDuration = 30
            
            val url1 = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/admin_settings?setting_key=eq.pricing_type"))
            val conn1 = url1.openConnection() as HttpURLConnection
            conn1.requestMethod = "GET"
            conn1.setRequestProperty("apikey", apiKey)
            conn1.setRequestProperty("Authorization", "Bearer $apiKey")
            if (conn1.responseCode == 200) {
                val responseText = conn1.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                if (jsonArray.length() > 0) {
                    pricingType = jsonArray.getJSONObject(0).getString("setting_value")
                }
            }
            conn1.disconnect()
            
            val url2 = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/admin_settings?setting_key=eq.price_amount"))
            val conn2 = url2.openConnection() as HttpURLConnection
            conn2.requestMethod = "GET"
            conn2.setRequestProperty("apikey", apiKey)
            conn2.setRequestProperty("Authorization", "Bearer $apiKey")
            if (conn2.responseCode == 200) {
                val responseText = conn2.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                if (jsonArray.length() > 0) {
                    priceAmount = jsonArray.getJSONObject(0).getString("setting_value").toDoubleOrNull() ?: 15.0
                }
            }
            conn2.disconnect()
            
            val url3 = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/admin_settings?setting_key=eq.price_duration_minutes"))
            val conn3 = url3.openConnection() as HttpURLConnection
            conn3.requestMethod = "GET"
            conn3.setRequestProperty("apikey", apiKey)
            conn3.setRequestProperty("Authorization", "Bearer $apiKey")
            if (conn3.responseCode == 200) {
                val responseText = conn3.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                if (jsonArray.length() > 0) {
                    durationMinutes = jsonArray.getJSONObject(0).getString("setting_value").toIntOrNull() ?: 60
                }
            }
            conn3.disconnect()
            
            val url4 = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/admin_settings?setting_key=eq.extend_price_amount"))
            val conn4 = url4.openConnection() as HttpURLConnection
            conn4.requestMethod = "GET"
            conn4.setRequestProperty("apikey", apiKey)
            conn4.setRequestProperty("Authorization", "Bearer $apiKey")
            if (conn4.responseCode == 200) {
                val responseText = conn4.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                if (jsonArray.length() > 0) {
                    extendPrice = jsonArray.getJSONObject(0).getString("setting_value").toDoubleOrNull() ?: 10.0
                }
            }
            conn4.disconnect()
            
            val url5 = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/admin_settings?setting_key=eq.extend_duration_minutes"))
            val conn5 = url5.openConnection() as HttpURLConnection
            conn5.requestMethod = "GET"
            conn5.setRequestProperty("apikey", apiKey)
            conn5.setRequestProperty("Authorization", "Bearer $apiKey")
            if (conn5.responseCode == 200) {
                val responseText = conn5.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                if (jsonArray.length() > 0) {
                    extendDuration = jsonArray.getJSONObject(0).getString("setting_value").toIntOrNull() ?: 30
                }
            }
            conn5.disconnect()
            
            PricingConfig(pricingType, priceAmount, durationMinutes, extendPrice, extendDuration)
        } catch (e: Exception) {
            PricingConfig("fixed", 15.0, 60, 10.0, 30)
        }
    }

    suspend fun updatePricingConfig(
        pricingType: String,
        priceAmount: Double,
        durationMinutes: Int,
        extendPrice: Double,
        extendDuration: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url1 = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/admin_settings?setting_key=eq.pricing_type"))
            val conn1 = url1.openConnection() as HttpURLConnection
            conn1.requestMethod = "PATCH"
            conn1.setRequestProperty("apikey", apiKey)
            conn1.setRequestProperty("Authorization", "Bearer $apiKey")
            conn1.setRequestProperty("Content-Type", "application/json")
            conn1.doOutput = true
            conn1.outputStream.write("{\"setting_value\":\"$pricingType\"}".toByteArray())
            conn1.disconnect()
            
            val url2 = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/admin_settings?setting_key=eq.price_amount"))
            val conn2 = url2.openConnection() as HttpURLConnection
            conn2.requestMethod = "PATCH"
            conn2.setRequestProperty("apikey", apiKey)
            conn2.setRequestProperty("Authorization", "Bearer $apiKey")
            conn2.setRequestProperty("Content-Type", "application/json")
            conn2.doOutput = true
            conn2.outputStream.write("{\"setting_value\":\"$priceAmount\"}".toByteArray())
            conn2.disconnect()
            
            val url3 = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/admin_settings?setting_key=eq.price_duration_minutes"))
            val conn3 = url3.openConnection() as HttpURLConnection
            conn3.requestMethod = "PATCH"
            conn3.setRequestProperty("apikey", apiKey)
            conn3.setRequestProperty("Authorization", "Bearer $apiKey")
            conn3.setRequestProperty("Content-Type", "application/json")
            conn3.doOutput = true
            conn3.outputStream.write("{\"setting_value\":\"$durationMinutes\"}".toByteArray())
            conn3.disconnect()
            
            val url4 = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/admin_settings?setting_key=eq.extend_price_amount"))
            val conn4 = url4.openConnection() as HttpURLConnection
            conn4.requestMethod = "PATCH"
            conn4.setRequestProperty("apikey", apiKey)
            conn4.setRequestProperty("Authorization", "Bearer $apiKey")
            conn4.setRequestProperty("Content-Type", "application/json")
            conn4.doOutput = true
            conn4.outputStream.write("{\"setting_value\":\"$extendPrice\"}".toByteArray())
            conn4.disconnect()
            
            val url5 = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/admin_settings?setting_key=eq.extend_duration_minutes"))
            val conn5 = url5.openConnection() as HttpURLConnection
            conn5.requestMethod = "PATCH"
            conn5.setRequestProperty("apikey", apiKey)
            conn5.setRequestProperty("Authorization", "Bearer $apiKey")
            conn5.setRequestProperty("Content-Type", "application/json")
            conn5.doOutput = true
            conn5.outputStream.write("{\"setting_value\":\"$extendDuration\"}".toByteArray())
            conn5.disconnect()
            
            true
        } catch (e: Exception) { false }
    }

    // ==================== TELEGRAM FUNCTIONS ====================

    suspend fun getTelegramConfig(): Pair<String, String> = withContext(Dispatchers.IO) {
        try {
            var token = ""
            var chatId = ""
            
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/admin_settings?setting_key=eq.telegram_bot_token"))
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("apikey", apiKey)
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            
            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                if (jsonArray.length() > 0) {
                    token = jsonArray.getJSONObject(0).getString("setting_value")
                }
            }
            connection.disconnect()
            
            val url2 = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/admin_settings?setting_key=eq.telegram_chat_id"))
            val connection2 = url2.openConnection() as HttpURLConnection
            connection2.requestMethod = "GET"
            connection2.setRequestProperty("apikey", apiKey)
            connection2.setRequestProperty("Authorization", "Bearer $apiKey")
            
            if (connection2.responseCode == 200) {
                val responseText = connection2.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                if (jsonArray.length() > 0) {
                    chatId = jsonArray.getJSONObject(0).getString("setting_value")
                }
            }
            connection2.disconnect()
            
            Pair(token, chatId)
        } catch (e: Exception) { Pair("", "") }
    }

    suspend fun updateTelegramConfig(token: String, chatId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url1 = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/admin_settings?setting_key=eq.telegram_bot_token"))
            val conn1 = url1.openConnection() as HttpURLConnection
            conn1.requestMethod = "PATCH"
            conn1.setRequestProperty("apikey", apiKey)
            conn1.setRequestProperty("Authorization", "Bearer $apiKey")
            conn1.setRequestProperty("Content-Type", "application/json")
            conn1.doOutput = true
            conn1.outputStream.write("{\"setting_value\":\"$token\"}".toByteArray())
            conn1.disconnect()
            
            val url2 = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/admin_settings?setting_key=eq.telegram_chat_id"))
            val conn2 = url2.openConnection() as HttpURLConnection
            conn2.requestMethod = "PATCH"
            conn2.setRequestProperty("apikey", apiKey)
            conn2.setRequestProperty("Authorization", "Bearer $apiKey")
            conn2.setRequestProperty("Content-Type", "application/json")
            conn2.doOutput = true
            conn2.outputStream.write("{\"setting_value\":\"$chatId\"}".toByteArray())
            conn2.disconnect()
            
            true
        } catch (e: Exception) { false }
    }

    // Fixed Telegram notification function using GET request (same as browser test)
    suspend fun sendTelegramNotification(message: String) {
        try {
            val (token, chatId) = getTelegramConfig()
            if (token.isEmpty() || chatId.isEmpty()) {
                return
            }
            
            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val url = URL("https://api.telegram.org/bot$token/sendMessage?chat_id=$chatId&text=$encodedMessage&parse_mode=HTML")
            
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            connection.responseCode
            connection.disconnect()
            
        } catch (e: Exception) {
            // Silent fail
        }
    }

    // ==================== DASHBOARD FUNCTIONS ====================

    suspend fun recordSession(pin: String, minutes: Int, amount: Double): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/session_history"))
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("apikey", apiKey)
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val jsonBody = JSONObject().apply {
                put("pin", pin)
                put("minutes", minutes)
                put("amount", amount)
                put("status", "completed")
            }.toString()
            
            connection.outputStream.write(jsonBody.toByteArray())
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode in 200..299
        } catch (e: Exception) { false }
    }

    suspend fun getIncomeStats(): IncomeStats = withContext(Dispatchers.IO) {
        try {
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/session_history?order=started_at.desc"))
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("apikey", apiKey)
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val now = System.currentTimeMillis()
            val oneDay = 24 * 60 * 60 * 1000L
            val oneWeek = 7 * oneDay
            val oneMonth = 30 * oneDay
            val oneYear = 365 * oneDay
            
            var daily = 0.0
            var weekly = 0.0
            var monthly = 0.0
            var yearly = 0.0
            var totalSessions = 0
            
            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                totalSessions = jsonArray.length()
                
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val amount = obj.getDouble("amount")
                    val startedAt = obj.getString("started_at")
                    try {
                        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US).parse(startedAt)
                        val time = date.time
                        if (time >= now - oneDay) daily += amount
                        if (time >= now - oneWeek) weekly += amount
                        if (time >= now - oneMonth) monthly += amount
                        if (time >= now - oneYear) yearly += amount
                    } catch (e: Exception) { }
                }
            }
            connection.disconnect()
            IncomeStats(daily, weekly, monthly, yearly, totalSessions)
        } catch (e: Exception) {
            IncomeStats(0.0, 0.0, 0.0, 0.0, 0)
        }
    }

    suspend fun getSessionHistory(): List<SessionRecord> = withContext(Dispatchers.IO) {
        try {
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/session_history?order=started_at.desc&limit=50"))
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("apikey", apiKey)
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val list = mutableListOf<SessionRecord>()
            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.US)
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US)
                
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val pin = obj.getString("pin")
                    val minutes = obj.getInt("minutes")
                    val amount = obj.getDouble("amount")
                    val startedAt = obj.getString("started_at")
                    val date = try {
                        dateFormat.format(inputFormat.parse(startedAt))
                    } catch (e: Exception) { startedAt.substring(0, 16) }
                    list.add(SessionRecord(pin, minutes, amount, date))
                }
            }
            connection.disconnect()
            list
        } catch (e: Exception) { emptyList() }
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
data class IncomeStats(val daily: Double, val weekly: Double, val monthly: Double, val yearly: Double, val totalSessions: Int)
data class SessionRecord(val pin: String, val minutes: Int, val amount: Double, val date: String)
