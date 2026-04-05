package com.kcb.kiosk

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
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
    private lateinit var contentContainer: LinearLayout
    private var currentSection = "dashboard"
    
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
        
        // Main container with horizontal layout (sidebar + content)
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        // ========== SIDEBAR ==========
        val sidebar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 40, 20, 30)
            setBackgroundColor(Color.parseColor("#1A2C3E"))
            layoutParams = LinearLayout.LayoutParams(240, LinearLayout.LayoutParams.MATCH_PARENT)
        }
        
        val appTitle = TextView(this).apply {
            text = "KCB RENTAL"
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 40)
        }
        sidebar.addView(appTitle)
        
        // Menu Items
        val menuItems = listOf(
            Triple("📊", "Dashboard", "dashboard"),
            Triple("🔑", "PIN Management", "pin"),
            Triple("📱", "App Whitelist", "apps"),
            Triple("⚙️", "Settings", "settings")
        )
        
        for ((icon, title, id) in menuItems) {
            val menuItem = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 16, 16, 16)
                gravity = android.view.Gravity.CENTER_VERTICAL
                setBackgroundColor(if (currentSection == id) Color.parseColor("#2C3E50") else Color.TRANSPARENT)
                setOnClickListener {
                    currentSection = id
                    updateContent()
                    // Update selected background
                    for (i in 0 until sidebar.childCount) {
                        val child = sidebar.getChildAt(i)
                        if (child is LinearLayout && child.tag is String) {
                            child.setBackgroundColor(if (child.tag == id) Color.parseColor("#2C3E50") else Color.TRANSPARENT)
                        }
                    }
                }
                tag = id
            }
            
            val iconText = TextView(this).apply {
                text = icon
                textSize = 20f
                setTextColor(Color.parseColor("#A0B0C0"))
                setPadding(0, 0, 16, 0)
            }
            menuItem.addView(iconText)
            
            val titleText = TextView(this).apply {
                text = title
                textSize = 15f
                setTextColor(Color.parseColor("#A0B0C0"))
            }
            menuItem.addView(titleText)
            
            sidebar.addView(menuItem)
        }
        
        // Spacer
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        sidebar.addView(spacer)
        
        // Change Password at bottom
        val changePwdItem = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
            gravity = android.view.Gravity.CENTER_VERTICAL
            setOnClickListener { showChangePasswordDialog() }
        }
        
        val changePwdIcon = TextView(this).apply {
            text = "🔐"
            textSize = 18f
            setTextColor(Color.parseColor("#A0B0C0"))
            setPadding(0, 0, 16, 0)
        }
        changePwdItem.addView(changePwdIcon)
        
        val changePwdText = TextView(this).apply {
            text = "Change Password"
            textSize = 14f
            setTextColor(Color.parseColor("#A0B0C0"))
        }
        changePwdItem.addView(changePwdText)
        
        sidebar.addView(changePwdItem)
        
        // ========== CONTENT AREA ==========
        val scrollView = ScrollView(this)
        contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 50)
            setBackgroundColor(Color.parseColor("#F5F7FA"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        scrollView.addView(contentContainer)
        
        rootLayout.addView(sidebar)
        rootLayout.addView(scrollView, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        
        setContentView(rootLayout)
        
        // Load initial data
        loadDashboardStats()
        loadPinsCount()
        loadInstalledApps()
        loadCurrentWhitelistLocal()
        loadTelegramSettings()
        updateContent()
    }
    
    private fun updateContent() {
        contentContainer.removeAllViews()
        when (currentSection) {
            "dashboard" -> showDashboardContent()
            "pin" -> showPinManagementContent()
            "apps" -> showAppWhitelistContent()
            "settings" -> showSettingsContent()
        }
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
            typeface = Typeface.DEFAULT_BOLD
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
    
    private fun createSection(title: String): LinearLayout {
        val section = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 20)
        }
        
        val titleText = TextView(this).apply {
            text = title
            textSize = 20f
            setTextColor(Color.parseColor("#2C3E50"))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 15)
        }
        section.addView(titleText)
        
        return section
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
    
    private fun createWhiteCard(content: View): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
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
    
    private fun showDashboardContent() {
        // Stats Row
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
        contentContainer.addView(statsRow)
        
        // Recent Sessions
        val sessionsSection = createSection("Recent Sessions")
        
        sessionHistoryRecycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@AdminActivity)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 400)
        }
        sessionsSection.addView(sessionHistoryRecycler)
        
        contentContainer.addView(createWhiteCard(sessionsSection))
        
        loadDashboardStats()
    }
    
    private fun showPinManagementContent() {
        // Generate PIN Section
        val generateSection = createSection("Generate New PIN")
        
        val generateCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        generatePinInput = createInputField("PIN (leave blank for random)")
        generateCard.addView(generatePinInput)
        
        generateMinutesInput = createInputField("Minutes", true)
        generateCard.addView(generateMinutesInput)
        
        generateAmountInput = createInputField("Amount (₱)", true)
        generateAmountInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        generateCard.addView(generateAmountInput)
        
        generateBtn = createButton("GENERATE PIN", "#2ECC71") { generatePin() }
        generateCard.addView(generateBtn)
        
        generateSection.addView(createWhiteCard(generateCard))
        contentContainer.addView(generateSection)
        
        // Extend PIN Section
        val extendSection = createSection("Extend Active PIN")
        
        val extendCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        extendPinInput = createInputField("PIN to extend")
        extendCard.addView(extendPinInput)
        
        extendMinutesInput = createInputField("Minutes to add", true)
        extendCard.addView(extendMinutesInput)
        
        extendAmountInput = createInputField("Amount (₱)", true)
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
        
        extendSection.addView(createWhiteCard(extendCard))
        contentContainer.addView(extendSection)
        
        // Active PINs List
        val pinsSection = createSection("Active PINs")
        val pinsText = TextView(this).apply {
            text = "Loading..."
            textSize = 14f
            setPadding(16, 12, 16, 12)
            setTextColor(Color.parseColor("#666666"))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F5F7FA"))
                cornerRadius = 12f
            }
        }
        pinsSection.addView(pinsText)
        contentContainer.addView(createWhiteCard(pinsSection))
        
        loadActivePins(pinsText)
    }
    
    private fun showAppWhitelistContent() {
        val whitelistSection = createSection("App Whitelist")
        
        val whitelistCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        appStatusText = TextView(this).apply {
            text = "Loading apps..."
            textSize = 13f
            setPadding(0, 0, 0, 12)
            setTextColor(Color.parseColor("#7F8C8D"))
        }
        whitelistCard.addView(appStatusText)
        
        val appScrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 350)
        }
        appContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
        }
        appScrollView.addView(appContainer)
        whitelistCard.addView(appScrollView)
        
        saveAppsBtn = createButton("SAVE WHITELIST", "#3498DB") { saveWhitelistLocal() }
        whitelistCard.addView(saveAppsBtn)
        
        whitelistSection.addView(createWhiteCard(whitelistCard))
        contentContainer.addView(whitelistSection)
        
        loadInstalledApps()
        loadCurrentWhitelistLocal()
    }
    
    private fun showSettingsContent() {
        val settingsSection = createSection("Settings")
        
        val settingsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        val telegramLabel = TextView(this).apply {
            text = "Telegram Bot Configuration"
            textSize = 15f
            setTextColor(Color.parseColor("#2C3E50"))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 12)
        }
        settingsCard.addView(telegramLabel)
        
        telegramTokenInput = createInputField("Bot Token")
        settingsCard.addView(telegramTokenInput)
        
        telegramChatIdInput = createInputField("Chat ID")
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
        
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20)
        }
        settingsCard.addView(spacer)
        
        exportReportBtn = createButton("EXPORT CSV REPORT", "#9B59B6") { exportReport() }
        settingsCard.addView(exportReportBtn)
        
        settingsSection.addView(createWhiteCard(settingsCard))
        contentContainer.addView(settingsSection)
        
        loadTelegramSettings()
    }
    
    private fun loadActivePins(pinsText: TextView) {
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
                    loadPinsCount()
                    updateContent()
                } else {
                    Toast.makeText(this@AdminActivity, "Failed to generate PIN", Toast.LENGTH_SHORT).show()
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
                supabase.sendTelegramNotification("⏰ *Session Extended!*%0APIN: $pin%0AAdded: $minutes minutes%0APayment: ₱${String.format("%.2f", amount)}")
                withContext(Dispatchers.Main) {
                    extendBtn.isEnabled = true
                    extendBtn.text = "EXTEND TIME"
                    Toast.makeText(this@AdminActivity, "Added $minutes minutes (₱$amount) to PIN $pin", Toast.LENGTH_LONG).show()
                    extendPinInput.text.clear()
                    extendMinutesInput.text.clear()
                    extendAmountInput.text.clear()
                    loadPinsCount()
                    updateContent()
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
}
