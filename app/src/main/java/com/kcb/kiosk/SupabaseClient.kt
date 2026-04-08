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
    private val url = "https://qbricrnjchbdyseeuwif.supabase.co"
    private val key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFicmljcm5qY2hiZHlzZWV1d2lmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQyMDU0NDUsImV4cCI6MjA4OTc4MTQ0NX0.5sJqi3fZc4VIFQAIw1QptHt7MlGdnkn5SVxYdRu4f7Q"

    private fun sign(u: String) = "$u${if (u.contains("?")) "&" else "?"}apikey=$key"

    suspend fun validatePin(p: String): MainActivity.PinRes = withContext(Dispatchers.IO) {
        try {
            val conn = URL(sign("$url/rest/v1/credits?pin=eq.$p")).openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $key")
            val res = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(res)
            if (arr.length() > 0) MainActivity.PinRes(true, arr.getJSONObject(0).getInt("seconds_left"))
            else MainActivity.PinRes(false, 0)
        } catch (e: Exception) { MainActivity.PinRes(false, 0) }
    }

    suspend fun updatePinSeconds(p: String, s: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL(sign("$url/rest/v1/credits?pin=eq.$p")).openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.setRequestProperty("Authorization", "Bearer $key")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.outputStream.write("{\"seconds_left\":$s}".toByteArray())
            conn.responseCode in 200..299
        } catch (e: Exception) { false }
    }

    suspend fun getActivePins(): List<PinData> = withContext(Dispatchers.IO) {
        try {
            val conn = URL(sign("$url/rest/v1/credits?seconds_left=gt.0")).openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $key")
            val arr = JSONArray(conn.inputStream.bufferedReader().use { it.readText() })
            List(arr.length()) { i -> PinData(arr.getJSONObject(i).getString("pin"), arr.getJSONObject(i).getInt("seconds_left")) }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun recordExtension(p: String, m: Int, a: Double) = withContext(Dispatchers.IO) {
        try {
            val conn = URL(sign("$url/rest/v1/session_history")).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $key")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            val body = JSONObject().apply { put("pin", p); put("minutes", m); put("amount", a); put("status", "extension") }
            conn.outputStream.write(body.toString().toByteArray())
            conn.responseCode
        } catch (e: Exception) { }
    }

    companion object {
        private var i: SupabaseClient? = null
        fun getInstance(): SupabaseClient { if (i == null) i = SupabaseClient(); return i!! }
    }
}