package com.kcb.kiosk

import android.app.AlertDialog
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

    private lateinit var supabase: SupabaseClient
    private lateinit var layoutLock: LinearLayout
    private lateinit var layoutUnlocked: LinearLayout
    private lateinit var tvTimer: TextView
    private lateinit var rvApps: RecyclerView
    private lateinit var etPin: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supabase = SupabaseClient.getInstance()

        layoutLock = findViewById(R.id.layoutLock)
        layoutUnlocked = findViewById(R.id.layoutUnlocked)
        tvTimer = findViewById(R.id.tvTimer)
        rvApps = findViewById(R.id.rvApps)
        etPin = findViewById(R.id.etPin)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnAdmin = findViewById<TextView>(R.id.btnAdmin)

        btnStart.setOnClickListener {
            val pin = etPin.text.toString().trim()
            if (pin.isNotEmpty()) {
                validatePin(pin)
            } else {
                Toast.makeText(this, "Please enter a PIN", Toast.LENGTH_SHORT).show()
            }
        }

        // SECURITY: Admin PIN Check before opening settings
        btnAdmin.setOnClickListener {
            showAdminLoginDialog()
        }

        setupAppGrid()
    }

    // 1. DISABLE BACK BUTTON
    override fun onBackPressed() {
        // Do nothing - Locked ang back button para sa customer
    }

    // 2. RECENT APPS GUARD (3-Lines Button)
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            // Kapag nawala ang focus (dahil pinindot ang 3 lines), isasara natin ang system dialogs
            val closeDialog = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            sendBroadcast(closeDialog)
        }
    }

    private fun showAdminLoginDialog() {
        val adminEditText = EditText(this)
        adminEditText.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        adminEditText.hint = "Enter Admin PIN"

        AlertDialog.Builder(this)
            .setTitle("Admin Access")
            .setMessage("Please enter your secret PIN to access settings.")
            .setView(adminEditText)
            .setPositiveButton("Login") { _, _ ->
                val enteredPin = adminEditText.text.toString()
                if (enteredPin == "2026") { // PALITAN MO ITO NG GUSTO MONG PIN
                    val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Wrong Admin PIN!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun validatePin(pin: String) {
        lifecycleScope.launch {
            try {
                val result = supabase.validatePin(pin)
                if (result != null) {
                    unlockDevice(result.seconds_left)
                } else {
                    Toast.makeText(this@MainActivity, "Invalid PIN", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun unlockDevice(seconds: Long) {
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
            layoutLock.visibility = View.VISIBLE
            layoutUnlocked.visibility = View.GONE
        }
    }

    private fun setupAppGrid() {
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val pkgAppsList = packageManager.queryIntentActivities(mainIntent, 0)

        // FILTER: Dito natin pwedeng itago ang mga system apps sa susunod
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
