package com.kcb.kiosk

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class AdminActivity : AppCompatActivity() {
    private lateinit var supabase: SupabaseClient
    private val botToken = "YOUR_TELEGRAM_BOT_TOKEN"
    private val chatId = "YOUR_CHAT_ID"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supabase = SupabaseClient.getInstance()
        setupUI()
    }

    private fun setupUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
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
            
            if (success) {
                sendTelegramAlert(pin, minsToAdd, amt, newTotal)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AdminActivity, "Success! Bagong oras: ${newTotal/60}m", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendTelegramAlert(pin: String, added: Long, amt: Double, total: Long) {
        val msg = "💰 SALE: PIN $pin | Added $added mins | Total ₱$amt"
        CoroutineScope(Dispatchers.IO).launch {
            try {
                java.net.URL("https://api.telegram.org/bot$botToken/sendMessage?chat_id=$chatId&text=$msg").readText()
            } catch (e: Exception) {}
        }
    }
}