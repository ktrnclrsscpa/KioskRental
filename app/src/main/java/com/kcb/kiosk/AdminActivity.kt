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
import android.widget.Spinner
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
    
    // Panels
    private lateinit var dashboardPanel: LinearLayout
    private lateinit var pinPanel: LinearLayout
    private lateinit var appPanel: LinearLayout
    private lateinit var settingsPanel: LinearLayout
    
    // PINS panel
    private lateinit var pinListText: TextView
    private lateinit var generatePinInput: EditText
    private lateinit var generateMinutesInput: EditText
    private lateinit var generateBtn: Button
    private lateinit var refreshPinsBtn: Button
    private lateinit var extendPinInput: EditText
    private lateinit var extendMinutesInput: EditText
    private lateinit var extendBtn: Button
    
    // APPS panel
    private lateinit var appContainer: LinearLayout
    private lateinit var saveAppsBtn: Button
    private lateinit var appStatusText: TextView
    private val checkBoxes = mutableListOf<Pair<CheckBox, String>>()
    
    // Dashboard panel
    private lateinit var dailyIncomeText: TextView
    private lateinit var weeklyIncomeText: TextView
    private lateinit var monthlyIncomeText: TextView
    private lateinit var yearlyIncomeText: TextView
    private lateinit var totalSessionsText: TextView
    private lateinit var sessionHistoryRecycler: RecyclerView
    
    // Settings panel
    private lateinit var pricingTypeSpinner: Spinner
    private lateinit var priceAmountInput: EditText
    private lateinit var durationInput: EditText
    private lateinit var extendPriceInput: EditText
    private lateinit var extendDurationInput: EditText
    private lateinit var savePricingBtn: Button
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
        
        val pinRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        
        generatePinInput = EditText(this).apply {
            hint = "PIN (leave blank for random)"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(10, 10, 10, 10)
            maxLines = 1
        }
        pinRow.addView(generatePinInput)
        
        generateMinutesInput = EditText(this).apply {
            hint = "Minutes"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f)
            setPadding(10, 10, 10, 10)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        pinRow.addView(generateMinutesInput)
        pinPanel.addView(pinRow)
        
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
        
        val extendRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 10)
        }
        
        extendPinInput = EditText(this).apply {
            hint = "PIN to extend"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(10, 10, 10, 10)
            maxLines = 1
        }
        extendRow.addView(extendPinInput)
        
        extendMinutesInput = EditText(this).apply {
            hint = "Minutes to add"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f)
            setPadding(10, 10, 10, 10)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        extendRow.addView(extendMinutesInput)
        pinPanel.addView(extendRow)
        
        extendBtn = Button(this).apply {
            text = "⏰ EXTEND TIME"
            setOnClickListener {
                val pin = extendPinInput.text.toString().trim()
                val minutes = extendMinutesInput.text.toString().toIntOrNull()
                if (pin.isEmpty() || minutes == null || minutes <= 0) {
                    Toast.makeText(this@AdminActivity, "Enter PIN and valid minutes", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                extendTime(pin, minutes)
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
        
        // ========== SETTINGS PANEL ==========
        settingsPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        
        val settingsScrollView = ScrollView(this)
        val settingsInner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }
        
        // Pricing Type
        val pricingTypeLabel = TextView(this).apply {
            text = "💰 Pricing Type"
            textSize = 16f
            setPadding(0, 20, 0, 10)
        }
        settingsInner.addView(pricingTypeLabel)
        
        pricingTypeSpinner = Spinner(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            adapter = android.widget.ArrayAdapter(this@AdminActivity, android.R.layout.simple_spinner_item, listOf("Fixed Price per Session", "Price per Hour"))
        }
        settingsInner.addView(pricingTypeSpinner)
        
        // Price Amount
        val priceAmountLabel = TextView(this).apply {
            text = "Price Amount (₱)"
            textSize = 14f
            setPadding(0, 10, 0, 5)
        }
        settingsInner.addView(priceAmountLabel)
        
        priceAmountInput = EditText(this).apply {
            hint = "e.g., 15"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(10, 10, 10, 10)
        }
        settingsInner.addView(priceAmountInput)
        
        // Duration
        val durationLabel = TextView(this).apply {
            text = "Duration (minutes)"
            textSize = 14f
            setPadding(0, 10, 0, 5)
        }
        settingsInner.addView(durationLabel)
        
        durationInput = EditText(this).apply {
            hint = "e.g., 60 for 1 hour"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setPadding(10, 10, 10, 10)
        }
        settingsInner.addView(durationInput)
        
        val separator1 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)
            setBackgroundColor(ContextCompat.getColor(this@AdminActivity, android.R.color.darker_gray))
            setPadding(0, 20, 0, 20)
        }
        settingsInner.addView(separator1)
        
        // Extension Settings
        val extensionLabel = TextView(this).apply {
            text = "⏰ Extension Settings"
            textSize = 16f
            setPadding(0, 0, 0, 10)
        }
        settingsInner.addView(extensionLabel)
        
        val extPriceLabel = TextView(this).apply {
            text = "Extension Price (₱)"
            textSize = 14f
            setPadding(0, 10, 0, 5)
        }
        settingsInner.addView(extPriceLabel)
        
        extendPriceInput = EditText(this).apply {
            hint = "e.g., 10"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(10, 10, 10, 10)
        }
        settingsInner.addView(extendPriceInput)
        
        val extDurationLabel = TextView(this).apply {
            text = "Extension Duration (minutes)"
            textSize = 14f
            setPadding(0, 10, 0, 5)
        }
        settingsInner.addView(extDurationLabel)
        
        extendDurationInput = EditText(this).apply {
            hint = "e.g., 30"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setPadding(10, 10, 10, 10)
        }
        settingsInner.addView(extendDurationInput)
        
        savePricingBtn = Button(this).apply {
            text = "💾 SAVE PRICING"
            setPadding(0, 15, 0, 15)
            setOnClickListener { savePricingConfig() }
        }
        settingsInner.addView(savePricingBtn)
        
        val separator2 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)
            setBackgroundColor(ContextCompat.getColor(this@AdminActivity, android.R.color.darker_gray))
            setPadding(0, 20, 0, 20)
        }
        settingsInner.addView(separator2)
        
        // Telegram Settings
        val telegramLabel = TextView(this).apply {
            text = "🤖 Telegram Notifications"
            textSize = 16f
            setPadding(0, 0, 0, 10)
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
        
        val separator3 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)
            setBackgroundColor(ContextCompat.getColor(this@AdminActivity, android.R.color.darker_gray))
            setPadding(0, 20, 0, 20)
        }
        settingsInner.addView(separator3)
        
        // Export Report
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
        loadSettings()
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
        loadSettings()
    }
    
    private fun loadSettings() {
        CoroutineScope(Dispatchers.IO).launch {
            val config = supabase.getPricingConfig()
            val (token, chatId) = supabase.getTelegramConfig()
            withContext(Dispatchers.Main) {
                pricingTypeSpinner.setSelection(if (config.pricingType == "fixed") 0 else 1)
                priceAmountInput.setText(config.priceAmount.toString())
                durationInput.setText(config.durationMinutes.toString())
                extendPriceInput.setText(config.extendPrice.toString())
                extendDurationInput.setText(config.extendDuration.toString())
                telegramTokenInput.setText(token)
                telegramChatIdInput.setText(chatId)
            }
        }
    }
    
    private fun savePricingConfig() {
        val pricingType = if (pricingTypeSpinner.selectedItemPosition == 0) "fixed" else "hourly"
        val priceAmount = priceAmountInput.text.toString().toDoubleOrNull() ?: 15.0
        val durationMinutes = durationInput.text.toString().toIntOrNull() ?: 60
        val extendPrice = extendPriceInput.text.toString().toDoubleOrNull() ?: 10.0
        val extendDuration = extendDurationInput.text.toString().toIntOrNull() ?: 30
        
        savePricingBtn.isEnabled = false
        savePricingBtn.text = "SAVING..."
        CoroutineScope(Dispatchers.IO).launch {
            val success = supabase.updatePricingConfig(pricingType, priceAmount, durationMinutes, extendPrice, extendDuration)
            withContext(Dispatchers.Main) {
                savePricingBtn.isEnabled = true
                savePricingBtn.text = "💾 SAVE PRICING"
                Toast.makeText(this@AdminActivity, if (success) "Pricing saved!" else "Failed to save", Toast.LENGTH_SHORT).show()
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
        // Get values directly from input fields
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

        // Run network request on background thread
        Thread {
            try {
                val urlString = "https://api.telegram.org/bot$token/sendMessage?chat_id=$chatId&text=✅%20Test%20notification%20from%20KCB%20Rental%20Kiosk!&parse_mode=HTML"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                val responseMessage = connection.responseMessage
                connection.disconnect()

                runOnUiThread {
                    testTelegramBtn.isEnabled = true
                    testTelegramBtn.text = "📨 TEST"
                    if (responseCode == 200) {
                        appStatusText.text = "✓ Message sent! Check your Telegram bot."
                        appStatusText.setTextColor(android.graphics.Color.GREEN)
                        Toast.makeText(this@AdminActivity, "Test sent! Check Telegram.", Toast.LENGTH_LONG).show()
                    } else {
                        appStatusText.text = "✗ Failed! HTTP $responseCode: $responseMessage"
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
    
    private fun generatePin() {
        val minutes = generateMinutesInput.text.toString().toIntOrNull()
        if (minutes == null || minutes <= 0) {
            Toast.makeText(this, "Enter valid minutes", Toast.LENGTH_SHORT).show()
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
            val result = supabase.generatePin(pin, minutes * 60)
            withContext(Dispatchers.Main) {
                generateBtn.isEnabled = true
                generateBtn.text = "GENERATE PIN"
                if (result != null) {
                    Toast.makeText(this@AdminActivity, "PIN: $result ($minutes min)", Toast.LENGTH_LONG).show()
                    generatePinInput.text.clear()
                    generateMinutesInput.text.clear()
                    loadPins()
                } else {
                    Toast.makeText(this@AdminActivity, "Failed to generate PIN", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun extendTime(pin: String, minutes: Int) {
        extendBtn.isEnabled = false
        extendBtn.text = "EXTENDING..."
        CoroutineScope(Dispatchers.IO).launch {
            val success = supabase.extendTime(pin, minutes)
            withContext(Dispatchers.Main) {
                extendBtn.isEnabled = true
                extendBtn.text = "⏰ EXTEND TIME"
                if (success) {
                    Toast.makeText(this@AdminActivity, "Added $minutes minutes to PIN $pin", Toast.LENGTH_LONG).show()
                    loadPins()
                    extendPinInput.text.clear()
                    extendMinutesInput.text.clear()
                } else {
                    Toast.makeText(this@AdminActivity, "Failed to extend", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
