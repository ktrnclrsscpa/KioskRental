package com.kcb.kiosk

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        supabase = SupabaseClient.getInstance()
        
        // Create layout programmatically
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(50, 50, 50, 50)
        }
        
        // Title
        val title = TextView(this).apply {
            text = "KCB RENTAL"
            textSize = 28f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 50)
        }
        layout.addView(title)
        
        // PIN input
        pinInput = EditText(this).apply {
            hint = "Enter 6-digit PIN"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            textSize = 24f
            gravity = android.view.Gravity.CENTER
            setPadding(20, 20, 20, 20)
        }
        layout.addView(pinInput)
        
        // Activate button
        activateBtn = Button(this).apply {
            text = "ACTIVATE"
            textSize = 18f
            setPadding(20, 20, 20, 20)
            setOnClickListener { validatePin() }
        }
        layout.addView(activateBtn)
        
        // Timer display
        timerText = TextView(this).apply {
            text = "--:--"
            textSize = 48f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 50, 0, 0)
        }
        layout.addView(timerText)
        
        // Status display
        statusText = TextView(this).apply {
            text = ""
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 20, 0, 0)
        }
        layout.addView(statusText)
        
        setContentView(layout)
    }
    
    private fun validatePin() {
        val pin = pinInput.text.toString().trim()
        if (pin.length != 6) {
            Toast.makeText(this, "Enter 6-digit PIN", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Disable input while checking
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
                    startSession(result.secondsLeft)
                } else {
                    Toast.makeText(this@MainActivity, "Invalid PIN or expired", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun startSession(seconds: Int) {
        pinInput.isEnabled = false
        activateBtn.isEnabled = false
        statusText.text = "ACTIVE"
        
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
    }
    
    private fun endSession() {
        countDownTimer?.cancel()
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
