package com.kcb.kiosk

import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Bundle
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

        // Pag-bind ng UI elements base sa bagong XML IDs
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

        btnAdmin.setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
        }

        setupAppGrid()
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
                kotlinx.coroutines.delay(1000)
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

data class AppInfo(val label: String, val packageName: String, val icon: android.graphics.drawable.Drawable)
