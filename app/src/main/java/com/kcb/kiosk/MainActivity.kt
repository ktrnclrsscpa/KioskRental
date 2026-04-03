package com.kcb.kiosk

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var pinInput: EditText
    private lateinit var activateBtn: Button
    private lateinit var timerText: TextView
    private lateinit var statusText: TextView
    private var countDownTimer: CountDownTimer? = null
    private lateinit var supabase: SupabaseClient
    private var currentPin: String? = null
    private var isActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        supabase = SupabaseClient.getInstance()
        
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            gravity = android.view.Gravity.CENTER
        }
        
        val title = TextView(this).apply {
            text = "KCB RENTAL"
            textSize = 28f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 50)
        }
        mainLayout.addView(title)
        
        pinInput = EditText(this).apply {
            hint = "Enter 6-digit PIN"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            textSize = 24f
            gravity = android.view.Gravity.CENTER
            setPadding(20, 20, 20, 20)
        }
        mainLayout.addView(pinInput)
        
        activateBtn = Button(this).apply {
            text = "ACTIVATE"
            textSize = 18f
            setPadding(20, 20, 20, 20)
            setOnClickListener { validatePin() }
        }
        mainLayout.addView(activateBtn)
        
        timerText = TextView(this).apply {
            text = "--:--"
            textSize = 48f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 50, 0, 0)
        }
        mainLayout.addView(timerText)
        
        statusText = TextView(this).apply {
            text = ""
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 20, 0, 0)
        }
        mainLayout.addView(statusText)
        
        val adminBtn = Button(this).apply {
            text = "🔐 ADMIN"
            textSize = 14f
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setTextColor(android.graphics.Color.GRAY)
            setPadding(0, 30, 0, 0)
            setOnClickListener {
                startActivity(android.content.Intent(this@MainActivity, AdminActivity::class.java))
            }
        }
        mainLayout.addView(adminBtn)
        
        setContentView(mainLayout)
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
                        startSession(result.secondsLeft)
                    } else {
                        Toast.makeText(this@MainActivity, "Invalid or expired PIN", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pinInput.isEnabled = true
                    activateBtn.isEnabled = true
                    statusText.text = ""
                    Toast.makeText(this@MainActivity, "Connection error", Toast.LENGTH_SHORT).show()
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
            
            // Display initial time
            val startMinutes = seconds / 60
            val startSecs = seconds % 60
            timerText.text = String.format("%02d:%02d", startMinutes, startSecs)
            
            // Start countdown timer
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
            
        } catch (e: Exception) {
            Toast.makeText(this, "Session error: ${e.message}", Toast.LENGTH_SHORT).show()
            endSession()
        }
    }
    
    private fun endSession() {
        isActive = false
        countDownTimer?.cancel()
        currentPin = null
        pinInput.isEnabled = true
        activateBtn.isEnabled = true
        statusText.text = ""
        timerText.text = "--:--"
        Toast.makeText(this, "Session expired", Toast.LENGTH_LONG).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
