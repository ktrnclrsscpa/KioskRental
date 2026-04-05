package com.kcb.kiosk

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
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
            textSize = 22f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        card.addView(valueText)
        
        val titleText = TextView(this).apply {
            text = title
            textSize = 11f
            setTextColor(Color.parseColor("#E0E0E0"))
        }
        card.addView(titleText)
        
        return card
    }
    
    private fun createSectionHeader(title: String, icon: String): LinearLayout {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 20, 0, 15)
        }
        
        val iconText = TextView(this).apply {
            text = icon
            textSize = 22f
            setPadding(0, 0, 12, 0)
        }
        header.addView(iconText)
        
        val titleText = TextView(this).apply {
            text = title
            textSize = 18f
            setTextColor(Color.parseColor("#2C3E50"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        header.addView(titleText)
        
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
                setMargins(0, 10, 0, 0)
            }
            setBackgroundColor(Color.parseColor("#E0E0E0"))
        }
        
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(header)
            addView(divider)
        }
        
        return container
    }
    
    private fun createInputField(hint: String, isNumber: Boolean = false): EditText {
        return EditText(this).apply {
            this.hint = hint
            setPadding(18, 14, 18, 14)
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
    
    private fun createButton(text: String, color: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            textSize = 14f
            setPadding(18, 14, 18, 14)
            setBackgroundColor(Color.parseColor(color))
            setTextColor(Color.WHITE)
            setAllCaps(false)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 5, 0, 5)
            }
        }
    }
    
    private fun createCardView(content: View): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 15, 20, 15)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 16f
                setStroke(1, Color.parseColor("#EEEEEE"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 20)
            }
            addView(content)
        }
    }
    
    private fun initAdminPanel() {
        supabase = SupabaseClient.getInstance()
        
        scrollView = ScrollView(this)
        mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(25, 30, 25, 50)
            setBackgroundColor(Color.parseColor("#F5F7FA"))
        }
        
        // ========== HEADER ==========
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 20)
        }
        
        val title = TextView(this).apply {
            text = "KCB RENTAL ADMIN"
            textSize = 26f
            setTextColor(Color.parseColor("#1A2C3E"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        header.addView(title)
        
        val changePwdBtn = Button(this).apply {
            text = "Change Pwd"
            textSize = 12f
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.parseColor("#3498DB"))
            setOnClickListener { showChangePasswordDialog() }
        }
        header.addView(changePwdBtn)
        
        mainContainer.addView(header)
        
        // ========== STATS ROW ==========
        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 25)
        }
        
        val statsCard1 = createStatCard("Total Income", "₱0", "💰", "#2ECC71")
        val statsCard2 = createStatCard("Total Sessions", "0", "📊", "#3498DB")
        val statsCard3 = createStatCard("Active PINs", "0", "🔑", "#E67E22")
        val statsCard4 = createStatCard("Today's Sales", "₱0", "📈", "#9B59B6")
        
        totalIncomeCard = (statsCard1.getChildAt(1) as TextView)
        totalSessionsCard = (statsCard2.getChildAt(1) as TextView)
        activePinsCard = (statsCard3.getChildAt(1) as TextView)
        todayIncomeCard = (statsCard4.getChildAt(1) as TextView)
        
        statsRow.addView(statsCard1)
        statsRow.addView(statsCard2)
        statsRow.addView(statsCard3)
        statsRow.addView(statsCard4)
        
        mainContainer.addView(statsRow)
        
        // ========== RECENT SESSIONS SECTION ==========
        mainContainer.addView(createSectionHeader("Recent Sessions", "📜"))
        
        val sessionsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 5, 0, 0)
        }
        
        sessionHistoryRecycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@AdminActivity)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 350)
        }
        sessionsCard.addView(sessionHistoryRecycler)
        
        mainContainer.addView(createCardView(sessionsCard))
        
        // ========== GENERATE PIN SECTION ==========
        mainContainer.addView(createSectionHeader("Generate New PIN", "🔐"))
        
        val generateCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 5, 0, 0)
        }
        
        generatePinInput = createInputField("PIN (leave blank for random)")
        generateCard.addView(generatePinInput)
        
        generateMinutesInput = createInputField("Minutes (e.g., 60)", true)
        generateCard.addView(generateMinutesInput)
        
        generateAmountInput = createInputField("Amount in ₱ (e.g., 15.00)", true)
        generateAmountInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        generateCard.addView(generateAmountInput)
        
        generateBtn = createButton("GENERATE PIN", "#2ECC71") { generatePin() }
        generateCard.addView(generateBtn)
        
        mainContainer.addView(createCardView(generateCard))
        
        // ========== EXTEND PIN SECTION ==========
        mainContainer.addView(createSectionHeader("Extend Active PIN", "⏰"))
        
        val extendCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 5, 0, 0)
        }
        
        extendPinInput = createInputField("PIN to extend")
        extendCard.addView(extendPinInput)
        
        extendMinutesInput = createInputField("Minutes to add", true)
        extendCard.addView(extendMinutesInput)
        
        extendAmountInput = createInputField("Amount in ₱ (e.g., 5.00)", true)
        extendAmountInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        extendCard.addView(extendAmountInput)
        
        extendBtn = createButton("EXTEND TIME", "#E67E22") {
            val pin = extendPinInput.text.toString().trim()
            val minutes = extendMinutesInput.text.toString().toIntOrNull()
            val amount = extendAmountInput.text.toString().toDoubleOrNull()
            if (pin.isEmpty() || minutes == null || minutes <= 0) {
                Toast.makeText(this@AdminActivity, "Enter PIN and valid minutes", Toast.LENGTH_SHORT).show()
                return@createButton
            }
            if (amount == null || amount <= 0) {
                Toast.makeText(this@AdminActivity, "Enter valid amount (₱)", Toast.LENGTH_SHORT).show()
                return@createButton
            }
            extendTime(pin, minutes, amount)
        }
        extendCard.addView(extendBtn)
        
        mainContainer.addView(createCardView(extendCard))
        
        // ========== APP WHITELIST SECTION ==========
        mainContainer.addView(createSectionHeader("App Whitelist", "📱"))
        
        val whitelistCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 5, 0, 0)
        }
        
        appStatusText = TextView(this).apply {
            text = "Loading apps..."
            textSize = 12f
            setPadding(0, 0, 0, 10)
            setTextColor(Color.parseColor("#7F8C8D"))
        }
        whitelistCard.addView(appStatusText)
        
        val appScrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 250)
        }
        appContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(5, 5, 5, 5)
        }
        appScrollView.addView(appContainer)
        whitelistCard.addView(appScrollView)
        
        saveAppsBtn = createButton("SAVE WHITELIST", "#3498DB") { saveWhitelistLocal() }
        whitelistCard.addView(saveAppsBtn)
        
        mainContainer.addView(createCardView(whitelistCard))
        
        // ========== SETTINGS SECTION ==========
        mainContainer.addView(createSectionHeader("Settings", "⚙️"))
        
        val settingsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 5, 0, 0)
        }
        
        telegramTokenInput = createInputField("Telegram Bot Token")
        settingsCard.addView(telegramTokenInput)
        
        telegramChatIdInput = createInputField("Telegram Chat ID")
        settingsCard.addView(telegramChatIdInput)
        
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 10, 0, 10)
        }
        
        saveTelegramBtn = Button(this).apply {
            text = "SAVE"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, 10, 0)
            }
            setBackgroundColor(Color.parseColor("#2ECC71"))
            setTextColor(Color.WHITE)
            setOnClickListener { saveTelegramConfig() }
        }
        buttonRow.addView(saveTelegramBtn)
        
        testTelegramBtn = Button(this).apply {
            text = "TEST"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setBackgroundColor(Color.parseColor("#E67E22"))
            setTextColor(Color.WHITE)
            setOnClickListener { testTelegram() }
        }
        buttonRow.addView(testTelegramBtn)
        
        settingsCard.addView(buttonRow)
        
        exportReportBtn = createButton("EXPORT CSV REPORT", "#9B59B6") { exportReport() }
        settingsCard.addView(exportReportBtn)
        
        mainContainer.addView(createCardView(settingsCard))
        
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
                Toast.makeText(this@AdminActivity, if (success) "Telegram saved!", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun testTelegram() {
        val token = telegramTokenInput.text.toString().trim()
        val chatId = telegramChatIdInput.text.toString().trim()
        if (token.isEmpty() || chatId.isEmpty()) {
            Toast.makeText(this, "Please enter Bot Token and Chat ID first", Toast.LENGTH_SHORT).show()
            return
        }
        testTelegramBtn.isEnabled = false
        testTelegramBtn.text = "SENDING..."
        Thread {
            try {
                val urlString = "https://api.telegram.org/bot$token/sendMessage?chat_id=$chatId&text=✅%20Test%20notification%20from%20KCB%20Rental%20Kiosk!"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                val responseCode = connection.responseCode
                connection.disconnect()
                runOnUiThread {
                    testTelegramBtn.isEnabled = true
                    testTelegramBtn.text = "TEST"
                    if (responseCode == 200) {
                        Toast.makeText(this, "Test sent! Check Telegram.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Failed! Check your Token and Chat ID.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    testTelegramBtn.isEnabled = true
                    testTelegramBtn.text = "TEST"
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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
                    generateBtn.text = "GENERATE PIN"
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
                    extendBtn.text = "EXTEND TIME"
                    Toast.makeText(this@AdminActivity, "Added $minutes minutes (₱$amount) to PIN $pin", Toast.LENGTH_LONG).show()
                    extendPinInput.text.clear()
                    extendMinutesInput.text.clear()
                    extendAmountInput.text.clear()
                    loadPinsCount()
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
                setPadding(15, 12, 15, 12)
                textSize = 13f
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
        saveAppsBtn.text = "SAVE WHITELIST"
        appStatusText.text = "✓ Saved ${selectedPackages.size} apps locally"
        Toast.makeText(this, "Whitelist saved! ${selectedPackages.size} apps", Toast.LENGTH_LONG).show()
    }
}
