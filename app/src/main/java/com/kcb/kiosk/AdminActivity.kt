package com.kcb.kiosk

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
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
    
    private lateinit var totalIncomeText: TextView
    private lateinit var totalSessionsText: TextView
    private lateinit var todayIncomeText: TextView
    private lateinit var pinsText: TextView
    private lateinit var appContainer: LinearLayout
    private lateinit var appStatusText: TextView
    private lateinit var saveAppsBtn: Button
    private val checkBoxes = mutableListOf<Pair<CheckBox, String>>()

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
                    showAdminPanel()
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
    
    private fun showAdminPanel() {
        supabase = SupabaseClient.getInstance()
        
        // Outer ScrollView for the whole page
        val outerScroll = ScrollView(this)
        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 40, 30, 60)
            setBackgroundColor(Color.parseColor("#F5F7FA"))
        }
        
        // Header
        val header = TextView(this).apply {
            text = "🔐 KCB RENTAL ADMIN"
            textSize = 24f
            setTextColor(Color.parseColor("#2C3E50"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }
        mainContainer.addView(header)
        
        val changePwdBtn = Button(this).apply {
            text = "Change Password"
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.parseColor("#3498DB"))
            setOnClickListener { showChangePasswordDialog() }
        }
        mainContainer.addView(changePwdBtn)
        
        // Income Summary
        val statsTitle = TextView(this).apply {
            text = "💰 INCOME SUMMARY"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 20, 0, 10)
        }
        mainContainer.addView(statsTitle)
        
        totalIncomeText = createCardText("Total Income: ₱0")
        mainContainer.addView(totalIncomeText)
        
        totalSessionsText = createCardText("Total Sessions: 0")
        mainContainer.addView(totalSessionsText)
        
        todayIncomeText = createCardText("Today's Sales: ₱0")
        mainContainer.addView(todayIncomeText)
        
        val refreshStatsBtn = Button(this).apply {
            text = "REFRESH STATS"
            setBackgroundColor(Color.parseColor("#3498DB"))
            setTextColor(Color.WHITE)
            setOnClickListener { loadStats() }
        }
        mainContainer.addView(refreshStatsBtn)
        
        // Generate PIN
        val genTitle = TextView(this).apply {
            text = "📌 Generate New PIN"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 20, 0, 10)
        }
        mainContainer.addView(genTitle)
        
        val generatePinInput = createEditText("PIN (leave blank for random)")
        mainContainer.addView(generatePinInput)
        
        val generateMinutesInput = createEditText("Minutes", true)
        mainContainer.addView(generateMinutesInput)
        
        val generateAmountInput = createEditText("Amount (₱)", true)
        generateAmountInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        mainContainer.addView(generateAmountInput)
        
        val generateBtn = createButton("GENERATE PIN", "#2ECC71")
        generateBtn.setOnClickListener {
            val minutes = generateMinutesInput.text.toString().toIntOrNull()
            val amount = generateAmountInput.text.toString().toDoubleOrNull()
            if (minutes == null || minutes <= 0) {
                Toast.makeText(this, "Enter valid minutes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Enter valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val customPin = generatePinInput.text.toString().trim()
            if (customPin.isNotEmpty() && customPin.length != 6) {
                Toast.makeText(this, "PIN must be 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
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
        mainContainer.addView(generateBtn)
        
        // Extend PIN
        val extendTitle = TextView(this).apply {
            text = "⏰ Extend Active PIN"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 20, 0, 10)
        }
        mainContainer.addView(extendTitle)
        
        val extendPinInput = createEditText("PIN to extend")
        mainContainer.addView(extendPinInput)
        
        val extendMinutesInput = createEditText("Minutes to add", true)
        mainContainer.addView(extendMinutesInput)
        
        val extendAmountInput = createEditText("Amount (₱)", true)
        extendAmountInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        mainContainer.addView(extendAmountInput)
        
        val extendBtn = createButton("EXTEND TIME", "#E67E22")
        extendBtn.setOnClickListener {
            val pin = extendPinInput.text.toString().trim()
            val minutes = extendMinutesInput.text.toString().toIntOrNull()
            val amount = extendAmountInput.text.toString().toDoubleOrNull()
            if (pin.isEmpty() || minutes == null || minutes <= 0) {
                Toast.makeText(this, "Enter PIN and valid minutes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Enter valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
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
        mainContainer.addView(extendBtn)
        
        // Active PINs
        val pinsTitle = TextView(this).apply {
            text = "🔑 Active PINs"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 20, 0, 10)
        }
        mainContainer.addView(pinsTitle)
        
        pinsText = createCardText("Loading...")
        mainContainer.addView(pinsText)
        
        val refreshPinsBtn = Button(this).apply {
            text = "REFRESH PIN LIST"
            setBackgroundColor(Color.parseColor("#3498DB"))
            setTextColor(Color.WHITE)
            setOnClickListener { loadActivePins() }
        }
        mainContainer.addView(refreshPinsBtn)
        
        // App Whitelist - with its own ScrollView for scrolling
        val whitelistTitle = TextView(this).apply {
            text = "📱 App Whitelist"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 20, 0, 10)
        }
        mainContainer.addView(whitelistTitle)
        
        appStatusText = TextView(this).apply {
            text = "Loading apps..."
            textSize = 13f
            setPadding(0, 0, 0, 12)
            setTextColor(Color.parseColor("#7F8C8D"))
        }
        mainContainer.addView(appStatusText)
        
        // Fixed height ScrollView for app list
        val appScrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400
            )
        }
        appContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
        }
        appScrollView.addView(appContainer)
        mainContainer.addView(appScrollView)
        
        saveAppsBtn = createButton("SAVE WHITELIST", "#3498DB")
        saveAppsBtn.setOnClickListener { saveWhitelistLocal() }
        mainContainer.addView(saveAppsBtn)
        
        // Telegram Settings
        val settingsTitle = TextView(this).apply {
            text = "⚙️ Telegram Settings"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 20, 0, 10)
        }
        mainContainer.addView(settingsTitle)
        
        val telegramTokenInput = createEditText("Bot Token")
        mainContainer.addView(telegramTokenInput)
        
        val telegramChatIdInput = createEditText("Chat ID")
        mainContainer.addView(telegramChatIdInput)
        
        val buttonRow = LinearLayout(this).apply {
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
                        Toast.makeText(this@AdminActivity, if (success) "Telegram saved!" else "Failed to save", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        buttonRow.addView(saveTelegramBtn)
        
        val testTelegramBtn = createButton("TEST", "#E67E22")
        testTelegramBtn.setOnClickListener {
            val token = telegramTokenInput.text.toString().trim()
            val chatId = telegramChatIdInput.text.toString().trim()
            if (token.isEmpty() || chatId.isEmpty()) {
                Toast.makeText(this, "Enter Bot Token and Chat ID first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
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
        buttonRow.addView(testTelegramBtn)
        mainContainer.addView(buttonRow)
        
        // Spacer before export button
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                20
            )
        }
        mainContainer.addView(spacer)
        
        val exportBtn = createButton("📥 EXPORT CSV REPORT", "#9B59B6")
        exportBtn.setOnClickListener { exportReport() }
        mainContainer.addView(exportBtn)
        
        outerScroll.addView(mainContainer)
        setContentView(outerScroll)
        
        // Load data
        loadStats()
        loadActivePins()
        loadTelegramSettings(telegramTokenInput, telegramChatIdInput)
        loadInstalledApps()
        loadCurrentWhitelistLocal()
    }
    
    private fun createCardText(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 16f
            setPadding(16, 12, 16, 12)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 12f
                setStroke(1, Color.parseColor("#EEEEEE"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 8)
            }
        }
    }
    
    private fun createEditText(hint: String, isNumber: Boolean = false): EditText {
        return EditText(this).apply {
            this.hint = hint
            setPadding(16, 14, 16, 14)
            textSize = 14f
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 12f
                setStroke(1, Color.parseColor("#DDDDDD"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 12)
            }
            if (isNumber) {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }
        }
    }
    
    private fun createButton(text: String, color: String): Button {
        return Button(this).apply {
            this.text = text
            textSize = 14f
            setPadding(18, 14, 18, 14)
            setBackgroundColor(Color.parseColor(color))
            setTextColor(Color.WHITE)
            setAllCaps(false)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 5, 0, 5)
            }
        }
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
    
    private fun loadTelegramSettings(tokenInput: EditText, chatIdInput: EditText) {
        CoroutineScope(Dispatchers.IO).launch {
            val (token, chatId) = supabase.getTelegramConfig()
            withContext(Dispatchers.Main) {
                tokenInput.setText(token)
                chatIdInput.setText(chatId)
            }
        }
    }
    
    private fun loadInstalledApps() {
        appStatusText.text = "Loading apps..."
        saveAppsBtn.isEnabled = false
        val installedApps = mutableListOf<Pair<String, String>>()
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in packages) {
            val isSystemApp = (app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystemApp = (app.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            if (!isSystemApp || isUpdatedSystemApp) {
                val appName = packageManager.getApplicationLabel(app).toString()
                installedApps.add(Pair(appName, app.packageName))
            }
        }
        installedApps.sortBy { it.first }
        appContainer.removeAllViews()
        checkBoxes.clear()
        for (app in installedApps) {
            val checkBox = CheckBox(this).apply {
                text = "${app.first}\n${app.second}"
                setPadding(12, 10, 12, 10)
                textSize = 13f
            }
            appContainer.addView(checkBox)
            checkBoxes.add(Pair(checkBox, app.second))
        }
        appStatusText.text = "Found ${installedApps.size} apps. Select allowed apps."
        saveAppsBtn.isEnabled = true
    }
    
    private fun loadCurrentWhitelistLocal() {
        val savedWhitelist = prefs.getStringSet("whitelist", emptySet()) ?: emptySet()
        for ((checkBox, packageName) in checkBoxes) {
            checkBox.isChecked = savedWhitelist.contains(packageName)
        }
    }
    
    private fun saveWhitelistLocal() {
        saveAppsBtn.isEnabled = false
        saveAppsBtn.text = "SAVING..."
        val selectedPackages = mutableSetOf<String>()
        for ((checkBox, packageName) in checkBoxes) {
            if (checkBox.isChecked) selectedPackages.add(packageName)
        }
        prefs.edit().putStringSet("whitelist", selectedPackages).apply()
        saveAppsBtn.isEnabled = true
        saveAppsBtn.text = "SAVE WHITELIST"
        appStatusText.text = "✓ Saved ${selectedPackages.size} apps"
        Toast.makeText(this, "Saved ${selectedPackages.size} apps", Toast.LENGTH_LONG).show()
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
