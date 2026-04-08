package com.kcb.kiosk

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.MotionEvent
import android.widget.*
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
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = android.content.Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivity(intent)
        }
        
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts.language = Locale.US
        }
        
        supabase = SupabaseClient.getInstance()
        
        // Layout creation (Same as your original)
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
        
        val pinRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        pinInput = EditText(this).apply {
            hint = "6-digit PIN"
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        activateBtn = Button(this).apply {
            text = "ACTIVATE"
            setOnClickListener { validatePin() }
        }
        pinRow.addView(pinInput); pinRow.addView(activateBtn)
        mainLayout.addView(pinRow)
        
        timerText = TextView(this).apply {
            text = "--:--"
            textSize = 48f
            gravity = android.view.Gravity.CENTER
        }
        mainLayout.addView(timerText)
        
        statusText = TextView(this).apply { gravity = android.view.Gravity.CENTER }
        mainLayout.addView(statusText)
        
        appGrid = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            visibility = android.view.View.GONE
        }
        mainLayout.addView(appGrid)
        
        setContentView(mainLayout)
        loadWhitelistedApps()
    }

    private fun validatePin() {
        val pin = pinInput.text.toString().trim()
        if (pin.length != 6) return
        
        statusText.text = "CHECKING..."
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = supabase.validatePin(pin)
                withContext(Dispatchers.Main) {
                    if (result.isValid && result.secondsLeft > 0) {
                        currentPin = pin
                        remainingSeconds = result.secondsLeft
                        paidAmount = result.amount
                        startSession()
                    } else {
                        Toast.makeText(this@MainActivity, "Invalid PIN", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) { }
        }
    }

    private fun startSession() {
        isActive = true
        statusText.text = "ACTIVE"
        appGrid.visibility = android.view.View.VISIBLE
        startCountDownTimer()
        startExtendListener()
        showFloatingTimer()
    }

    private fun startCountDownTimer() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(remainingSeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = (millisUntilFinished / 1000).toInt()
                val timeString = String.format("%02d:%02d", remainingSeconds / 60, remainingSeconds % 60)
                timerText.text = timeString
                if (::floatingTimer.isInitialized) floatingTimer.text = timeString
            }
            override fun onFinish() { endSession() }
        }.start()
    }

    private fun startExtendListener() {
        extendCheckJob?.cancel()
        extendCheckJob = CoroutineScope(Dispatchers.IO).launch {
            var lastKnownDbSeconds = remainingSeconds // Start sync point

            while (isActive && currentPin != null) {
                delay(3000)
                try {
                    val result = supabase.validatePin(currentPin!!)
                    if (result.isValid) {
                        val currentDbSeconds = result.secondsLeft
                        
                        // KUNG LUMAKI ANG ORAS SA DB (Dahil sa Admin Extend)
                        if (currentDbSeconds > lastKnownDbSeconds) {
                            val added = currentDbSeconds - lastKnownDbSeconds
                            withContext(Dispatchers.Main) {
                                countDownTimer?.cancel()
                                remainingSeconds += added // DAGDAGAN LANG, HUWAG PALITAN LAHAT
                                startCountDownTimer()
                                Toast.makeText(this@MainActivity, "Time Extended!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        lastKnownDbSeconds = currentDbSeconds
                    }
                } catch (e: Exception) { }
            }
        }
    }

    private fun endSession() {
        isActive = false
        countDownTimer?.cancel()
        extendCheckJob?.cancel()
        currentPin = null
        remainingSeconds = 0
        timerText.text = "--:--"
        appGrid.visibility = android.view.View.GONE
        hideFloatingTimer()
    }

    // Helper functions for Apps and Floating Timer (Simplified for code length)
    private fun loadWhitelistedApps() { /* Implementation same as your original */ }
    private fun showFloatingTimer() { /* Implementation same as your original */ }
    private fun hideFloatingTimer() { /* Implementation same as your original */ }
}
