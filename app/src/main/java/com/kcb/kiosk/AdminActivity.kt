package com.kcb.kiosk

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        val rvAdminApps = findViewById<RecyclerView>(R.id.rvAdminApps)
        val btnSave = findViewById<Button>(R.id.btnSaveWhitelist)

        // Kunin lahat ng Installed Launcher Apps
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val allApps = packageManager.queryIntentActivities(mainIntent, 0)

        // Pag-aayos ng listahan alphabetically
        val sortedApps = allApps.sortedBy { it.loadLabel(packageManager).toString() }

        // Kunin ang kasalukuyang whitelist mula sa SharedPrefs
        val sharedPrefs = getSharedPreferences("KCB_SETTINGS", Context.MODE_PRIVATE)
        val currentWhitelist = sharedPrefs.getStringSet("allowed_apps", mutableSetOf()) ?: mutableSetOf()

        val adapter = AdminAppAdapter(sortedApps, currentWhitelist.toMutableSet(), packageManager)
        rvAdminApps.layoutManager = LinearLayoutManager(this)
        rvAdminApps.adapter = adapter

        btnSave.setOnClickListener {
            val selectedApps = adapter.getSelectedApps()
            sharedPrefs.edit().putStringSet("allowed_apps", selectedApps).apply()
            
            // Bumalik sa MainActivity para mag-apply ang changes
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}