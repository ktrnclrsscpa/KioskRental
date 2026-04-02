package com.kcb.kiosk

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var pinInput: EditText
    private lateinit var activateBtn: Button
    private lateinit var timerText: TextView
    private lateinit var statusText: TextView
    private lateinit var appGrid: RecyclerView
    private var countDownTimer: CountDownTimer? = null
    private lateinit var supabase: SupabaseClient
    private var currentPin: String? = null
    private var syncJob: Job? = null
    private var isActive = false
    private var appList = mutableListOf<AppInfo>()
    private var allInstalledApps = mutableListOf<AppInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        supabase = SupabaseClient.getInstance()
        
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 50, 30, 30)
        }
        
        // Title
        val title = TextView(this).apply {
            text = "KCB RENTAL"
            textSize = 28f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }
        mainLayout.addView(title)
        
        // PIN input row
        val pinRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 20)
        }
        
        pinInput = EditText(this).apply {
            hint = "Enter 6-digit PIN"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            textSize = 20f
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(20, 15, 20, 15)
        }
        pinRow.addView(pinInput)
        
        activateBtn = Button(this).apply {
            text = "ACTIVATE"
            textSize = 16f
            setPadding(20, 15, 20, 15)
            setOnClickListener { validatePin() }
        }
        pinRow.addView(activateBtn)
        mainLayout.addView(pinRow)
        
        // Timer and status
        timerText = TextView(this).apply {
            text = "--:--"
            textSize = 48f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 20, 0, 10)
        }
        mainLayout.addView(timerText)
        
        statusText = TextView(this).apply {
            text = ""
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        mainLayout.addView(statusText)
        
        // App grid (hidden initially)
        appGrid = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@MainActivity, 3)
            visibility = android.view.View.GONE
        }
        mainLayout.addView(appGrid)
        
        // Load Apps button
        val loadAppsBtn = Button(this).apply {
            text = "📱 LOAD ALL APPS"
            textSize = 12f
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setTextColor(android.graphics.Color.GRAY)
            setOnClickListener { loadAllInstalledApps() }
        }
        mainLayout.addView(loadAppsBtn)
        
        // Admin button
        val adminBtn = Button(this).apply {
            text = "🔐 ADMIN"
            textSize = 14f
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setTextColor(android.graphics.Color.GRAY)
            setPadding(0, 10, 0, 0)
            setOnClickListener {
                startActivity(android.content.Intent(this@MainActivity, AdminActivity::class.java))
            }
        }
        mainLayout.addView(adminBtn)
        
        setContentView(mainLayout)
        
        // Auto-load apps when app starts
        loadAllInstalledApps()
    }
    
    private fun loadAllInstalledApps() {
        allInstalledApps.clear()
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        for (app in packages) {
            // Check if app can be launched (has an icon)
            if (packageManager.getLaunchIntentForPackage(app.packageName) != null) {
                val appName = packageManager.getApplicationLabel(app).toString()
                allInstalledApps.add(AppInfo(appName, app.packageName))
            }
        }
        
        allInstalledApps.sortBy { it.name }
        
        // Show the first 6 apps as demo (you can change this)
        appList.clear()
        val maxApps = minOf(6, allInstalledApps.size)
        for (i in 0 until maxApps) {
            appList.add(allInstalledApps[i])
        }
        
        if (appList.isNotEmpty()) {
            appGrid.adapter = AppAdapter(appList, packageManager)
            Toast.makeText(this, "✅ Loaded ${appList.size} apps", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "❌ No apps found", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun validatePin() {
        val pin = pinInput.text.toString().trim()
        if (pin.length != 6) {
            Toast.makeText(this, "Enter 6-digit PIN", Toast.LENGTH_SHORT).show()
            return
        }
        
        pinInput.isEnabled = false
        activateBtn.isEnabled = false
        statusText.text = "CHECKING..."
        
        CoroutineScope(Dispatchers.IO).launch {
            val result = supabase.validatePin(pin)
            withContext(Dispatchers.Main) {
                pinInput.isEnabled = true
                activateBtn.isEnabled = true
                statusText.text = ""
                
                if (result.isValid && result.secondsLeft > 0) {
                    currentPin = pin
                    startSession(result.secondsLeft)
                } else {
                    val errorMsg = result.error ?: "Invalid PIN or expired"
                    Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun startSession(seconds: Int) {
        try {
            isActive = true
            pinInput.isEnabled = false
            activateBtn.isEnabled = false
            statusText.text = "ACTIVE"
            
            // Show app grid if there are apps
            if (appList.isNotEmpty()) {
                appGrid.visibility = android.view.View.VISIBLE
            }
            
            countDownTimer?.cancel()
            countDownTimer = object : CountDownTimer(seconds * 1000L, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val remainingSeconds = (millisUntilFinished / 1000).toInt()
                    val minutes = remainingSeconds / 60
                    val secs = remainingSeconds % 60
                    timerText.text = String.format("%02d:%02d", minutes, secs)
                }
                
                override fun onFinish() {
                    endSession()
                }
            }.start()
            
            startSync()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            endSession()
        }
    }
    
    private fun startSync() {
        syncJob?.cancel()
        syncJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && currentPin != null) {
                delay(5000)
                val result = supabase.validatePin(currentPin!!)
                withContext(Dispatchers.Main) {
                    if (!result.isValid || result.secondsLeft <= 0) {
                        Toast.makeText(this@MainActivity, "Session expired (admin)", Toast.LENGTH_SHORT).show()
                        endSession()
                    } else if (result.secondsLeft != getCurrentRemainingSeconds()) {
                        updateTimerFromDatabase(result.secondsLeft)
                    }
                }
            }
        }
    }
    
    private fun getCurrentRemainingSeconds(): Int {
        val timeStr = timerText.text.toString()
        if (timeStr == "--:--") return 0
        val parts = timeStr.split(":")
        if (parts.size == 2) {
            return parts[0].toInt() * 60 + parts[1].toInt()
        }
        return 0
    }
    
    private fun updateTimerFromDatabase(seconds: Int) {
        countDownTimer?.cancel()
        startSession(seconds)
    }
    
    private fun endSession() {
        isActive = false
        syncJob?.cancel()
        countDownTimer?.cancel()
        currentPin = null
        pinInput.isEnabled = true
        activateBtn.isEnabled = true
        statusText.text = ""
        timerText.text = "--:--"
        appGrid.visibility = android.view.View.GONE
        Toast.makeText(this, "Session expired", Toast.LENGTH_LONG).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        syncJob?.cancel()
    }
}
