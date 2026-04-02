package com.kcb.kiosk

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class AdminActivity : AppCompatActivity() {
    private lateinit var supabase: SupabaseClient
    private lateinit var container: LinearLayout
    private lateinit var saveBtn: Button
    private lateinit var statusText: TextView
    private lateinit var errorText: TextView
    private val checkBoxes = mutableListOf<Pair<CheckBox, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        supabase = SupabaseClient.getInstance()
        
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 50, 30, 30)
        }
        
        val title = TextView(this).apply {
            text = "🔐 SELECT APPS FOR CUSTOMERS"
            textSize = 22f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        mainLayout.addView(title)
        
        statusText = TextView(this).apply {
            text = "Loading apps..."
            textSize = 12f
            setPadding(0, 0, 0, 10)
        }
        mainLayout.addView(statusText)
        
        errorText = TextView(this).apply {
            text = ""
            textSize = 12f
            setTextColor(android.graphics.Color.RED)
            setPadding(0, 0, 0, 10)
        }
        mainLayout.addView(errorText)
        
        val scrollView = ScrollView(this)
        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10, 10, 10, 10)
        }
        scrollView.addView(container)
        mainLayout.addView(scrollView)
        
        saveBtn = Button(this).apply {
            text = "💾 SAVE WHITELIST"
            setOnClickListener { saveWhitelist() }
        }
        mainLayout.addView(saveBtn)
        
        setContentView(mainLayout)
        
        loadInstalledApps()
        loadCurrentWhitelist()
    }
    
    private fun loadInstalledApps() {
        statusText.text = "Scanning apps..."
        saveBtn.isEnabled = false
        
        val installedApps = mutableListOf<Pair<String, String>>()
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        for (app in packages) {
            if (packageManager.getLaunchIntentForPackage(app.packageName) != null) {
                val appName = packageManager.getApplicationLabel(app).toString()
                installedApps.add(Pair(appName, app.packageName))
            }
        }
        
        installedApps.sortBy { it.first }
        
        container.removeAllViews()
        checkBoxes.clear()
        
        for (app in installedApps) {
            val checkBox = CheckBox(this).apply {
                text = "${app.first}\n(${app.second})"
                setPadding(10, 15, 10, 15)
                textSize = 14f
            }
            container.addView(checkBox)
            checkBoxes.add(Pair(checkBox, app.second))
        }
        
        statusText.text = "Found ${installedApps.size} apps. Select which ones to allow."
        saveBtn.isEnabled = true
    }
    
    private fun loadCurrentWhitelist() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val whitelist = supabase.getWhitelistApps()
                withContext(Dispatchers.Main) {
                    for ((checkBox, packageName) in checkBoxes) {
                        checkBox.isChecked = whitelist.contains(packageName)
                    }
                    errorText.text = "✓ Loaded whitelist from server"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorText.text = "Error loading: ${e.message}"
                }
            }
        }
    }
    
    private fun saveWhitelist() {
        saveBtn.isEnabled = false
        saveBtn.text = "SAVING..."
        errorText.text = ""
        
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
                    saveBtn.isEnabled = true
                    saveBtn.text = "💾 SAVE WHITELIST"
                    if (success) {
                        errorText.text = "✓ Success! Saved ${selectedPackages.size} apps"
                        Toast.makeText(this@AdminActivity, "Whitelist saved! ${selectedPackages.size} apps", Toast.LENGTH_LONG).show()
                    } else {
                        errorText.text = "✗ Failed to save. Check internet connection."
                        Toast.makeText(this@AdminActivity, "Failed to save", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    saveBtn.isEnabled = true
                    saveBtn.text = "💾 SAVE WHITELIST"
                    errorText.text = "✗ Error: ${e.message}"
                    Toast.makeText(this@AdminActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
