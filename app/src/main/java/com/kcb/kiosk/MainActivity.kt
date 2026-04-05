package com.kcb.kiosk

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.util.Locale

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
    private var isActive = false
    private var appList = mutableListOf<AppInfo>()
    private var remainingSeconds = 0
    private var extendCheckJob: Job? = null
    private var paidAmount = 0.0
    
    private lateinit var tts: TextToSpeech
    
    private lateinit var floatingTimerContainer: LinearLayout
    private lateinit var floatingTimer: TextView
    private var windowManager: android.view.WindowManager? = null
    private var isOverlayVisible = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = android.content.Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                startActivity(intent)
            }
        }
        
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
            }
        }
        
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
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            textSize = 20f
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(20, 15, 20, 15)
            maxLines = 1
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
        debugText.text = "Loading whitelisted apps from local storage..."
        
        val prefs = getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE)
        val whitelistPackages = prefs.getStringSet("whitelist", emptySet()) ?: emptySet()
        
        val allInstalledApps = mutableListOf<AppInfo>()
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        for (app in packages) {
            val appName = packageManager.getApplicationLabel(app).toString()
            allInstalledApps.add(AppInfo(appName, app.packageName))
        }
        
        val matchedApps = mutableListOf<AppInfo>()
        for (whitelistPkg in whitelistPackages) {
            val found = allInstalledApps.find { it.packageName == whitelistPkg }
            if (found != null) {
                matchedApps.add(found)
            }
        }
        
        matchedApps.sortBy { it.name }
        
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
            if (isActive) {
                appGrid.visibility = android.view.View.VISIBLE
            }
            Toast.makeText(this@MainActivity, "✅ Loaded ${appList.size} apps", Toast.LENGTH_SHORT).show()
        } else {
            debugText.text = "$statusMsg\nNo matching apps. Go to Admin > APPS to select apps."
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
                        paidAmount = result.amount
                        startSession()
                    } else {
                        Toast.makeText(this@MainActivity, "Invalid or expired PIN", Toast.LENGTH_LONG).show()
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
            
            if (appList.isNotEmpty()) {
                appGrid.visibility = android.view.View.VISIBLE
            }
            
            showFloatingTimer()
            startCountDownTimer()
            startExtendListener()
            
            CoroutineScope(Dispatchers.IO).launch {
                val minutes = remainingSeconds / 60
                supabase.recordSession(currentPin!!, minutes, paidAmount)
                supabase.sendTelegramNotification("🎮 *New Rental Session!*%0APIN: ${currentPin}%0ADuration: ${minutes} minutes%0AAmount: ₱${String.format("%.2f", paidAmount)}")
            }
            
        } catch (e: Exception) {
            Toast.makeText(this, "Session error: ${e.message}", Toast.LENGTH_SHORT).show()
            endSession()
        }
    }
    
    private fun startCountDownTimer() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(remainingSeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = (millisUntilFinished / 1000).toInt()
                val minutes = remainingSeconds / 60
                val secs = remainingSeconds % 60
                val timeString = String.format("%02d:%02d", minutes, secs)
                timerText.text = timeString
                if (::floatingTimer.isInitialized) {
                    floatingTimer.text = timeString
                }
                
                when (remainingSeconds) {
                    300 -> speakAlert("5 minutes remaining")
                    60 -> speakAlert("1 minute remaining")
                    30 -> speakAlert("30 seconds remaining")
                }
            }
            
            override fun onFinish() {
                speakAlert("Time expired")
                endSession()
            }
        }.start()
    }
    
    private fun startExtendListener() {
        extendCheckJob?.cancel()
        extendCheckJob = CoroutineScope(Dispatchers.IO).launch {
            var lastSeconds = remainingSeconds
            while (isActive && currentPin != null) {
                delay(2000)
                try {
                    val result = supabase.validatePin(currentPin!!)
                    if (result.isValid && result.secondsLeft > 0) {
                        val newSeconds = result.secondsLeft
                        if (newSeconds != lastSeconds) {
                            val addedSeconds = newSeconds - lastSeconds
                            val addedMinutes = addedSeconds / 60
                            withContext(Dispatchers.Main) {
                                countDownTimer?.cancel()
                                remainingSeconds = newSeconds
                                startCountDownTimer()
                                if (addedMinutes > 0) {
                                    Toast.makeText(this@MainActivity, "✓ Extended! +$addedMinutes minutes", Toast.LENGTH_LONG).show()
                                }
                            }
                            lastSeconds = newSeconds
                        }
                    } else if (result.secondsLeft <= 0) {
                        withContext(Dispatchers.Main) {
                            endSession()
                        }
                        break
                    }
                } catch (e: Exception) { }
            }
        }
    }
    
    private fun speakAlert(message: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
            } else {
                tts.speak(message, TextToSpeech.QUEUE_FLUSH, null)
            }
        } catch (e: Exception) { }
    }
    
    private fun showFloatingTimer() {
        if (isOverlayVisible) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return
        }
        
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as android.view.WindowManager
            
            floatingTimerContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(android.graphics.Color.parseColor("#CC000000"))
                setPadding(12, 8, 12, 8)
                gravity = android.view.Gravity.CENTER
                
                val dragHandle = TextView(this@MainActivity).apply {
                    text = "⋮⋮"
                    textSize = 18f
                    setTextColor(android.graphics.Color.WHITE)
                    setPadding(8, 0, 8, 0)
                }
                addView(dragHandle)
                
                floatingTimer = TextView(this@MainActivity).apply {
                    text = timerText.text
                    textSize = 18f
                    setTextColor(android.graphics.Color.WHITE)
                    setPadding(8, 0, 8, 0)
                    typeface = android.graphics.Typeface.MONOSPACE
                }
                addView(floatingTimer)
                
                setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            lastTouchX = event.rawX
                            lastTouchY = event.rawY
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = event.rawX - lastTouchX
                            val dy = event.rawY - lastTouchY
                            val params = floatingTimerContainer.layoutParams as android.view.WindowManager.LayoutParams
                            params.x += dx.toInt()
                            params.y += dy.toInt()
                            windowManager?.updateViewLayout(floatingTimerContainer, params)
                            lastTouchX = event.rawX
                            lastTouchY = event.rawY
                            true
                        }
                        else -> false
                    }
                }
            }
            
            val params = android.view.WindowManager.LayoutParams(
                android.view.WindowManager.LayoutParams.WRAP_CONTENT,
                android.view.WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    android.view.WindowManager.LayoutParams.TYPE_PHONE,
                android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                android.graphics.PixelFormat.TRANSLUCENT
            )
            
            params.gravity = android.view.Gravity.TOP or android.view.Gravity.START
            params.x = 20
            params.y = 100
            
            windowManager?.addView(floatingTimerContainer, params)
            isOverlayVisible = true
        } catch (e: Exception) { }
    }
    
    private fun hideFloatingTimer() {
        if (!isOverlayVisible) return
        try {
            windowManager?.removeView(floatingTimerContainer)
            isOverlayVisible = false
        } catch (e: Exception) { }
    }
    
    private fun endSession() {
        try {
            isActive = false
            extendCheckJob?.cancel()
            countDownTimer?.cancel()
            
            if (currentPin != null) {
                val pinToDelete = currentPin
                CoroutineScope(Dispatchers.IO).launch {
                    supabase.deletePin(pinToDelete!!)
                }
            }
            
            currentPin = null
            remainingSeconds = 0
            paidAmount = 0.0
            pinInput.isEnabled = true
            activateBtn.isEnabled = true
            statusText.text = ""
            timerText.text = "--:--"
            appGrid.visibility = android.view.View.GONE
            hideFloatingTimer()
            Toast.makeText(this, "Session expired. PIN can no longer be used.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) { }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            countDownTimer?.cancel()
            extendCheckJob?.cancel()
            hideFloatingTimer()
            tts.stop()
            tts.shutdown()
        } catch (e: Exception) { }
    }
}
