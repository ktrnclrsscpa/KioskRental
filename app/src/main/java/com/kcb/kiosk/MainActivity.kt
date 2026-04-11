package com.kcb.kiosk

import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var layoutLock: LinearLayout
    private lateinit var layoutUnlocked: LinearLayout
    private lateinit var tvTimer: TextView
    private lateinit var rvApps: RecyclerView
    private lateinit var etPin: EditText
    private var isUnlocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        layoutLock = findViewById(R.id.layoutLock)
        layoutUnlocked = findViewById(R.id.layoutUnlocked)
        tvTimer = findViewById(R.id.tvTimer)
        rvApps = findViewById(R.id.rvApps)
        etPin = findViewById(R.id.etPin)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnAdmin = findViewById<TextView>(R.id.btnAdmin)

        btnStart.setOnClickListener {
            val pin = etPin.text.toString().trim()
            if (pin == "123456") { // Gpalitan mo ng Supabase logic mo dito
                unlockDevice(300) 
            } else {
                Toast.makeText(this, "Invalid PIN", Toast.LENGTH_SHORT).show()
            }
        }

        btnAdmin.setOnClickListener {
            showAdminLoginDialog()
        }

        setupAppGrid()
    }

    override fun onResume() {
        super.onResume()
        setupAppGrid() // Refresh grid para lumitaw ang changes mula Admin
    }

    override fun onPause() {
        super.onPause()
        if (!isUnlocked) {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.moveTaskToFront(taskId, 0)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && !isUnlocked) {
            val closeDialog = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            sendBroadcast(closeDialog)
        }
    }

    override fun onBackPressed() {
        // Disabled back button
    }

    private fun showAdminLoginDialog() {
        val adminEditText = EditText(this)
        adminEditText.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        
        AlertDialog.Builder(this)
            .setTitle("Admin Access")
            .setMessage("Enter Admin PIN to manage apps")
            .setView(adminEditText)
            .setPositiveButton("Login") { _, _ ->
                if (adminEditText.text.toString() == "2026") {
                    startActivity(Intent(this, AdminActivity::class.java))
                } else {
                    Toast.makeText(this, "Wrong PIN!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun unlockDevice(seconds: Long) {
        isUnlocked = true
        layoutLock.visibility = View.GONE
        layoutUnlocked.visibility = View.VISIBLE
        startTimer(seconds)
    }

    private fun startTimer(seconds: Long) {
        var timeLeft = seconds
        lifecycleScope.launch {
            while (timeLeft > 0) {
                val mins = timeLeft / 60
                val secs = timeLeft % 60
                tvTimer.text = String.format("%02d:%02d", mins, secs)
                delay(1000)
                timeLeft--
            }
            isUnlocked = false
            layoutLock.visibility = View.VISIBLE
            layoutUnlocked.visibility = View.GONE
        }
    }

    private fun setupAppGrid() {
        val sharedPrefs = getSharedPreferences("KCB_SETTINGS", Context.MODE_PRIVATE)
        val allowedPackages = sharedPrefs.getStringSet("allowed_apps", emptySet()) ?: emptySet()

        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val pkgAppsList = packageManager.queryIntentActivities(mainIntent, 0)

        val filteredApps = pkgAppsList.filter { 
            allowedPackages.isEmpty() || allowedPackages.contains(it.activityInfo.packageName)
        }.map { 
            AppInfo(it.loadLabel(packageManager).toString(), it.activityInfo.packageName, it.loadIcon(packageManager)) 
        }

        rvApps.layoutManager = GridLayoutManager(this, 4)
        rvApps.adapter = AppAdapter(filteredApps) { app ->
            val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
            launchIntent?.let { startActivity(it) }
        }
    }
}