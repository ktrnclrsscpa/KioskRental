package com.kcb.kiosk

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class AdminActivity : AppCompatActivity() {
    private lateinit var supabase: SupabaseClient
    private lateinit var appListRecycler: RecyclerView
    private lateinit var saveAppsBtn: Button
    private lateinit var statusText: TextView
    private var allApps = mutableListOf<AppInfo>()
    private var selectedPackages = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        supabase = SupabaseClient.getInstance()
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 50, 30, 30)
        }
        
        val title = TextView(this).apply {
            text = "🔐 SELECT APPS FOR CUSTOMERS"
            textSize = 22f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        layout.addView(title)
        
        statusText = TextView(this).apply {
            text = "Loading apps..."
            textSize = 12f
            setPadding(0, 0, 0, 10)
        }
        layout.addView(statusText)
        
        appListRecycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@AdminActivity)
            setPadding(0, 0, 0, 20)
            visibility = android.view.View.GONE
        }
        layout.addView(appListRecycler)
        
        saveAppsBtn = Button(this).apply {
            text = "💾 SAVE WHITELIST"
            setOnClickListener { saveWhitelist() }
            visibility = android.view.View.GONE
        }
        layout.addView(saveAppsBtn)
        
        setContentView(layout)
        
        loadInstalledApps()
        loadCurrentWhitelist()
    }
    
    private fun loadInstalledApps() {
        statusText.text = "Scanning apps..."
        statusText.visibility = android.view.View.VISIBLE
        appListRecycler.visibility = android.view.View.GONE
        saveAppsBtn.visibility = android.view.View.GONE
        
        val installedApps = mutableListOf<AppInfo>()
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        for (app in packages) {
            if (packageManager.getLaunchIntentForPackage(app.packageName) != null) {
                val appName = packageManager.getApplicationLabel(app).toString()
                installedApps.add(AppInfo(appName, app.packageName))
            }
        }
        
        installedApps.sortBy { it.name }
        allApps = installedApps
        
        runOnUiThread {
            if (allApps.isEmpty()) {
                statusText.text = "No apps found."
            } else {
                statusText.text = "Found ${allApps.size} apps. Select which ones to allow."
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
}
