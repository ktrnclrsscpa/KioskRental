package com.kcb.kiosk

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
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
    
    private lateinit var scrollView: ScrollView
    private lateinit var mainContainer: LinearLayout
    
    // Stats Cards
    private lateinit var totalIncomeCard: TextView
    private lateinit var totalSessionsCard: TextView
    private lateinit var activePinsCard: TextView
    private lateinit var todayIncomeCard: TextView
    
    // Recent Sessions
    private lateinit var sessionHistoryRecycler: RecyclerView
    
    // PIN Management
    private lateinit var generatePinInput: EditText
    private lateinit var generateMinutesInput: EditText
    private lateinit var generateAmountInput: EditText
    private lateinit var generateBtn: Button
    private lateinit var extendPinInput: EditText
    private lateinit var extendMinutesInput: EditText
    private lateinit var extendAmountInput: EditText
    private lateinit var extendBtn: Button
    
    // App Whitelist
    private lateinit var appContainer: LinearLayout
    private lateinit var saveAppsBtn: Button
    private lateinit var appStatusText: TextView
    private val checkBoxes = mutableListOf<Pair<CheckBox, String>>()
    
    // Settings
    private lateinit var telegramTokenInput: EditText
    private lateinit var telegramChatIdInput: EditText
    private lateinit var saveTelegramBtn: Button
    private lateinit var testTelegramBtn: Button
    private lateinit var exportReportBtn: Button
    
    private val prefs by lazy { getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE) }
    private var activePinsCount = 0

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
    
    private fun createStatCard(title: String, value: String, icon: String, color: String): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, 15, 0)
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor(color))
                cornerRadius = 16f
            }
        }
        
        val iconText = TextView(this).apply {
            text = icon
            textSize = 28f
            setPadding(0, 0, 0, 10)
        }
        card.addView(iconText)
        
        val valueText = TextView(this).apply {
            text = value
            textSize = 24f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        card.addView(valueText)
        
        val titleText = TextView(this).apply {
            text = title
            textSize = 12f
            setTextColor(Color.parseColor("#E0E0E0"))
        }
        card.addView(titleText)
        
        return card
    }
    
    private fun createSection(title: String, icon: String): LinearLayout {
        val section = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 20, 0, 10)
        }
        
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 15)
        }
        
        val iconText = TextView(this).apply {
            text = icon
            textSize = 20f
            setPadding(0, 0, 10, 0)
        }
        header.addView(iconText)
        
        val titleText = TextView(this).apply {
            text = title
            textSize = 18f
            setTextColor(Color.parseColor("#2C3E50"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        header.addView(titleText)
        
        section.addView(header)
        
        return section
    }
    
    private fun createEditTextBackground(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = 12f
            setStroke(1, Color.parseColor("#DDDDDD"))
        }
    }
    
    private fun initAdminPanel() {
        supabase = SupabaseClient.getInstance()
        
        scrollView = ScrollView(this)
        mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(25, 30, 25, 50)
            setBackgroundColor(Color.parseColor("#F8F9FA"))
        }
        
        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 25)
        }
        
        val title = TextView(this).apply {
            text = "KCB RENTAL ADMIN"
            textSize = 24f
            setTextColor(Color.parseColor("#2C3E50"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        header.addView(title)
        
        val changePwdBtn = Button(this).apply {
            text = "Change Password"
            textSize = 12f
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.parseColor("#3498DB"))
            setOnClickListener { showChangePasswordDialog() }
        }
        header.addView(changePwdBtn)
        
        mainContainer.addView(header)
        
        // Stats Row
        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 25)
        }
        
        totalIncomeCard = TextView(this)
        totalSessionsCard = TextView(this)
        activePinsCard = TextView(this)
        todayIncomeCard = TextView(this)
        
        val statsCard1 = createStatCard("Total Income", "₱0", "💰", "#2ECC71")
        val statsCard2 = createStatCard("Total Sessions", "0", "📊", "#3498DB")
        val statsCard3 = createStatCard("Active PINs", "0", "🔑", "#E67E22")
        val statsCard4 = createStatCard("Today's Sales", "₱0", "📈", "#9B59B6")
        
        // Store references to update later
        totalIncomeCard = (statsCard1.getChildAt(1) as TextView)
        totalSessionsCard = (statsCard2.getChildAt(1) as TextView)
        activePinsCard = (statsCard3.getChildAt(1) as TextView)
        todayIncomeCard = (statsCard4.getChildAt(1) as TextView)
        
        statsRow.addView(statsCard1)
        statsRow.addView(statsCard2)
        statsRow.addView(statsCard3)
        statsRow.addView(statsCard4)
        
        mainContainer.addView(statsRow)
        
        // Recent Sessions Section
        val sessionsSection = createSection("Recent Sessions", "📜")
        
        sessionHistoryRecycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@AdminActivity)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 350)
        }
        sessionsSection.addView(sessionHistoryRecycler)
        mainContainer.addView(sessionsSection)
        
        // PIN Management Section
        val pinManagementSection = createSection("PIN Management", "🔑")
        
        generatePinInput = EditText(this).apply {
            hint = "PIN (leave blank for random)"
            setPadding(15, 12, 15, 12)
            background = createEditTextBackground()
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 10)
            }
        }
        pinManagementSection.addView(generatePinInput)
        
        generateMinutesInput = EditText(this).apply {
            hint = "Minutes (e.g., 60)"
            setPadding(15, 12, 15, 12)
            background = createEditTextBackground()
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 10)
            }
        }
        pinManagementSection.addView(generateMinutesInput)
        
        generateAmountInput = EditText(this).apply {
            hint = "Amount in ₱ (e.g., 15.00)"
            setPadding(15, 12, 15, 12)
            background = createEditTextBackground()
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 10)
            }
        }
        pinManagementSection.addView(generateAmountInput)
        
        generateBtn = Button(this).apply {
            text = "🔐 GENERATE PIN"
            textSize = 14f
            setPadding(15, 12, 15, 12)
            setBackgroundColor(Color.parseColor("#2ECC71"))
            setTextColor(Color.WHITE)
            setOnClickListener { generatePin() }
        }
        pinManagementSection.addView(generateBtn)
        
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 15)
        }
        pinManagementSection.addView(spacer)
        
        // Extend PIN
        extendPinInput = EditText(this).apply {
            hint = "PIN to extend"
            setPadding(15, 12, 15, 12)
            background = createEditTextBackground()
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 10)
            }
        }
        pinManagementSection.addView(extendPinInput)
        
        extendMinutesInput = EditText(this).apply {
            hint = "Minutes to add"
            setPadding(15, 12, 15, 12)
            background = createEditTextBackground()
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 10)
            }
        }
        pinManagementSection.addView(extendMinutesInput)
        
        extendAmountInput = EditText(this).apply {
            hint = "Amount in ₱ (e.g., 5.00)"
            setPadding(15, 12, 15, 12)
            background = createEditTextBackground()
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 10)
            }
        }
        pinManagementSection.addView(extendAmountInput)
        
        extendBtn = Button(this).apply {
            text = "⏰ EXTEND TIME"
            textSize = 14f
            setPadding(15, 12, 15, 12)
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
                    Toast.makeText(this@AdminActivity, "Enter valid amount (₱)", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                extendTime(pin, minutes, amount)
            }
        }
        pinManagementSection.addView(extendBtn)
        
        mainContainer.addView(pinManagementSection)
        
        // App Whitelist Section
        val whitelistSection = createSection("App Whitelist", "📱")
        
        appStatusText = TextView(this).apply {
            text = "Loading apps..."
            textSize = 12f
            setPadding(0, 0, 0, 10)
            setTextColor(Color.parseColor("#7F8C8D"))
        }
        whitelistSection.addView(appStatusText)
        
        val appScrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 250)
        }
        appContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10, 10, 10, 10)
        }
        appScrollView.addView(appContainer)
        whitelistSection.addView(appScrollView)
        
        saveAppsBtn = Button(this).apply {
            text = "💾 SAVE WHITELIST"
            textSize = 14f
            setPadding(15, 12, 15, 12)
            setBackgroundColor(Color.parseColor("#3498DB"))
            setTextColor(Color.WHITE)
            setOnClickListener { saveWhitelistLocal() }
        }
        whitelistSection.addView(saveAppsBtn)
        
        mainContainer.addView(whitelistSection)
        
        // Settings Section
        val settingsSection = createSection("Settings", "⚙️")
        
        telegramTokenInput = EditText(this).apply {
            hint = "Telegram Bot Token"
            setPadding(15, 12, 15, 12)
            background = createEditTextBackground()
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 10)
            }
        }
        settingsSection.addView(telegramTokenInput)
        
        telegramChatIdInput = EditText(this).apply {
            hint = "Telegram Chat ID"
            setPadding(15, 12, 15, 12)
            background = createEditTextBackground()
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 10)
            }
        }
        settingsSection.addView(telegramChatIdInput)
        
        val telegramRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 10)
        }
        
        saveTelegramBtn = Button(this).apply {
            text = "💾 SAVE"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, 10, 0)
            }
            setBackgroundColor(Color.parseColor("#2ECC71"))
            setTextColor(Color.WHITE)
            setOnClickListener { saveTelegramConfig() }
        }
        telegramRow.addView(saveTelegramBtn)
        
        testTelegramBtn = Button(this).apply {
            text = "📨 TEST"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setBackgroundColor(Color.parseColor("#E67E22"))
            setTextColor(Color.WHITE)
            setOnClickListener { testTelegram() }
        }
        telegramRow.addView(testTelegramBtn)
        
        settingsSection.addView(telegramRow)
        
        exportReportBtn = Button(this).apply {
            text = "📥 EXPORT CSV REPORT"
            textSize = 14f
            setPadding(15, 12, 15, 12)
            setBackgroundColor(Color.parseColor("#9B59B6"))
            setTextColor(Color.WHITE)
            setOnClickListener { exportReport() }
        }
        settingsSection.addView(exportReportBtn)
        
        mainContainer.addView(settingsSection)
        
        scrollView.addView(mainContainer)
        setContentView(scrollView)
        
        loadDashboardStats()
        loadPinsCount()
        loadInstalledApps()
        loadCurrentWhitelistLocal()
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
            return
        }
        testTelegramBtn.isEnabled = false
        testTelegramBtn.text = "SENDING..."
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
                        Toast.makeText(this@AdminActivity, "Test sent! Check Telegram.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@AdminActivity, "Failed: HTTP $responseCode", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    testTelegramBtn.isEnabled = true
                    testTelegramBtn.text = "📨 TEST"
                    Toast.makeText(this@AdminActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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
                    generateBtn.text = "🔐 GENERATE PIN"
                    if (result != null) {
                        Toast.makeText(this@AdminActivity, "✅ PIN: $result ($minutes min - ₱$amount)", Toast.LENGTH_LONG).show()
                        generatePinInput.text.clear()
                        generateMinutesInput.text.clear()
                        generateAmountInput.text.clear()
                        loadPinsCount()
                    } else {
                        Toast.makeText(this@AdminActivity, "Failed to generate PIN", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    generateBtn.isEnabled = true
                    generateBtn.text = "🔐 GENERATE PIN"
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
                    extendPinInput.text.clear()
                    extendMinutesInput.text.clear()
                    extendAmountInput.text.clear()
                    loadPinsCount()
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
            val csv = StringBuilder("PIN,Minutes,Amount,Date,Type\n")
            for (record in history) {
                val type = if (record.pin.endsWith("_EXT")) "EXTENSION" else "SESSION"
                val cleanPin = record.pin.replace("_EXT", "")
                csv.append("$cleanPin,${record.minutes},${record.amount},${record.date},$type\n")
            }
            withContext(Dispatchers.Main) {
                val file = File(cacheDir, "sales_report.csv")
                file.writeText(csv.toString())
                val uri = androidx.core.content.FileProvider.getUriForFile(this@AdminActivity, "${packageName}.fileprovider", file)
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
                totalIncomeCard.text = currency.format(stats.yearly)
                totalSessionsCard.text = stats.totalSessions.toString()
                todayIncomeCard.text = currency.format(stats.daily)
                sessionHistoryRecycler.adapter = SessionHistoryAdapter(history)
            }
        }
    }
    
    private fun loadPinsCount() {
        CoroutineScope(Dispatchers.IO).launch {
            val pins = supabase.getActivePins()
            withContext(Dispatchers.Main) {
                activePinsCard.text = pins.size.toString()
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
        saveAppsBtn.text = "💾 SAVE WHITELIST"
        appStatusText.text = "✓ Success! Saved ${selectedPackages.size} apps locally"
        Toast.makeText(this, "Whitelist saved locally! ${selectedPackages.size} apps", Toast.LENGTH_LONG).show()
    }
}
