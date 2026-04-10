package com.kcb.kiosk

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var tvTimer: TextView
    private lateinit var etPin: EditText
    private lateinit var layoutLocked: LinearLayout
    private lateinit var layoutUnlocked: LinearLayout
    private lateinit var rvApps: RecyclerView
    private var countDownTimer: CountDownTimer? = null
    private val supabase = SupabaseClient.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTimer = findViewById(R.id.tvTimer)
        etPin = findViewById(R.id.etPin)
        layoutLocked = findViewById(R.id.layoutLocked)
        layoutUnlocked = findViewById(R.id.layoutUnlocked)
        rvApps = findViewById(R.id.rvAllowedApps)

        setupAppGrid()
        checkExistingTime()

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            val pin = etPin.text.toString().trim()
            if (pin.isNotEmpty()) validateAndStart(pin)
        }

        findViewById<Button>(R.id.btnAdmin).setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
        }
    }

    private fun setupAppGrid() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(intent, 0).map {
            AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm))
        }.filter { it.packageName != packageName } // Wag ipakita ang sariling app sa listahan

        rvApps.layoutManager = GridLayoutManager(this, 4)
        rvApps.adapter = AppAdapter(apps) { app ->
            val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
            launchIntent?.let { startActivity(it) }
        }
    }

    private fun validateAndStart(pin: String) {
        lifecycleScope.launch {
            try {
                val result = supabase.validatePin(pin)
                if (result != null && result.seconds_left > 0) {
                    startTimer(result.seconds_left)
                    etPin.text.clear()
                } else {
                    Toast.makeText(this@MainActivity, "Invalid PIN or No Time", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Connection Error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startTimer(seconds: Long) {
        countDownTimer?.cancel()
        layoutLocked.visibility = View.GONE
        layoutUnlocked.visibility = View.VISIBLE

        countDownTimer = object : CountDownTimer(seconds * 1000, 1000) {
            override fun onTick(m: Long) {
                val s = m / 1000
                tvTimer.text = String.format("%02d:%02d", s / 60, s % 60)
                saveTimeLocally(s)
            }
            override fun onFinish() { lockPhone() }
        }.start()
    }

    private fun lockPhone() {
        layoutLocked.visibility = View.VISIBLE
        layoutUnlocked.visibility = View.GONE
        saveTimeLocally(0)
    }

    private fun saveTimeLocally(s: Long) = getSharedPreferences("KioskPrefs", Context.MODE_PRIVATE).edit().putLong("rem", s).apply()

    private fun checkExistingTime() {
        val s = getSharedPreferences("KioskPrefs", Context.MODE_PRIVATE).getLong("rem", 0)
        if (s > 0) startTimer(s) else lockPhone()
    }

    override fun onBackPressed() { /* Disabled */ }
}