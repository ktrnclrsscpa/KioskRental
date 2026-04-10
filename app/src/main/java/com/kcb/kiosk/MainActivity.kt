package com.kcb.kiosk

import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
            if (pin == "123456") { // Temporary test PIN
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

    // 1. DISABLE RECENT APPS (3-LINES) BY RECALLING ACTIVITY
    override fun onPause() {
        super.onPause()
        if (isUnlocked) {
            // Kung sinubukan nilang mag-recent apps habang "Unlocked" (naglalaro),
            // hindi natin sila pipigilan para makapunta sa laro.
        } else {
            // Pero kung "Locked" (PIN Screen), hihilahin natin sila pabalik agad
            val activityManager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.moveTaskToFront(taskId, 0)
        }
    }

    // 2. BLOCK SYSTEM DIALOGS (PULL-DOWN MENU & RECENT APPS)
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            // Ito ang "killer" line: Pinipilit isara ang mga system windows
            val closeDialog = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            sendBroadcast(closeDialog)
            
            // I-delay nang konti tapos hilahin pabalik ang app focus
            lifecycleScope.launch {
                delay(100)
                val intent = Intent(this@MainActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
            }
        }
    }

    // 3. DISABLE BACK BUTTON
    override fun onBackPressed() {
        // No action para locked ang back button
    }

    private fun showAdminLoginDialog() {
        val adminEditText = EditText(this)
        adminEditText.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        adminEditText.hint = "Enter Admin PIN"

        AlertDialog.Builder(this)
            .setTitle("Admin Access")
            .setView(adminEditText)
            .setPositiveButton("Login") { _, _ ->
                if (adminEditText.text.toString() == "2026") {
                    val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Wrong Admin PIN!", Toast.LENGTH_SHORT).show()
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
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val pkgAppsList = packageManager.queryIntentActivities(mainIntent, 0)

        val apps = pkgAppsList.map { 
            AppInfo(it.loadLabel(packageManager).toString(), it.activityInfo.packageName, it.loadIcon(packageManager)) 
        }

        rvApps.layoutManager = GridLayoutManager(this, 4)
        rvApps.adapter = AppAdapter(apps) { app ->
            val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
            launchIntent?.let { startActivity(it) }
        }
    }
}
