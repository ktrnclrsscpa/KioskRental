package com.kcb.kiosk

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAdminPassword()
    }
    
    private fun checkAdminPassword() {
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = "Enter admin password"
        
        AlertDialog.Builder(this)
            .setTitle("🔐 Admin Access")
            .setMessage("Enter password to continue")
            .setView(input)
            .setPositiveButton("Login") { _, _ ->
                val password = input.text.toString()
                val savedPassword = prefs.getString("admin_password", "admin123")
                if (password == savedPassword) {
                    startAdminPanel()
                } else {
                    Toast.makeText(this, "Wrong password!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun startAdminPanel() {
        supabase = SupabaseClient.getInstance()
        
        val scrollView = ScrollView(this)
        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 40, 30, 50)
            setBackgroundColor(Color.parseColor("#F5F7FA"))
        }
        
        // Header
        val header = TextView(this).apply {
            text = "🔐 KCB RENTAL ADMIN"
            textSize = 24f
            setTextColor(Color.parseColor("#2C3E50"))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 30)
        }
        mainContainer.addView(header)
        
        // Change Password Button
        val changePwdBtn = Button(this).apply {
            text = "Change Password"
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.parseColor("#3498DB"))
            setOnClickListener { showChangePasswordDialog() }
        }
        mainContainer.addView(changePwdBtn)
        
        // Stats Row
        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 20, 0, 20)
        }
        
        val totalIncomeText = TextView(this).apply {
            text = "Total Income: ₱0"
            textSize = 16f
            setPadding(0, 5, 0, 5)
        }
        statsRow.addView(totalIncomeText)
        
        mainContainer.addView(statsRow)
        
        // ========== GENERATE PIN SECTION ==========
        val genTitle = TextView(this).apply {
            text = "📌 Generate New PIN"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 20, 0, 10)
        }
        mainContainer.addView(genTitle)
        
        val generatePinInput = EditText(this).apply {
            hint = "PIN (leave blank for random)"
            setPadding(16, 14, 16, 14)
            background = createEditTextBackground()
        }
        mainContainer.addView(generatePinInput)
        
        val generateMinutesInput = EditText(this).apply {
            hint = "Minutes"
            setPadding(16, 14, 16, 14)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            background = createEditTextBackground()
        }
        mainContainer.addView(generateMinutesInput)
        
        val generateAmountInput = EditText(this).apply {
            hint = "Amount (₱)"
            setPadding(16, 14, 16, 14)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            background = createEditTextBackground()
        }
        mainContainer.addView(generateAmountInput)
        
        val generateBtn = Button(this).apply {
            text = "GENERATE PIN"
            setBackgroundColor(Color.parseColor("#2ECC71"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val minutes = generateMinutesInput.text.toString().toIntOrNull()
                val amount = generateAmountInput.text.toString().toDoubleOrNull()
                if (minutes == null || minutes <= 0) {
                    Toast.makeText(this@AdminActivity, "Enter valid minutes", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (amount == null || amount <= 0) {
                    Toast.makeText(this@AdminActivity, "Enter valid amount", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val customPin = generatePinInput.text.toString().trim()
                if (customPin.isNotEmpty() && customPin.length != 6) {
                    Toast.makeText(this@AdminActivity, "PIN must be 6 characters", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val pin = if (customPin.isNotEmpty()) customPin else null
                
                this.isEnabled = false
                this.text = "GENERATING..."
                
                CoroutineScope(Dispatchers.IO).launch {
                    val result = supabase.generatePin(pin, minutes * 60, amount)
                    withContext(Dispatchers.Main) {
                        this@apply.isEnabled = true
                        this@apply.text = "GENERATE PIN"
                        if (result != null) {
                            Toast.makeText(this@AdminActivity, "✅ PIN: $result ($minutes min - ₱$amount)", Toast.LENGTH_LONG).show()
                            generatePinInput.text.clear()
                            generateMinutesInput.text.clear()
                            generateAmountInput.text.clear()
                            loadStats(totalIncomeText)
                        } else {
                            Toast.makeText(this@AdminActivity, "Failed to generate PIN", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        mainContainer.addView(generateBtn)
        
        // ========== EXTEND PIN SECTION ==========
        val extendTitle = TextView(this).apply {
            text = "⏰ Extend Active PIN"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 20, 0, 10)
        }
        mainContainer.addView(extendTitle)
        
        val extendPinInput = EditText(this).apply {
            hint = "PIN to extend"
            setPadding(16, 14, 16, 14)
            background = createEditTextBackground()
        }
        mainContainer.addView(extendPinInput)
        
        val extendMinutesInput = EditText(this).apply {
            hint = "Minutes to add"
            setPadding(16, 14, 16, 14)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            background = createEditTextBackground()
        }
        mainContainer.addView(extendMinutesInput)
        
        val extendAmountInput = EditText(this).apply {
            hint = "Amount (₱)"
            setPadding(16, 14, 16, 14)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            background = createEditTextBackground()
        }
        mainContainer.addView(extendAmountInput)
        
        val extendBtn = Button(this).apply {
            text = "EXTEND TIME"
            setBackgroundColor(Color.parseColor("#E67E22"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val pin = extendPinInput.text.toString().trim()
                val minutes = extendMinutesInput.text.toString().toIntOrNull()
                val amount = extendAmountInput.text.toString().toDoubleOrNull()
                if (pin.isEmpty() || minutes == null || minutes <= 0) {
                    Toast.makeText(this@AdminActivity, "Enter PIN and valid minutes", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (amount == null || amount <= 0) {
                    Toast.makeText(this@AdminActivity, "Enter valid amount", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                this.isEnabled = false
                this.text = "EXTENDING..."
                
                CoroutineScope(Dispatchers.IO).launch {
                    val success = supabase.extendTime(pin, minutes)
                    if (success) {
                        supabase.recordExtension(pin, minutes, amount)
                        supabase.sendTelegramNotification("⏰ *Session Extended!*%0APIN: $pin%0AAdded: $minutes minutes%0APayment: ₱${String.format("%.2f", amount)}")
                        withContext(Dispatchers.Main) {
                            this@apply.isEnabled = true
                            this@apply.text = "EXTEND TIME"
                            Toast.makeText(this@AdminActivity, "Added $minutes minutes (₱$amount) to PIN $pin", Toast.LENGTH_LONG).show()
                            extendPinInput.text.clear()
                            extendMinutesInput.text.clear()
                            extendAmountInput.text.clear()
                            loadStats(totalIncomeText)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            this@apply.isEnabled = true
                            this@apply.text = "EXTEND TIME"
                            Toast.makeText(this@AdminActivity, "Failed to extend", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        mainContainer.addView(extendBtn)
        
        // ========== TELEGRAM SETTINGS SECTION ==========
        val settingsTitle = TextView(this).apply {
            text = "⚙️ Telegram Settings"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 30, 0, 10)
        }
        mainContainer.addView(settingsTitle)
        
        val telegramTokenInput = EditText(this).apply {
            hint = "Bot Token"
            setPadding(16, 14, 16, 14)
            background = createEditTextBackground()
        }
        mainContainer.addView(telegramTokenInput)
        
        val telegramChatIdInput = EditText(this).apply {
            hint = "Chat ID"
            setPadding(16, 14, 16, 14)
            background = createEditTextBackground()
        }
        mainContainer.addView(telegramChatIdInput)
        
        val telegramButtonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 10, 0, 10)
        }
        
        val saveTelegramBtn = Button(this).apply {
            text = "SAVE"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, 10, 0)
            }
            setBackgroundColor(Color.parseColor("#2ECC71"))
            setTextColor(Color.WHITE)
            setOnClickListener {
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
        }
        telegramButtonRow.addView(saveTelegramBtn)
        
        val testTelegramBtn = Button(this).apply {
            text = "TEST"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setBackgroundColor(Color.parseColor("#E67E22"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val token = telegramTokenInput.text.toString().trim()
                val chatId = telegramChatIdInput.text.toString().trim()
                if (token.isEmpty() || chatId.isEmpty()) {
                    Toast.makeText(this@AdminActivity, "Enter Bot Token and Chat ID first", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                this.isEnabled = false
                this.text = "SENDING..."
                Thread {
                    try {
                        val url = URL("https://api.telegram.org/bot$token/sendMessage?chat_id=$chatId&text=✅%20Test%20from%20KCB%20Rental!")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "GET"
                        conn.connectTimeout = 10000
                        val responseCode = conn.responseCode
                        conn.disconnect()
                        runOnUiThread {
                            this@apply.isEnabled = true
                            this@apply.text = "TEST"
                            if (responseCode == 200) {
                                Toast.makeText(this@AdminActivity, "Test sent! Check Telegram.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@AdminActivity, "Failed! Check credentials.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            this@apply.isEnabled = true
                            this@apply.text = "TEST"
                            Toast.makeText(this@AdminActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            }
        }
        telegramButtonRow.addView(testTelegramBtn)
        
        mainContainer.addView(telegramButtonRow)
        
        // ========== EXPORT REPORT BUTTON ==========
        val exportBtn = Button(this).apply {
            text = "📥 EXPORT CSV REPORT"
            setBackgroundColor(Color.parseColor("#9B59B6"))
            setTextColor(Color.WHITE)
            setPadding(16, 14, 16, 14)
            setOnClickListener { exportReport() }
        }
        mainContainer.addView(exportBtn)
        
        // Load saved Telegram settings
        CoroutineScope(Dispatchers.IO).launch {
            val (token, chatId) = supabase.getTelegramConfig()
            withContext(Dispatchers.Main) {
                telegramTokenInput.setText(token)
                telegramChatIdInput.setText(chatId)
            }
        }
        
        scrollView.addView(mainContainer)
        setContentView(scrollView)
        
        // Load stats
        loadStats(totalIncomeText)
    }
    
    private fun createEditTextBackground(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = 12f
            setStroke(1, Color.parseColor("#DDDDDD"))
        }
    }
    
    private fun loadStats(totalIncomeText: TextView) {
        CoroutineScope(Dispatchers.IO).launch {
            val stats = supabase.getIncomeStats()
            withContext(Dispatchers.Main) {
                val currency = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
                totalIncomeText.text = "Total Income: ${currency.format(stats.yearly)}"
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
