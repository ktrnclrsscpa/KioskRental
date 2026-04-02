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
    private lateinit var debugText: TextView
    private var countDownTimer: CountDownTimer? = null
    private lateinit var supabase: SupabaseClient
    private var currentPin: String? = null
    private var syncJob: Job? = null
    private var isActive = false
    private var remainingSeconds = 0
    private var appList = mutableListOf<AppInfo>()
    private var gridReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        supabase = SupabaseClient.getInstance()
        
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 50, 30, 30)
        }
        
        val title = TextView(this).apply {
            text = "KCB RENTAL"
            textSize = 28f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }
        mainLayout.addView(title)
        
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
        
        debugText = TextView(this).apply {
            text = ""
            textSize = 11f
            setPadding(0, 0, 0, 10)
        }
        mainLayout.addView(debugText)
        
        appGrid = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            visibility = android.view.View.GONE
        }
        mainLayout.addView(appGrid)
        
        val loadAppsBtn = Button(this).apply {
            text = "📱 LOAD APPS"
            textSize = 12f
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setTextColor(android.graphics.Color.GRAY)
            setOnClickListener { loadWhitelistedApps() }
        }
        mainLayout.addView(loadAppsBtn)
        
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
        
        loadWhitelistedApps()
    }
    
    private fun loadWhitelistedApps() {
        debugText.text = "Loading whitelisted apps..."
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val whitelistPackages = supabase.getWhitelistApps()
                
                val allInstalledApps = mutableListOf<AppInfo>()
                val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                
                for (app in packages) {
                    if (packageManager.getLaunchIntentForPackage(app.packageName) != null) {
                        val appName = packageManager.getApplicationLabel(app).toString()
                        allInstalledApps.add(AppInfo(appName, app.packageName))
                    }
                }
                
                val matchedApps = mutableListOf<AppInfo>()
                for (whitelistPkg in whitelistPackages) {
                    val found = allInstalledApps.find { it.packageName == whitelistPkg }
                    if (found != null) {
                        matchedApps.add(found)
                    }
                }
                
                matchedApps.sortBy { it.name }
                
                withContext(Dispatchers.Main) {
                    appList.clear()
                    appList.addAll(matchedApps)
                    
                    val statusMsg = "Whitelist: ${whitelistPackages.size} apps | Installed: ${allInstalledApps.size} | Matched: ${matchedApps.size}"
                    debugText.text = statusMsg
                    
                    if (appList.isNotEmpty()) {
                        appGrid.adapter = SimpleAppAdapter(appList) { packageName ->
                            try {
                                val intent = packageManager.getLaunchIntentForPackage(packageName)
                                if (intent != null) {
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(this@MainActivity, "Cannot open app", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        gridReady = true
                        Toast.makeText(this@MainActivity, "✅ Loaded ${appList.size} apps", Toast.LENGTH_SHORT).show()
                    } else {
                        debugText.text = "$statusMsg\nNo matching apps. Go to Admin > APPS to select apps."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    debugText.text = "Error: ${e.message}"
                }
            }
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
            try {
                val result = supabase.validatePin(pin)
                withContext(Dispatchers.Main) {
                    pinInput.isEnabled = true
                    activateBtn.isEnabled = true
                    statusText.text = ""
                    
                    if (result.isValid && result.secondsLeft > 0) {
                        currentPin = pin
                        remainingSeconds = result.secondsLeft
                        startSession()
                    } else {
                        val errorMsg = result.error ?: "Invalid PIN or expired"
                        Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pinInput.isEnabled = true
                    activateBtn.isEnabled = true
                    statusText.text = ""
                    Toast.makeText(this@MainActivity, "Connection error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun startSession() {
        try {
            isActive = true
            pinInput.isEnabled = false
            activateBtn.isEnabled = false
            statusText.text = "ACTIVE"
            
            // Show app grid if there are apps
            if (gridReady && appList.isNotEmpty()) {
                appGrid.visibility = android.view.View.VISIBLE
            }
            
            // Start the countdown timer
            countDownTimer?.cancel()
            countDownTimer = object : CountDownTimer(remainingSeconds * 1000L, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    remainingSeconds = (millisUntilFinished / 1000).toInt()
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
            Toast.makeText(this, "Session error: ${e.message}", Toast.LENGTH_SHORT).show()
            endSession()
        }
    }
    
    private fun startSync() {
        syncJob?.cancel()
        syncJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && currentPin != null) {
                delay(5000)
                try {
                    val result = supabase.validatePin(currentPin!!)
                    withContext(Dispatchers.Main) {
                        if (!result.isValid || result.secondsLeft <= 0) {
                            Toast.makeText(this@MainActivity, "Session expired (admin)", Toast.LENGTH_SHORT).show()
                            endSession()
                        } else if (result.secondsLeft != remainingSeconds) {
                            // Update remaining seconds from server
                            remainingSeconds = result.secondsLeft
                            countDownTimer?.cancel()
                            startSession()
                        }
                    }
                } catch (e: Exception) {
                    // Ignore network errors during sync
                }
            }
        }
    }
    
    private fun endSession() {
        try {
            isActive = false
            syncJob?.cancel()
            countDownTimer?.cancel()
            currentPin = null
            remainingSeconds = 0
            pinInput.isEnabled = true
            activateBtn.isEnabled = true
            statusText.text = ""
            timerText.text = "--:--"
            appGrid.visibility = android.view.View.GONE
            Toast.makeText(this, "Session expired", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            countDownTimer?.cancel()
            syncJob?.cancel()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
