package com.kcb.kiosk

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.*

class AdminActivity : AppCompatActivity() {
    
    private lateinit var supabase: SupabaseClient
    private val prefs by lazy { getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE) }
    
    private lateinit var totalIncomeText: TextView
    private lateinit var totalSessionsText: TextView
    private lateinit var todayIncomeText: TextView
    private lateinit var generatePinInput: EditText
    private lateinit var generateMinutesInput: EditText
    private lateinit var generateAmountInput: EditText
    private lateinit var generateBtn: Button
    private lateinit var extendPinInput: EditText
    private lateinit var extendMinutesInput: EditText
    private lateinit var extendAmountInput: EditText
    private lateinit var extendBtn: Button
    private lateinit var pinsText: TextView
    private lateinit var telegramTokenInput: EditText
    private lateinit var telegramChatIdInput: EditText
    private lateinit var saveTelegramBtn: Button
    private lateinit var testTelegramBtn: Button
    private lateinit var exportReportBtn: Button
    private lateinit var refreshStatsBtn: Button
    private lateinit var refreshPinsBtn: Button
    private lateinit var changePwdBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        
        supabase = SupabaseClient.getInstance()
        
        // Initialize views
        totalIncomeText = findViewById(R.id.totalIncomeText)
        totalSessionsText = findViewById(R.id.totalSessionsText)
        todayIncomeText = findViewById(R.id.todayIncomeText)
        generatePinInput = findViewById(R.id.generatePinInput)
        generateMinutesInput = findViewById(R.id.generateMinutesInput)
        generateAmountInput = findViewById(R.id.generateAmountInput)
        generateBtn = findViewById(R.id.generateBtn)
        extendPinInput = findViewById(R.id.extendPinInput)
        extendMinutesInput = findViewById(R.id.extendMinutesInput)
        extendAmountInput = findViewById(R.id.extendAmountInput)
        extendBtn = findViewById(R.id.extendBtn)
        pinsText = findViewById(R.id.pinsText)
        telegramTokenInput = findViewById(R.id.telegramTokenInput)
        telegramChatIdInput = findViewById(R.id.telegramChatIdInput)
        saveTelegramBtn = findViewById(R.id.saveTelegramBtn)
        testTelegramBtn = findViewById(R.id.testTelegramBtn)
        exportReportBtn = findViewById(R.id.exportReportBtn)
        refreshStatsBtn = findViewById(R.id.refreshStatsBtn)
        refreshPinsBtn = findViewById(R.id.refreshPinsBtn)
        changePwdBtn = findViewById(R.id.changePwdBtn)
        
        // Set click listeners
        generateBtn.setOnClickListener { generatePin() }
        extendBtn.setOnClickListener { extendPin() }
        refreshStatsBtn.setOnClickListener { loadStats() }
        refreshPinsBtn.setOnClickListener { loadActivePins() }
        saveTelegramBtn.setOnClickListener { saveTelegramConfig() }
        testTelegramBtn.setOnClickListener { testTelegram() }
        exportReportBtn.setOnClickListener { exportReport() }
        changePwdBtn.setOnClickListener { showChangePasswordDialog() }
        
        // Load data
        loadStats()
        loadActivePins()
        loadTelegramSettings()
    }
    
    private fun loadStats() {
        CoroutineScope(Dispatchers.IO).launch {
            val stats = supabase.getIncomeStats()
            withContext(Dispatchers.Main) {
                val currency = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
                totalIncomeText.text = "Total Income: ${currency.format(stats.yearly)}"
                totalSessionsText.text = "Total Sessions: ${stats.totalSessions}"
                todayIncomeText.text = "Today's Sales: ${currency.format(stats.daily)}"
            }
        }
    }
    
    private fun loadActivePins() {
        CoroutineScope(Dispatchers.IO).launch {
            val pins = supabase.getActivePins()
            withContext(Dispatchers.Main) {
                if (pins.isEmpty()) {
                    pinsText.text = "No active PINs"
                } else {
                    val sb = StringBuilder()
                    for (p in pins) {
                        sb.append("🔑 ${p.pin} - ${p.secondsLeft / 60} min remaining\n")
                    }
                    pinsText.text = sb.toString()
                }
            }
        }
    }
    
    private fun loadTelegramSettings() {
        CoroutineScope(Dispatchers.IO).launch {
            val (token, chatId) = supabase.getTelegramConfig()
            withContext(Dispatchers.Main) {
                telegramTokenInput.setText(token)
                telegramChatIdInput.setText(chatId)
            }
        }
    }
    
    private fun saveTelegramConfig() {
        val token = telegramTokenInput.text.toString().trim()
        val chatId = telegramChatIdInput.text.toString().trim()
        CoroutineScope(Dispatchers.IO).launch {
            val success = supabase.updateTelegramConfig(token, chatId)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(this@AdminActivity, "Telegram saved!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AdminActivity, "Failed to save", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun testTelegram() {
        val token = telegramTokenInput.text.toString().trim()
        val chatId = telegramChatIdInput.text.toString().trim()
        if (token.isEmpty() || chatId.isEmpty()) {
            Toast.makeText(this, "Enter Bot Token and Chat ID first", Toast.LENGTH_SHORT).show()
            return
        }
        testTelegramBtn.isEnabled = false
        testTelegramBtn.text = "SENDING..."
        Thread {
            try {
                val url = URL("https://api.telegram.org/bot$token/sendMessage?chat_id=$chatId&text=✅%20Test%20from%20KCB%20Rental!")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                val responseCode = conn.responseCode
                conn.disconnect()
                runOnUiThread {
                    testTelegramBtn.isEnabled = true
                    testTelegramBtn.text = "TEST"
                    if (responseCode == 200) {
                        Toast.makeText(this, "Test sent! Check Telegram.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed! Check credentials.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    testTelegramBtn.isEnabled = true
                    testTelegramBtn.text = "TEST"
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
    
    private fun generatePin() {
        val minutes = generateMinutesInput.text.toString().toIntOrNull()
        val amount = generateAmountInput.text.toString().toDoubleOrNull()
        if (minutes == null || minutes <= 0) {
            Toast.makeText(this, "Enter valid minutes", Toast.LENGTH_SHORT).show()
            return
        }
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Enter valid amount", Toast.LENGTH_SHORT).show()
            return
        }
        val customPin = generatePinInput.text.toString().trim()
        if (customPin.isNotEmpty() && customPin.length != 6) {
            Toast.makeText(this, "PIN must be 6 characters", Toast.LENGTH_SHORT).show()
            return
        }
        val pin = if (customPin.isNotEmpty()) customPin else null
        generateBtn.isEnabled = false
        generateBtn.text = "GENERATING..."
        CoroutineScope(Dispatchers.IO).launch {
            val result = supabase.generatePin(pin, minutes * 60, amount)
            withContext(Dispatchers.Main) {
                generateBtn.isEnabled = true
                generateBtn.text = "GENERATE PIN"
                if (result != null) {
                    Toast.makeText(this@AdminActivity, "✅ PIN: $result ($minutes min - ₱$amount)", Toast.LENGTH_LONG).show()
                    generatePinInput.text.clear()
                    generateMinutesInput.text.clear()
                    generateAmountInput.text.clear()
                    loadStats()
                    loadActivePins()
                } else {
                    Toast.makeText(this@AdminActivity, "Failed to generate PIN", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun extendPin() {
        val pin = extendPinInput.text.toString().trim()
        val minutes = extendMinutesInput.text.toString().toIntOrNull()
        val amount = extendAmountInput.text.toString().toDoubleOrNull()
        if (pin.isEmpty() || minutes == null || minutes <= 0) {
            Toast.makeText(this, "Enter PIN and valid minutes", Toast.LENGTH_SHORT).show()
            return
        }
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Enter valid amount", Toast.LENGTH_SHORT).show()
            return
        }
        extendBtn.isEnabled = false
        extendBtn.text = "EXTENDING..."
        CoroutineScope(Dispatchers.IO).launch {
            val success = supabase.extendTime(pin, minutes)
            if (success) {
                supabase.recordExtension(pin, minutes, amount)
                supabase.sendTelegramNotification("⏰ *Session Extended!*%0APIN: $pin%0AAdded: $minutes minutes%0APayment: ₱${String.format("%.2f", amount)}")
                withContext(Dispatchers.Main) {
                    extendBtn.isEnabled = true
                    extendBtn.text = "EXTEND TIME"
                    Toast.makeText(this@AdminActivity, "Added $minutes minutes (₱$amount) to PIN $pin", Toast.LENGTH_LONG).show()
                    extendPinInput.text.clear()
                    extendMinutesInput.text.clear()
                    extendAmountInput.text.clear()
                    loadStats()
                    loadActivePins()
                }
            } else {
                withContext(Dispatchers.Main) {
                    extendBtn.isEnabled = true
                    extendBtn.text = "EXTEND TIME"
                    Toast.makeText(this@AdminActivity, "Failed to extend", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun exportReport() {
        CoroutineScope(Dispatchers.IO).launch {
            val history = supabase.getSessionHistory()
            val csv = StringBuilder("PIN,Minutes,Amount,Date,Type\n")
            for (record in history) {
                val type = if (record.pin.endsWith("_EXT")) "EXTENSION" else "SESSION"
                val cleanPin = record.pin.replace("_EXT", "")
                csv.append("$cleanPin,${record.minutes},${record.amount},${record.date},$type\n")
            }
            withContext(Dispatchers.Main) {
                try {
                    val file = File(cacheDir, "sales_report.csv")
                    file.writeText(csv.toString())
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        this@AdminActivity,
                        "${packageName}.fileprovider",
                        file
                    )
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(android.content.Intent.createChooser(intent, "Export Report"))
                } catch (e: Exception) {
                    Toast.makeText(this@AdminActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun showChangePasswordDialog() {
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = "New password (min 4 characters)"
        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setMessage("Enter new password")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newPassword = input.text.toString()
                if (newPassword.length >= 4) {
                    prefs.edit().putString("admin_password", newPassword).apply()
                    Toast.makeText(this, "Password changed!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
