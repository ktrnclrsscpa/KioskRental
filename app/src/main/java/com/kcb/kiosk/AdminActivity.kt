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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class AdminActivity : AppCompatActivity() {
    
    private lateinit var supabase: SupabaseClient
    private val prefs by lazy { getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE) }
    
    private lateinit var contentContainer: LinearLayout
    private lateinit var tabBar: LinearLayout
    
    private lateinit var salesContent: LinearLayout
    private lateinit var pinsContent: LinearLayout
    private lateinit var appsContent: LinearLayout
    private lateinit var settingsContent: LinearLayout
    
    private lateinit var dailyTotalText: TextView
    private lateinit var transactionRecycler: RecyclerView
    private lateinit var pinsText: TextView
    private lateinit var appContainer: LinearLayout
    private lateinit var appStatusText: TextView
    private lateinit var saveAppsBtn: Button
    private val checkBoxes = mutableListOf<Pair<CheckBox, String>>()
    private lateinit var telegramTokenInput: EditText
    private lateinit var telegramChatIdInput: EditText
    private lateinit var saveTelegramBtn: Button
    private lateinit var testTelegramBtn: Button
    
    // Class-level references for extend UI elements (used in extendTime)
    private lateinit var extendPinInput: EditText
    private lateinit var extendMinutesInput: EditText
    private lateinit var extendAmountInput: EditText
    private lateinit var extendBtn: Button
    
    private var currentTab = "sales"
    
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
        
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F5F7FA"))
        }
        
        // Header
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(30, 30, 30, 20)
        }
        val title = TextView(this).apply {
            text = "ADMIN PANEL"
            textSize = 22f
            setTextColor(Color.parseColor("#2C3E50"))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerRow.addView(title)
        val changePwdBtn = Button(this).apply {
            text = "Change Password"
            textSize = 12f
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.parseColor("#3498DB"))
            setOnClickListener { showChangePasswordDialog() }
        }
        headerRow.addView(changePwdBtn)
        rootLayout.addView(headerRow)
        
        // Tab bar
        tabBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 0, 20, 0)
            setBackgroundColor(Color.WHITE)
            elevation = 4f
        }
        
        val salesTab = TextView(this).apply {
            text = "SALES"
            textSize = 13f
            setTextColor(Color.parseColor("#7F8C8D"))
            setPadding(20, 15, 20, 15)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            gravity = android.view.Gravity.CENTER
            tag = "sales"
        }
        val pinsTab = TextView(this).apply {
            text = "PINS"
            textSize = 13f
            setTextColor(Color.parseColor("#7F8C8D"))
            setPadding(20, 15, 20, 15)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            gravity = android.view.Gravity.CENTER
            tag = "pins"
        }
        val appsTab = TextView(this).apply {
            text = "APPS"
            textSize = 13f
            setTextColor(Color.parseColor("#7F8C8D"))
            setPadding(20, 15, 20, 15)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            gravity = android.view.Gravity.CENTER
            tag = "apps"
        }
        val settingsTab = TextView(this).apply {
            text = "SETTINGS"
            textSize = 13f
            setTextColor(Color.parseColor("#7F8C8D"))
            setPadding(20, 15, 20, 15)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            gravity = android.view.Gravity.CENTER
            tag = "settings"
        }
        
        tabBar.addView(salesTab)
        tabBar.addView(pinsTab)
        tabBar.addView(appsTab)
        tabBar.addView(settingsTab)
        
        // Content container
        contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        
        rootLayout.addView(tabBar)
        rootLayout.addView(contentContainer)
        
        // Create content for each tab
        salesContent = createSalesContent()
        pinsContent = createPinsContent()
        appsContent = createAppsContent()
        settingsContent = createSettingsContent()
        
        // Set click listeners
        salesTab.setOnClickListener { selectTab("sales") }
        pinsTab.setOnClickListener { selectTab("pins") }
        appsTab.setOnClickListener { selectTab("apps") }
        settingsTab.setOnClickListener { selectTab("settings") }
        
        setContentView(rootLayout)
        
        selectTab("sales")
        
        loadInstalledApps()
        loadCurrentWhitelistLocal()
        loadTelegramSettings()
    }
    
    private fun selectTab(tab: String) {
        currentTab = tab
        for (i in 0 until tabBar.childCount) {
            val child = tabBar.getChildAt(i) as? TextView ?: continue
            if (child.tag == tab) {
                child.setTextColor(Color.parseColor("#2ECC71"))
                child.setTypeface(Typeface.DEFAULT_BOLD)
            } else {
                child.setTextColor(Color.parseColor("#7F8C8D"))
                child.setTypeface(Typeface.DEFAULT)
            }
        }
        contentContainer.removeAllViews()
        when (tab) {
            "sales" -> contentContainer.addView(salesContent)
            "pins" -> contentContainer.addView(pinsContent)
            "apps" -> contentContainer.addView(appsContent)
            "settings" -> contentContainer.addView(settingsContent)
        }
        when (tab) {
            "sales" -> loadTransactionHistory()
            "pins" -> loadActivePins()
        }
    }
    
    private fun createSalesContent(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            
            dailyTotalText = TextView(this@AdminActivity).apply {
                text = "Daily Total: ₱0.00"
                textSize = 16f
                setPadding(16, 12, 16, 12)
                background = createCardBackground()
            }
            addView(dailyTotalText)
            
            val historyTitle = TextView(this@AdminActivity).apply {
                text = "📋 TRANSACTION HISTORY"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 20, 0, 10)
            }
            addView(historyTitle)
            
            transactionRecycler = RecyclerView(this@AdminActivity).apply {
                layoutManager = LinearLayoutManager(this@AdminActivity)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            }
            addView(transactionRecycler)
            
            val refreshBtn = Button(this@AdminActivity).apply {
                text = "REFRESH"
                setBackgroundColor(Color.parseColor("#3498DB"))
                setTextColor(Color.WHITE)
                setOnClickListener { loadTransactionHistory() }
            }
            addView(refreshBtn)
            
            val exportBtn = Button(this@AdminActivity).apply {
                text = "📥 EXPORT CSV REPORT"
                setBackgroundColor(Color.parseColor("#9B59B6"))
                setTextColor(Color.WHITE)
                setPadding(16, 14, 16, 14)
                setOnClickListener { exportReport() }
            }
            addView(exportBtn)
        }
    }
    
    private fun createPinsContent(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            
            // Generate PIN section
            val genTitle = TextView(this@AdminActivity).apply {
                text = "📌 Generate New PIN"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 10)
            }
            addView(genTitle)
            
            val generatePinInput = createEditText("PIN (leave blank for random)")
            addView(generatePinInput)
            val generateMinutesInput = createEditText("Minutes", true)
            addView(generateMinutesInput)
            val generateAmountInput = createEditText("Amount (₱)", true)
            generateAmountInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            addView(generateAmountInput)
            
            val generateBtn = createButton("GENERATE PIN", "#2ECC71")
            generateBtn.setOnClickListener {
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
                            loadActivePins()
                            loadTransactionHistory()
                        } else {
                            Toast.makeText(this@AdminActivity, "Failed to generate PIN", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            addView(generateBtn)
            
            // Extend PIN section
            val extendTitle = TextView(this@AdminActivity).apply {
                text = "⏰ Extend Active PIN"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 20, 0, 10)
            }
            addView(extendTitle)
            
            extendPinInput = createEditText("PIN to extend")
            addView(extendPinInput)
            extendMinutesInput = createEditText("Minutes to add", true)
            addView(extendMinutesInput)
            extendAmountInput = createEditText("Amount (₱)", true)
            extendAmountInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            addView(extendAmountInput)
            
            extendBtn = createButton("EXTEND TIME", "#E67E22")
            extendBtn.setOnClickListener {
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
                extendTime(pin, minutes, amount)
            }
            addView(extendBtn)
            
            // Active PINs list
            val pinsTitle = TextView(this@AdminActivity).apply {
                text = "🔑 Active PINs"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 20, 0, 10)
            }
            addView(pinsTitle)
            
            val pinsScrollView = ScrollView(this@AdminActivity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            }
            pinsText = TextView(this@AdminActivity).apply {
                text = "Loading..."
                textSize = 14f
                setPadding(16, 12, 16, 12)
                background = createCardBackground()
            }
            pinsScrollView.addView(pinsText)
            addView(pinsScrollView)
            
            val refreshPinsBtn = Button(this@AdminActivity).apply {
                text = "REFRESH PIN LIST"
                setBackgroundColor(Color.parseColor("#3498DB"))
                setTextColor(Color.WHITE)
                setOnClickListener { loadActivePins() }
            }
            addView(refreshPinsBtn)
        }
    }
    
    private fun createAppsContent(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            
            val whitelistTitle = TextView(this@AdminActivity).apply {
                text = "📱 App Whitelist"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 10)
            }
            addView(whitelistTitle)
            
            appStatusText = TextView(this@AdminActivity).apply {
                text = "Loading apps..."
                textSize = 13f
                setPadding(0, 0, 0, 12)
                setTextColor(Color.parseColor("#7F8C8D"))
            }
            addView(appStatusText)
            
            val appScrollView = ScrollView(this@AdminActivity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            }
            appContainer = LinearLayout(this@AdminActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(8, 8, 8, 8)
            }
            appScrollView.addView(appContainer)
            addView(appScrollView)
            
            saveAppsBtn = createButton("SAVE WHITELIST", "#3498DB")
            saveAppsBtn.setOnClickListener { saveWhitelistLocal() }
            addView(saveAppsBtn)
        }
    }
    
    private fun createSettingsContent(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            
            val settingsTitle = TextView(this@AdminActivity).apply {
                text = "⚙️ Telegram Settings"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 10)
            }
            addView(settingsTitle)
            
            telegramTokenInput = createEditText("Bot Token")
            addView(telegramTokenInput)
            
            telegramChatIdInput = createEditText("Chat ID")
            addView(telegramChatIdInput)
            
            val buttonRow = LinearLayout(this@AdminActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 10, 0, 10)
            }
            
            saveTelegramBtn = Button(this@AdminActivity).apply {
                text = "SAVE"
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(0, 0, 10, 0)
                }
                setBackgroundColor(Color.parseColor("#2ECC71"))
                setTextColor(Color.WHITE)
                setAllCaps(false)
                setOnClickListener {
                    val token = telegramTokenInput.text.toString().trim()
                    val chatId = telegramChatIdInput.text.toString().trim()
                    CoroutineScope(Dispatchers.IO).launch {
                        val success = supabase.updateTelegramConfig(token, chatId)
                        withContext(Dispatchers.Main) {
                            if (success) {
                                Toast.makeText(this@AdminActivity, "Telegram settings saved!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@AdminActivity, "Failed to save", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            buttonRow.addView(saveTelegramBtn)
            
            testTelegramBtn = Button(this@AdminActivity).apply {
                text = "TEST"
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setBackgroundColor(Color.parseColor("#E67E22"))
                setTextColor(Color.WHITE)
                setAllCaps(false)
                setOnClickListener {
                    val token = telegramTokenInput.text.toString().trim()
                    val chatId = telegramChatIdInput.text.toString().trim()
                    if (token.isEmpty() || chatId.isEmpty()) {
                        Toast.makeText(this@AdminActivity, "Enter Bot Token and Chat ID first", Toast.LENGTH_SHORT).show()
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
                                    Toast.makeText(this@AdminActivity, "Test sent! Check Telegram.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this@AdminActivity, "Failed! Check credentials.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                testTelegramBtn.isEnabled = true
                                testTelegramBtn.text = "TEST"
                                Toast.makeText(this@AdminActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }.start()
                }
            }
            buttonRow.addView(testTelegramBtn)
            addView(buttonRow)
        }
    }
    
    // ==================== CORRECTED EXTEND TIME WITH DEBUG TOASTS ====================
    private fun extendTime(pin: String, minutes: Int, amount: Double) {
        extendBtn.isEnabled = false
        extendBtn.text = "EXTENDING..."
        
        CoroutineScope(Dispatchers.IO).launch {
            // Get current seconds BEFORE update
            val beforeResult = supabase.validatePin(pin)
            val beforeSeconds = beforeResult.secondsLeft
            
            val success = supabase.extendTime(pin, minutes)
            if (success) {
                val afterResult = supabase.validatePin(pin)
                val afterSeconds = afterResult.secondsLeft
                supabase.recordExtension(pin, minutes, amount)
                supabase.sendTelegramNotification("⏰ *Session Extended!*%0APIN: $pin%0AAdded: $minutes minutes%0APayment: ₱${String.format("%.2f", amount)}")
                withContext(Dispatchers.Main) {
                    extendBtn.isEnabled = true
                    extendBtn.text = "EXTEND TIME"
                    // Debug toast showing before/after
                    Toast.makeText(this@AdminActivity, "PIN: $pin\nBefore: ${beforeSeconds}s → After: ${afterSeconds}s\nExpected +${minutes*60}s", Toast.LENGTH_LONG).show()
                    extendPinInput.text.clear()
                    extendMinutesInput.text.clear()
                    extendAmountInput.text.clear()
                    loadPinsCount()
                    loadActivePins()
                    loadTransactionHistory()
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
    
    // ==================== OTHER METHODS ====================
    private fun loadTransactionHistory() {
        CoroutineScope(Dispatchers.IO).launch {
            val history = supabase.getSessionHistory()
            withContext(Dispatchers.Main) {
                val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
                val today = dateFormat.format(Date())
                var dailyTotal = 0.0
                val displayList = mutableListOf<String>()
                for (record in history) {
                    val type = if (record.pin.endsWith("_EXT")) "🔁 EXTENSION" else "🎮 SESSION"
                    val cleanPin = record.pin.replace("_EXT", "")
                    val detail = "$type: $cleanPin | ${record.minutes} min | ₱${String.format("%.2f", record.amount)} | ${record.date}"
                    displayList.add(detail)
                    val recordDate = record.date.split(" ")[0]
                    if (recordDate == today) dailyTotal += record.amount
                }
                dailyTotalText.text = "Daily Total (Today): ₱${String.format("%.2f", dailyTotal)}"
                transactionRecycler.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                        val tv = TextView(parent.context).apply {
                            setPadding(16, 12, 16, 12)
                            textSize = 14f
                            setTextColor(Color.BLACK)
                        }
                        return object : RecyclerView.ViewHolder(tv) {}
                    }
                    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                        (holder.itemView as TextView).text = displayList[position]
                    }
                    override fun getItemCount(): Int = displayList.size
                }
            }
        }
    }
    
    private fun loadActivePins() {
        CoroutineScope(Dispatchers.IO).launch {
            val pins = supabase.getActivePins()
            withContext(Dispatchers.Main) {
                if (pins.isEmpty()) pinsText.text = "No active PINs"
                else {
                    val sb = StringBuilder()
                    for (p in pins) sb.append("🔑 ${p.pin} - ${p.secondsLeft / 60} min remaining\n")
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
            val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
            val today = dateFormat.format(Date())
            var dailyTotal = 0.0
            val csv = StringBuilder()
            csv.append("DAILY TOTAL (Today):,₱0.00\n\n")
            csv.append("PIN,Minutes,Amount,Date,Type\n")
            for (record in history) {
                val type = if (record.pin.endsWith("_EXT")) "EXTENSION" else "SESSION"
                val cleanPin = record.pin.replace("_EXT", "")
                csv.append("$cleanPin,${record.minutes},${record.amount},${record.date},$type\n")
                val recordDate = record.date.split(" ")[0]
                if (recordDate == today) dailyTotal += record.amount
            }
            val finalCsv = "DAILY TOTAL (Today):,₱${String.format("%.2f", dailyTotal)}\n\n${csv.substring(csv.indexOf("\n") + 1)}"
            withContext(Dispatchers.Main) {
                try {
                    val file = File(cacheDir, "sales_report.csv")
                    file.writeText(finalCsv)
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
    
    private fun loadPinsCount() {
        CoroutineScope(Dispatchers.IO).launch {
            val pins = supabase.getActivePins()
            withContext(Dispatchers.Main) {
                // update stats if needed
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
    
    // Helper UI methods
    private fun createCardBackground(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = 12f
            setStroke(1, Color.parseColor("#EEEEEE"))
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
            ).apply { setMargins(0, 0, 0, 12) }
            if (isNumber) inputType = android.text.InputType.TYPE_CLASS_NUMBER
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
            ).apply { setMargins(0, 5, 0, 5) }
        }
    }
}
