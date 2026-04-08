package com.kcb.kiosk

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class AdminActivity : AppCompatActivity() {
    private lateinit var supabase: SupabaseClient
    private val botToken = "8754642119:AAEueRR7PuzTcKAkfQ8b2sfMK_HeJ_WDrpU"
    private val chatId = "579327360"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supabase = SupabaseClient.getInstance()
        setupUI()
    }

    private fun setupUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            setBackgroundColor(android.graphics.Color.WHITE)
        }

        val etPin = EditText(this).apply { hint = "PIN ng Customer" }
        val etAmt = EditText(this).apply { hint = "Magkano (₱)"; inputType = 2 }
        val etMins = EditText(this).apply { hint = "Ilang Minutes"; inputType = 2 }

        val btnExtend = Button(this).apply {
            text = "EXTEND TIME"
            setOnClickListener {
                val pin = etPin.text.toString()
                val mins = etMins.text.toString().toLongOrNull() ?: 0
                val amt = etAmt.text.toString().toDoubleOrNull() ?: 0.0
                if (pin.isNotEmpty() && mins > 0) processExtension(pin, mins, amt)
            }
        }

        root.addView(etPin); root.addView(etAmt); root.addView(etMins); root.addView(btnExtend)
        setContentView(root)
    }

    private fun processExtension(pin: String, minsToAdd: Long, amt: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            val currentSecs = supabase.getCurrentRemainingSeconds(pin)
            val newTotal = currentSecs + (minsToAdd * 60)
            val success = supabase.updatePinTime(pin, newTotal, amt, true)
            
            withContext(Dispatchers.Main) {
                if (success) {
                    sendTelegramAlert(pin, minsToAdd, amt, newTotal)
                    Toast.makeText(this@AdminActivity, "Success! New: ${newTotal/60}m", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendTelegramAlert(pin: String, added: Long, amt: Double, total: Long) {
        val msg = "💰 SALE: PIN $pin | Added $added mins | Total ₱$amt"
        CoroutineScope(Dispatchers.IO).launch {
            try {
                java.net.URL("https://api.telegram.org/bot$botToken/sendMessage?chat_id=$chatId&text=${java.net.URLEncoder.encode(msg, "UTF-8")}").readText()
            } catch (e: Exception) {}
        }
    }
}