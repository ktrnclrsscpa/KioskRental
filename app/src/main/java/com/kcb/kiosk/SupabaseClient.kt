package com.kcb.kiosk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

class SupabaseClient private constructor() {

    private val supabaseUrl = "https://qbricrnjchbdyseeuwif.supabase.co"
    private val apiKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFicmljcm5qY2hiZHlzZWV1d2lmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQyMDU0NDUsImV4cCI6MjA4OTc4MTQ0NX0.5sJqi3fZc4VIFQAIw1QptHt7MlGdnkn5SVxYdRu4f7Q"

    private val characters = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789!@#$%&*"
    
    private fun generateRandomPin(): String {
        return (1..6).map { characters.random() }.joinToString("")
    }

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
        } catch (e: Exception) {
            false
        }
    }

    suspend fun validatePin(pin: String): PinValidationResult = withContext(Dispatchers.IO) {
        try {
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/credits?pin=eq.$pin"))
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
                    val jsonObject = jsonArray.getJSONObject(0)
                    val seconds = jsonObject.getInt("seconds_left")
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
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val jsonBody = JSONObject().apply {
                put("pin", pin)
                put("seconds_left", seconds)
            }.toString()
            
            connection.outputStream.write(jsonBody.toByteArray())
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            return@withContext if (responseCode in 200..299) pin else null
        } catch (e: Exception) {
            return@withContext null
        }
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
                    list.add(PinData(
                        pin = obj.getString("pin"),
                        secondsLeft = obj.getInt("seconds_left")
                    ))
                }
                return@withContext list
            } else {
                return@withContext emptyList()
            }
        } catch (e: Exception) {
            return@withContext emptyList()
        }
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
        } catch (e: Exception) {
            return@withContext emptyList()
        }
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
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val jsonBody = JSONObject().apply {
                put("setting_value", appsString)
                put("updated_at", System.currentTimeMillis())
            }.toString()
            
            connection.outputStream.write(jsonBody.toByteArray())
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            responseCode in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deletePin(pin: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/credits?pin=eq.$pin"))
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "DELETE"
            connection.setRequestProperty("apikey", apiKey)
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }

    suspend fun extendTime(pin: String, extraMinutes: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            // First get current seconds left
            val getUrl = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/credits?pin=eq.$pin"))
            val getConnection = getUrl.openConnection() as HttpURLConnection
            getConnection.requestMethod = "GET"
            getConnection.setRequestProperty("apikey", apiKey)
            getConnection.setRequestProperty("Authorization", "Bearer $apiKey")
            getConnection.connectTimeout = 5000
            getConnection.readTimeout = 5000
            
            val currentSeconds = if (getConnection.responseCode == 200) {
                val responseText = getConnection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                if (jsonArray.length() > 0) {
                    jsonArray.getJSONObject(0).getInt("seconds_left")
                } else {
                    return@withContext false
                }
            } else {
                return@withContext false
            }
            getConnection.disconnect()
            
            // Update with new seconds
            val newSeconds = currentSeconds + (extraMinutes * 60)
            val updateUrl = URL(addApiKeyToUrl("$supabaseUrl/rest/v1/credits?pin=eq.$pin"))
            val updateConnection = updateUrl.openConnection() as HttpURLConnection
            updateConnection.requestMethod = "PATCH"
            updateConnection.setRequestProperty("apikey", apiKey)
            updateConnection.setRequestProperty("Authorization", "Bearer $apiKey")
            updateConnection.setRequestProperty("Content-Type", "application/json")
            updateConnection.doOutput = true
            updateConnection.connectTimeout = 5000
            updateConnection.readTimeout = 5000
            
            val jsonBody = "{\"seconds_left\":$newSeconds}"
            updateConnection.outputStream.write(jsonBody.toByteArray())
            
            val responseCode = updateConnection.responseCode
            updateConnection.disconnect()
            
            responseCode in 200..299
        } catch (e: Exception) {
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
