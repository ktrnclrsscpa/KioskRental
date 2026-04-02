package com.kcb.kiosk

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class AdminActivity : AppCompatActivity() {
    private lateinit var supabase: SupabaseClient
    private lateinit var pinListText: TextView
    private lateinit var generatePinInput: EditText
    private lateinit var generateMinutesInput: EditText
    private lateinit var generateBtn: Button
    private lateinit var refreshBtn: Button
    private lateinit var appListRecycler: RecyclerView
    private lateinit var saveAppsBtn: Button
    private lateinit var pinPanel: LinearLayout
    private lateinit var appPanel: LinearLayout
    private lateinit var appStatusText: TextView
    private var allApps = mutableListOf<AppInfo>()
    private var selectedPackages = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        supabase = SupabaseClient.getInstance()
        
        // Main layout
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
        
        val pinTab = Button(this).apply {
            text = "📋 PINS"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { showPinPanel() }
        }
        tabLayout.addView(pinTab)
        
        val appTab = Button(this).apply {
            text = "📱 APPS"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { showAppPanel() }
        }
        tabLayout.addView(appTab)
        
        mainLayout.addView(tabLayout)
        
        // PIN Panel
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
        
        refreshBtn = Button(this).apply {
            text = "🔄 REFRESH LIST"
            setPadding(0, 20, 0, 20)
            setOnClickListener { loadPins() }
        }
        pinPanel.addView(refreshBtn)
        
        pinListText = TextView(this).apply {
            text = "Loading PINs..."
            textSize = 14f
            setPadding(0, 20, 0, 0)
        }
        pinPanel.addView(pinListText)
        
        mainLayout.addView(pinPanel)
        
        // APP Panel
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
        
        val infoText = TextView(this).apply {
            text = "Check the apps you want customers to access"
            textSize = 12f
            setPadding(0, 0, 0, 10)
        }
        appPanel.addView(infoText)
        
        appStatusText = TextView(this).apply {
            text = "Loading apps..."
            textSize = 12f
            setPadding(0, 0, 0, 10)
        }
        appPanel.addView(appStatusText)
        
        appListRecycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@AdminActivity)
            setPadding(0, 0, 0, 20)
            visibility = android.view.View.GONE
        }
        appPanel.addView(appListRecycler)
        
        saveAppsBtn = Button(this).apply {
            text = "💾 SAVE WHITELIST"
            setOnClickListener { saveWhitelist() }
            visibility = android.view.View.GONE
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
        // Refresh app list when showing
        loadInstalledApps()
        loadCurrentWhitelist()
    }
    
    private fun loadInstalledApps() {
        appStatusText.text = "Scanning apps..."
        appStatusText.visibility = android.view.View.VISIBLE
        appListRecycler.visibility = android.view.View.GONE
        saveAppsBtn.visibility = android.view.View.GONE
        
        val installedApps = mutableListOf<AppInfo>()
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        for (app in packages) {
            if (packageManager.getLaunchIntentForPackage(app.packageName) != null) {
                installedApps.add(AppInfo(
                    name = packageManager.getApplicationLabel(app).toString(),
                    packageName = app.packageName
                ))
            }
        }
        
        installedApps.sortBy { it.name }
        allApps = installedApps
        
        runOnUiThread {
            if (allApps.isEmpty()) {
                appStatusText.text = "No apps found. Make sure you have apps installed."
            } else {
                appStatusText.text = "Found ${allApps.size} apps. Select which ones to allow."
                appListRecycler.visibility = android.view.View.VISIBLE
                saveAppsBtn.visibility = android.view.View.VISIBLE
                appListRecycler.adapter = AppSelectionAdapter(allApps, selectedPackages) { updatedSelection ->
                    selectedPackages.clear()
                    selectedPackages.addAll(updatedSelection)
                }
            }
        }
    }
    
    private fun loadCurrentWhitelist() {
        CoroutineScope(Dispatchers.IO).launch {
            val whitelist = supabase.getWhitelistApps()
            withContext(Dispatchers.Main) {
                selectedPackages.clear()
                selectedPackages.addAll(whitelist)
                (appListRecycler.adapter as? AppSelectionAdapter)?.updateSelection(selectedPackages.toList())
            }
        }
    }
    
    private fun saveWhitelist() {
        saveAppsBtn.isEnabled = false
        saveAppsBtn.text = "SAVING..."
        
        CoroutineScope(Dispatchers.IO).launch {
            val success = supabase.updateWhitelistApps(selectedPackages.toList())
            withContext(Dispatchers.Main) {
                saveAppsBtn.isEnabled = true
                saveAppsBtn.text = "💾 SAVE WHITELIST"
                if (success) {
                    Toast.makeText(this@AdminActivity, "Whitelist saved!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AdminActivity, "Failed to save", Toast.LENGTH_SHORT).show()
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
