package com.kcb.kiosk

import android.content.Context
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
    
    // Local storage for whitelist
    private val prefs by lazy { getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        supabase = SupabaseClient.getInstance()
        
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 50, 30, 30)
        }
        
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
        
        val scrollView = ScrollView(this)
        appContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10, 10, 10, 10)
        }
        scrollView.addView(appContainer)
        
        val scrollContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
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
        
        setContentView(mainLayout)
        
        loadPins()
        loadInstalledApps()
        loadCurrentWhitelistLocal()
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
        loadCurrentWhitelistLocal()
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
            if (checkBox.isChecked) {
                selectedPackages.add(packageName)
            }
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
