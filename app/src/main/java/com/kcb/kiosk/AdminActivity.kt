package com.kcb.kiosk

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.*

class AdminActivity : AppCompatActivity() {
    private lateinit var supabase: SupabaseClient
    
    private lateinit var dashboardPanel: LinearLayout
    private lateinit var pinPanel: LinearLayout
    private lateinit var appPanel: LinearLayout
    private lateinit var settingsPanel: LinearLayout
    
    private lateinit var pinListText: TextView
    private lateinit var generatePinInput: EditText
    private lateinit var generateMinutesInput: EditText
    private lateinit var generateAmountInput: EditText
    private lateinit var generateBtn: Button
    private lateinit var refreshPinsBtn: Button
    private lateinit var extendPinInput: EditText
    private lateinit var extendMinutesInput: EditText
    private lateinit var extendAmountInput: EditText
    private lateinit var extendBtn: Button
    
    private lateinit var appContainer: LinearLayout
    private lateinit var saveAppsBtn: Button
    private lateinit var appStatusText: TextView
    private val checkBoxes = mutableListOf<Pair<CheckBox, String>>()
    
    private lateinit var dailyIncomeText: TextView
    private lateinit var weeklyIncomeText: TextView
    private lateinit var monthlyIncomeText: TextView
    private lateinit var yearlyIncomeText: TextView
    private lateinit var totalSessionsText: TextView
    private lateinit var sessionHistoryRecycler: RecyclerView
    
    private lateinit var telegramTokenInput: EditText
    private lateinit var telegramChatIdInput: EditText
    private lateinit var saveTelegramBtn: Button
    private lateinit var testTelegramBtn: Button
    private lateinit var exportReportBtn: Button
    
    private val prefs by lazy { getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showPasswordDialog()
    }
    
    private fun showPasswordDialog() {
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
                    initAdminPanel()
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
    
    private fun initAdminPanel() {
        supabase = SupabaseClient.getInstance()
        
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 50, 30, 30)
        }
        
        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 20)
        }
        
        val title = TextView(this).apply {
            text = "🔐 ADMIN PANEL"
            textSize = 24f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleRow.addView(title)
        
        val changePwdBtn = Button(this).apply {
            text = "Change Password"
            textSize = 12f
            setOnClickListener { showChangePasswordDialog() }
        }
        titleRow.addView(changePwdBtn)
        
        mainLayout.addView(titleRow)
        
        val tabLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 20)
        }
        
        val dashboardTab = Button(this).apply {
            text = "📊 DASHBOARD"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { showDashboardPanel() }
        }
        tabLayout.addView(dashboardTab)
        
        val pinsTab = Button(this).apply {
            text = "📋 PINS"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { showPinPanel() }
        }
        tabLayout.addView(pinsTab)
        
        val appsTab = Button(this).apply {
            text = "📱 APPS"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { showAppPanel() }
        }
        tabLayout.addView(appsTab)
        
        val settingsTab = Button(this).apply {
            text = "⚙️ SETTINGS"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { showSettingsPanel() }
        }
        tabLayout.addView(settingsTab)
        
        mainLayout.addView(tabLayout)
        
        // ========== DASHBOARD PANEL ==========
        dashboardPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.VISIBLE
        }
        
        val incomeTitle = TextView(this).apply {
            text = "💰 INCOME SUMMARY"
            textSize = 18f
            setPadding(0, 20, 0, 10)
        }
        dashboardPanel.addView(incomeTitle)
        
        dailyIncomeText = TextView(this).apply {
            text = "Today: ₱0.00"
            textSize = 16f
            setPadding(0, 5, 0, 5)
        }
        dashboardPanel.addView(dailyIncomeText)
        
        weeklyIncomeText = TextView(this).apply {
            text = "This Week: ₱0.00"
            textSize = 16f
            setPadding(0, 5, 0, 5)
        }
        dashboardPanel.addView(weeklyIncomeText)
        
        monthlyIncomeText = TextView(this).apply {
            text = "This Month: ₱0.00"
            textSize = 16f
            setPadding(0, 5, 0, 5)
        }
        dashboardPanel.addView(monthlyIncomeText)
        
        yearlyIncomeText = TextView(this).apply {
            text = "This Year: ₱0.00"
            textSize = 16f
            setPadding(0, 5, 0, 5)
        }
        dashboardPanel.addView(yearlyIncomeText)
        
        totalSessionsText = TextView(this).apply {
            text = "Total Sessions: 0"
            textSize = 16f
            setPadding(0, 5, 0, 5)
        }
        dashboardPanel.addView(totalSessionsText)
        
        val historyTitle = TextView(this).apply {
            text = "📜 SESSION HISTORY"
            textSize = 18f
            setPadding(0, 20, 0, 10)
        }
        dashboardPanel.addView(historyTitle)
        
        sessionHistoryRecycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@AdminActivity)
            setPadding(0, 0, 0, 20)
        }
        dashboardPanel.addView(sessionHistoryRecycler)
        
        val refreshStatsBtn = Button(this).apply {
            text = "🔄 REFRESH STATS"
            setOnClickListener { loadDashboardStats() }
        }
        dashboardPanel.addView(refreshStatsBtn)
        
        mainLayout.addView(dashboardPanel)
        
        // ========== PINS PANEL ==========
        pinPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        
        val genLabel = TextView(this).apply {
            text = "Generate New PIN"
            textSize = 18f
            setPadding(0, 20, 0, 10)
        }
        pinPanel.addView(genLabel)
        
        generatePinInput = EditText(this).apply {
            hint = "PIN (leave blank for random)"
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setPadding(10, 10, 10, 10)
            maxLines = 1
        }
        pinPanel.addView(generatePinInput)
        
        generateMinutesInput = EditText(this).apply {
            hint = "Minutes (e.g., 60)"
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setPadding(10, 10, 10, 10)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        pinPanel.addView(generateMinutesInput)
        
        generateAmountInput = EditText(this).apply {
            hint = "Amount in ₱ (e.g., 15.00)"
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setPadding(10, 10, 10, 10)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        pinPanel.addView(generateAmountInput)
        
        generateBtn = Button(this).apply {
            text = "GENERATE PIN"
            setOnClickListener { generatePin() }
        }
        pinPanel.addView(generateBtn)
        
        refreshPinsBtn = Button(this).apply {
            text = "🔄 REFRESH PIN LIST"
            setPadding(0, 20, 0, 20)
            setOnClickListener { loadPins() }
        }
        pinPanel.addView(refreshPinsBtn)
        
        val extendSeparator = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)
            setBackgroundColor(ContextCompat.getColor(this@AdminActivity, android.R.color.darker_gray))
        }
        pinPanel.addView(extendSeparator)
        
        val extendTitle = TextView(this).apply {
            text = "⏰ Extend Active PIN"
            textSize = 18f
            setPadding(0, 10, 0, 10)
        }
        pinPanel.addView(extendTitle)
        
        extendPinInput = EditText(this).apply {
            hint = "PIN to extend"
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setPadding(10, 10, 10, 10)
            maxLines = 1
        }
        pinPanel.addView(extendPinInput)
        
        extendMinutesInput = EditText(this).apply {
            hint = "Minutes to add"
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setPadding(10, 10, 10, 10)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        pinPanel.addView(extendMinutesInput)
        
        extendAmountInput = EditText(this).apply {
            hint = "Amount in ₱ (e.g., 5.00)"
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setPadding(10, 10, 10, 10)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        pinPanel.addView(extendAmountInput)
        
        extendBtn = Button(this).apply {
            text = "⏰ EXTEND TIME"
            setOnClickListener {
                val pin = extendPinInput.text.toString().trim()
                val minutes = extendMinutesInput.text.toString().toIntOrNull()
                val amount = extendAmountInput.text.toString().toDoubleOrNull()
                if (pin.isEmpty() || minutes == null || minutes <= 0) {
                    Toast.makeText(this@AdminActivity, "Enter PIN and valid minutes", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (amount == null || amount <= 0) {
                    Toast.makeText(this@AdminActivity, "Enter valid amount (₱)", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                extendTime(pin, minutes, amount)
            }
        }
        pinPanel.addView(extendBtn)
        
        pinListText = TextView(this).apply {
            text = "Loading PINs..."
            textSize = 14f
            setPadding(0, 20, 0, 0)
        }
        pinPanel.addView(pinListText)
        
        mainLayout.addView(pinPanel)
        
        // ========== APPS PANEL ==========
        appPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        
        val appLabel = TextView(this).apply {
            text = "Select Apps for Customers"
            textSize = 18f
            setPadding(0, 20, 0, 10)
        }
        appPanel.addView(appLabel)
        
        appStatusText = TextView(this).apply {
            text = "Loading apps..."
            textSize = 12f
            setPadding(0, 0, 0, 10)
        }
        appPanel.addView(appStatusText)
        
        val scrollView = ScrollView(this)
        appContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10, 10, 10, 10)
        }
        scrollView.addView(appContainer)
        
        val scrollContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            addView(scrollView)
        }
        appPanel.addView(scrollContainer)
        
        saveAppsBtn = Button(this).apply {
            text = "💾 SAVE WHITELIST (LOCAL)"
            setPadding(0, 20, 0, 20)
            setOnClickListener { saveWhitelistLocal() }
        }
        appPanel.addView(saveAppsBtn)
        
        mainLayout.addView(appPanel)
        
        // ========== SETTINGS PANEL (Telegram only) ==========
        settingsPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        
        val settingsScrollView = ScrollView(this)
        val settingsInner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }
        
        val telegramLabel = TextView(this).apply {
            text = "🤖 Telegram Notifications"
            textSize = 16f
            setPadding(0, 20, 0, 10)
        }
        settingsInner.addView(telegramLabel)
        
        telegramTokenInput = EditText(this).apply {
            hint = "Bot Token"
            setPadding(10, 10, 10, 10)
        }
        settingsInner.addView(telegramTokenInput)
        
        telegramChatIdInput = EditText(this).apply {
            hint = "Chat ID"
            setPadding(10, 10, 10, 10)
        }
        settingsInner.addView(telegramChatIdInput)
        
        val telegramButtonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 10, 0, 10)
        }
        
        saveTelegramBtn = Button(this).apply {
            text = "💾 SAVE"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(10, 15, 10, 15)
            setOnClickListener { saveTelegramConfig() }
        }
        telegramButtonRow.addView(saveTelegramBtn)
        
        testTelegramBtn = Button(this).apply {
            text = "📨 TEST"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(10, 15, 10, 15)
            setOnClickListener { testTelegram() }
        }
        telegramButtonRow.addView(testTelegramBtn)
        
        settingsInner.addView(telegramButtonRow)
        
        val separator = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)
            setBackgroundColor(ContextCompat.getColor(this@AdminActivity, android.R.color.darker_gray))
            setPadding(0, 20, 0, 20)
        }
        settingsInner.addView(separator)
        
        val exportLabel = TextView(this).apply {
            text = "📊 Export Report"
            textSize = 16f
            setPadding(0, 0, 0, 10)
        }
        settingsInner.addView(exportLabel)
        
        exportReportBtn = Button(this).apply {
            text = "📥 EXPORT CSV"
            setPadding(10, 15, 10, 15)
            setOnClickListener { exportReport() }
        }
        settingsInner.addView(exportReportBtn)
        
        settingsScrollView.addView(settingsInner)
        settingsPanel.addView(settingsScrollView)
        
        mainLayout.addView(settingsPanel)
        
        setContentView(mainLayout)
        
        loadDashboardStats()
        loadPins()
        loadInstalledApps()
        loadCurrentWhitelistLocal()
        loadTelegramSettings()
    }
    
    private fun showDashboardPanel() {
        dashboardPanel.visibility = View.VISIBLE
        pinPanel.visibility = View.GONE
        appPanel.visibility = View.GONE
        settingsPanel.visibility = View.GONE
        loadDashboardStats()
    }
    
    private fun showPinPanel() {
        dashboardPanel.visibility = View.GONE
        pinPanel.visibility = View.VISIBLE
        appPanel.visibility = View.GONE
        settingsPanel.visibility = View.GONE
        loadPins()
    }
    
    private fun showAppPanel() {
        dashboardPanel.visibility = View.GONE
        pinPanel.visibility = View.GONE
        appPanel.visibility = View.VISIBLE
        settingsPanel.visibility = View.GONE
        loadInstalledApps()
        loadCurrentWhitelistLocal()
    }
    
    private fun showSettingsPanel() {
        dashboardPanel.visibility = View.GONE
        pinPanel.visibility = View.GONE
        appPanel.visibility = View.GONE
        settingsPanel.visibility = View.VISIBLE
        loadTelegramSettings()
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
                Toast.makeText(this@AdminActivity, if (success) "Telegram saved!" else "Failed to save", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun testTelegram() {
        val token = telegramTokenInput.text.toString().trim()
        val chatId = telegramChatIdInput.text.toString().trim()

        if (token.isEmpty() || chatId.isEmpty()) {
            appStatusText.text = "❌ Please enter Bot Token and Chat ID, then tap SAVE."
            appStatusText.setTextColor(android.graphics.Color.RED)
            return
        }

        testTelegramBtn.isEnabled = false
        testTelegramBtn.text = "SENDING..."
        appStatusText.text = "Sending test message..."
        appStatusText.setTextColor(android.graphics.Color.BLUE)

        Thread {
            try {
                val urlString = "https://api.telegram.org/bot$token/sendMessage?chat_id=$chatId&text=✅%20Test%20notification%20from%20KCB%20Rental%20Kiosk!&parse_mode=HTML"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                connection.disconnect()

                runOnUiThread {
                    testTelegramBtn.isEnabled = true
                    testTelegramBtn.text = "📨 TEST"
                    if (responseCode == 200) {
                        appStatusText.text = "✓ Message sent! Check your Telegram bot."
                        appStatusText.setTextColor(android.graphics.Color.GREEN)
                        Toast.makeText(this@AdminActivity, "Test sent! Check Telegram.", Toast.LENGTH_LONG).show()
                    } else {
                        appStatusText.text = "✗ Failed! HTTP $responseCode"
                        appStatusText.setTextColor(android.graphics.Color.RED)
                        Toast.makeText(this@AdminActivity, "Failed: HTTP $responseCode", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    testTelegramBtn.isEnabled = true
                    testTelegramBtn.text = "📨 TEST"
                    appStatusText.text = "✗ Error: ${e.message}"
                    appStatusText.setTextColor(android.graphics.Color.RED)
                    Toast.makeText(this@AdminActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
    
    private fun generatePin() {
        val minutes = generateMinutesInput.text.toString().toIntOrNull()
        val amount = generateAmountInput.text.toString().toDoubleOrNull()

        if (minutes == null || minutes <= 0) {
            Toast.makeText(this, "Enter valid minutes (1-1440)", Toast.LENGTH_SHORT).show()
            return
        }
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Enter valid amount (₱)", Toast.LENGTH_SHORT).show()
            return
        }

        val customPin = generatePinInput.text.toString().trim()
        if (customPin.isNotEmpty() && customPin.length != 6) {
            Toast.makeText(this, "PIN must be exactly 6 characters", Toast.LENGTH_SHORT).show()
            return
        }
        val pin = if (customPin.isNotEmpty()) customPin else null

        generateBtn.isEnabled = false
        generateBtn.text = "GENERATING..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = supabase.generatePin(pin, minutes * 60, amount)
                
                withContext(Dispatchers.Main) {
                    generateBtn.isEnabled = true
                    generateBtn.text = "GENERATE PIN"
                    
                    if (result != null) {
                        val message = "💰 *New PIN Generated!*%0APIN: $result%0ADuration: $minutes minutes%0AAmount: ₱${String.format("%.2f", amount)}"
                        supabase.sendTelegramNotification(message)
                        Toast.makeText(this@AdminActivity, "✅ PIN: $result ($minutes min - ₱$amount)", Toast.LENGTH_LONG).show()
                        generatePinInput.text.clear()
                        generateMinutesInput.text.clear()
                        generateAmountInput.text.clear()
                        loadPins()
                    } else {
                        Toast.makeText(this@AdminActivity, "Failed to generate PIN", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    generateBtn.isEnabled = true
                    generateBtn.text = "GENERATE PIN"
                    Toast.makeText(this@AdminActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun extendTime(pin: String, minutes: Int, amount: Double) {
        extendBtn.isEnabled = false
        extendBtn.text = "EXTENDING..."
        
        CoroutineScope(Dispatchers.IO).launch {
            val success = supabase.extendTime(pin, minutes)
            if (success) {
                supabase.recordExtension(pin, minutes, amount)
                supabase.sendTelegramNotification("⏰ *Session Extended!*%0APIN: $pin%0AAdded: $minutes minutes%0AAdditional Payment: ₱${String.format("%.2f", amount)}")
                
                withContext(Dispatchers.Main) {
                    extendBtn.isEnabled = true
                    extendBtn.text = "⏰ EXTEND TIME"
                    Toast.makeText(this@AdminActivity, "Added $minutes minutes (₱$amount) to PIN $pin", Toast.LENGTH_LONG).show()
                    loadPins()
                    extendPinInput.text.clear()
                    extendMinutesInput.text.clear()
                    extendAmountInput.text.clear()
                }
            } else {
                withContext(Dispatchers.Main) {
                    extendBtn.isEnabled = true
                    extendBtn.text = "⏰ EXTEND TIME"
                    Toast.makeText(this@AdminActivity, "Failed to extend", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun exportReport() {
        CoroutineScope(Dispatchers.IO).launch {
            val history = supabase.getSessionHistory()
            val csv = StringBuilder("PIN,Minutes,Amount,Date\n")
            for (record in history) {
                csv.append("${record.pin},${record.minutes},${record.amount},${record.date}\n")
            }
            withContext(Dispatchers.Main) {
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
            }
        }
    }
    
    private fun loadDashboardStats() {
        CoroutineScope(Dispatchers.IO).launch {
            val stats = supabase.getIncomeStats()
            val history = supabase.getSessionHistory()
            withContext(Dispatchers.Main) {
                val currency = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
                dailyIncomeText.text = "Today: ${currency.format(stats.daily)}"
                weeklyIncomeText.text = "This Week: ${currency.format(stats.weekly)}"
                monthlyIncomeText.text = "This Month: ${currency.format(stats.monthly)}"
                yearlyIncomeText.text = "This Year: ${currency.format(stats.yearly)}"
                totalSessionsText.text = "Total Sessions: ${stats.totalSessions}"
                sessionHistoryRecycler.adapter = SessionHistoryAdapter(history)
            }
        }
    }
    
    private fun showChangePasswordDialog() {
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = "New password (min 4 characters)"
        
        AlertDialog.Builder(this)
            .setTitle("Change Admin Password")
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
    
    private fun loadInstalledApps() {
        appStatusText.text = "Scanning apps..."
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
                text = "${app.first}\n(${app.second})"
                setPadding(10, 15, 10, 15)
                textSize = 14f
            }
            appContainer.addView(checkBox)
            checkBoxes.add(Pair(checkBox, app.second))
        }
        
        appStatusText.text = "Found ${installedApps.size} user apps. Select which ones to allow."
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
        saveAppsBtn.text = "💾 SAVE WHITELIST (LOCAL)"
        appStatusText.text = "✓ Success! Saved ${selectedPackages.size} apps locally"
        appStatusText.setTextColor(android.graphics.Color.GREEN)
        Toast.makeText(this, "Whitelist saved locally! ${selectedPackages.size} apps", Toast.LENGTH_LONG).show()
    }
    
    private fun loadPins() {
        pinListText.text = "Loading..."
        CoroutineScope(Dispatchers.IO).launch {
            val pins = supabase.getActivePins()
            withContext(Dispatchers.Main) {
                if (pins.isEmpty()) {
                    pinListText.text = "No active PINs"
                } else {
                    val sb = StringBuilder()
                    for (p in pins) {
                        val minutes = p.secondsLeft / 60
                        sb.append("🔑 ${p.pin} - ${minutes} min TOTAL\n")
                    }
                    pinListText.text = sb.toString()
                }
            }
        }
    }
}
