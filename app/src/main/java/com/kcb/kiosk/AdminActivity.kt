package com.kcb.kiosk

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class AdminActivity : AppCompatActivity() {
    private lateinit var appListRecycler: RecyclerView
    private lateinit var statusText: TextView
    private var allApps = mutableListOf<AppInfo>()
    private var selectedPackages = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 50, 30, 30)
        }
        
        // Title
        val title = TextView(this).apply {
            text = "🔐 SELECT APPS FOR CUSTOMERS"
            textSize = 20f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        layout.addView(title)
        
        // Status text
        statusText = TextView(this).apply {
            text = "Loading apps..."
            textSize = 14f
            setPadding(0, 0, 0, 20)
        }
        layout.addView(statusText)
        
        // Recycler for apps
        appListRecycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@AdminActivity)
            setPadding(0, 0, 0, 20)
        }
        layout.addView(appListRecycler)
        
        setContentView(layout)
        
        // Load apps
        loadInstalledApps()
    }
    
    private fun loadInstalledApps() {
        statusText.text = "Scanning apps..."
        
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
                statusText.text = "No apps found."
            } else {
                statusText.text = "Found ${allApps.size} apps. Select which ones to allow."
                appListRecycler.adapter = AppSelectionAdapter(allApps, selectedPackages) { updatedSelection ->
                    selectedPackages.clear()
                    selectedPackages.addAll(updatedSelection)
                    // Optional: save automatically
                }
            }
        }
    }
}
