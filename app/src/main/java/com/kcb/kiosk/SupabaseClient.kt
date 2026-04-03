package com.kcb.kiosk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SupabaseClient private constructor() {

    private val supabaseUrl = "https://qbricrnjchbdyseeuwif.supabase.co"
    private val apiKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFicmljcm5qY2hiZHlzZWV1d2lmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQyMDU0NDUsImV4cCI6MjA4OTc4MTQ0NX0.5sJqi3fZc4VIFQAIw1QptHt7MlGdnkn5SVxYdRu4f7Q"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun addApiKeyToUrl(urlString: String): String {
        val separator = if (urlString.contains("?")) "&" else "?"
        return "$urlString${separator}apikey=$apiKey"
    }

    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(addApiKeyToUrl("$supabaseUrl/rest/v1/admin_settings?limit=1"))
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun validatePin(pin: String): PinValidationResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(addApiKeyToUrl("$supabaseUrl/rest/v1/credits?pin=eq.$pin"))
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseText = response.body?.string() ?: ""
                val jsonArray = JSONArray(responseText)
                if (jsonArray.length() > 0) {
                    val jsonObject = jsonArray.getJSONObject(0)
                    val seconds = jsonObject.getInt("seconds_left")
                    PinValidationResult(true, seconds, null)
                } else {
                    PinValidationResult(false, 0, "PIN not found")
                }
            } else {
                PinValidationResult(false, 0, "HTTP error: ${response.code}")
            }
        } catch (e: Exception) {
            PinValidationResult(false, 0, e.message ?: "Network error")
        }
    }

    suspend fun generatePin(customPin: String?, seconds: Int): String? = withContext(Dispatchers.IO) {
        try {
            val pin = customPin ?: (100000..999999).random().toString()
            
            val jsonBody = JSONObject().apply {
                put("pin", pin)
                put("seconds_left", seconds)
            }.toString()
            
            val request = Request.Builder()
                .url(addApiKeyToUrl("$supabaseUrl/rest/v1/credits"))
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) pin else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getActivePins(): List<PinData> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(addApiKeyToUrl("$supabaseUrl/rest/v1/credits?seconds_left=gt.0&order=created_at.desc"))
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseText = response.body?.string() ?: ""
                val jsonArray = JSONArray(responseText)
                val list = mutableListOf<PinData>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(PinData(
                        pin = obj.getString("pin"),
                        secondsLeft = obj.getInt("seconds_left")
                    ))
                }
                list
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getWhitelistApps(): List<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(addApiKeyToUrl("$supabaseUrl/rest/v1/admin_settings?setting_key=eq.whitelist_apps"))
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseText = response.body?.string() ?: ""
                val jsonArray = JSONArray(responseText)
                if (jsonArray.length() > 0) {
                    val value = jsonArray.getJSONObject(0).getString("setting_value")
                    value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                } else {
                    emptyList()
                }
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
            
            val jsonBody = JSONObject().apply {
                put("setting_key", "whitelist_apps")
                put("setting_value", appsString)
                put("updated_at", System.currentTimeMillis())
            }.toString()
            
            val request = Request.Builder()
                .url(addApiKeyToUrl("$supabaseUrl/rest/v1/admin_settings"))
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            response.isSuccessful
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
