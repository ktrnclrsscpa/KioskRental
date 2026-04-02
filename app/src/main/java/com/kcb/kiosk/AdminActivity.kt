package com.kcb.kiosk

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class AdminActivity : AppCompatActivity() {
    private lateinit var supabase: SupabaseClient
    
    // PINS panel
    private lateinit var pinPanel: LinearLayout
    private lateinit var pinListText: TextView
    private lateinit var generatePinInput: EditText
    private lateinit var generateMinutesInput: EditText
    private lateinit var generateBtn: Button
    private lateinit var refreshPinsBtn: Button
    
    // APPS panel
    private lateinit var appPanel: LinearLayout
    private lateinit var appContainer: LinearLayout
    private lateinit var saveAppsBtn: Button
    private lateinit var appStatusText: TextView
    private val checkBoxes = mutableListOf<Pair<CheckBox, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        supabase = SupabaseClient.getInstance()
        
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 50, 30, 30)
        }
        
        // Title
        val title = TextView(this).apply {
            text = "🔐 ADMIN PANEL"
            textSize = 24f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        mainLayout.addView(title)
        
        // Tab buttons
        val tabLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 20)
        }
        
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
        
        mainLayout.addView(tabLayout)
        
        // ========== PINS PANEL ==========
        pinPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
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
            hint = "PIN (leave blank)"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(10, 10, 10, 10)
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
            visibility = android.view.View.GONE
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
        
        val testBtn = Button(this).apply {
            text = "🔌 TEST CONNECTION"
            textSize = 12f
            setOnClickListener { testConnection() }
        }
        appPanel.addView(testBtn)
        
        val scrollView = ScrollView(this)
        appContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10, 10, 10, 10)
        }
        scrollView.addView(appContainer)
        appPanel.addView(scrollView)
        
        // SAVE WHITELIST BUTTON - make sure it's added
        saveAppsBtn = Button(this).apply {
            text = "💾 SAVE WHITELIST"
            setPadding(0, 20, 0, 20)
            setOnClickListener { saveWhitelist() }
        }
        appPanel.addView(saveAppsBtn)
        
        mainLayout.addView(appPanel)
        
        setContentView(mainLayout)
        
        // Load initial data
        loadPins()
        loadInstalledApps()
        loadCurrentWhitelist()
    }
    
    private fun showPinPanel() {
        pinPanel.visibility = android.view.View.VISIBLE
        appPanel.visibility = android.view.View.GONE
        loadPins()
    }
    
    private fun showAppPanel() {
        pinPanel.visibility = android.view.View.GONE
        appPanel.visibility = android.view.View.VISIBLE
        loadInstalledApps()
        loadCurrentWhitelist()
    }
    
    private fun testConnection() {
        appStatusText.text = "Testing connection..."
        CoroutineScope(Dispatchers.IO).launch {
            val result = supabase.testConnection()
            withContext(Dispatchers.Main) {
                if (result) {
                    appStatusText.text = "✓ Connection successful!"
                    appStatusText.setTextColor(android.graphics.Color.GREEN)
                } else {
                    appStatusText.text = "✗ Connection failed!"
                    appStatusText.setTextColor(android.graphics.Color.RED)
                }
            }
        }
    }
    
    private fun loadInstalledApps() {
        appStatusText.text = "Scanning apps..."
        saveAppsBtn.isEnabled = false
        
        val installedApps = mutableListOf<Pair<String, String>>()
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        for (app in packages) {
            if (packageManager.getLaunchIntentForPackage(app.packageName) != null) {
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
        
        appStatusText.text = "Found ${installedApps.size} apps. Select which ones to allow."
        saveAppsBtn.isEnabled = true
    }
    
    private fun loadCurrentWhitelist() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val whitelist = supabase.getWhitelistApps()
                withContext(Dispatchers.Main) {
                    for ((checkBox, packageName) in checkBoxes) {
                        checkBox.isChecked = whitelist.contains(packageName)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    appStatusText.text = "Error loading: ${e.message}"
                }
            }
        }
    }
    
    private fun saveWhitelist() {
        saveAppsBtn.isEnabled = false
        saveAppsBtn.text = "SAVING..."
        
        val selectedPackages = mutableListOf<String>()
        for ((checkBox, packageName) in checkBoxes) {
            if (checkBox.isChecked) {
                selectedPackages.add(packageName)
            }
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = supabase.updateWhitelistApps(selectedPackages)
                withContext(Dispatchers.Main) {
                    saveAppsBtn.isEnabled = true
                    saveAppsBtn.text = "💾 SAVE WHITELIST"
                    if (success) {
                        appStatusText.text = "✓ Success! Saved ${selectedPackages.size} apps"
                        appStatusText.setTextColor(android.graphics.Color.GREEN)
                        Toast.makeText(this@AdminActivity, "Whitelist saved! ${selectedPackages.size} apps", Toast.LENGTH_LONG).show()
                    } else {
                        appStatusText.text = "✗ Failed to save. Check internet connection."
                        appStatusText.setTextColor(android.graphics.Color.RED)
                        Toast.makeText(this@AdminActivity, "Failed to save", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    saveAppsBtn.isEnabled = true
                    saveAppsBtn.text = "💾 SAVE WHITELIST"
                    appStatusText.text = "✗ Error: ${e.message}"
                    appStatusText.setTextColor(android.graphics.Color.RED)
                    Toast.makeText(this@AdminActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun generatePin() {
        val minutes = generateMinutesInput.text.toString().toIntOrNull()
        if (minutes == null || minutes <= 0) {
            Toast.makeText(this, "Enter valid minutes (1-1440)", Toast.LENGTH_SHORT).show()
            return
        }
        
        val customPin = generatePinInput.text.toString().trim()
        val pin = if (customPin.length == 6) customPin else null
        
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
                        sb.append("🔑 ${p.pin} - ${minutes} min left\n")
                    }
                    pinListText.text = sb.toString()
                }
            }
        }
    }
}
