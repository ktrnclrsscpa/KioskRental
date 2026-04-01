package com.kcb.kiosk

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class AdminActivity : AppCompatActivity() {
    private lateinit var supabase: SupabaseClient
    private lateinit var pinListText: TextView
    private lateinit var generatePinInput: EditText
    private lateinit var generateMinutesInput: EditText
    private lateinit var generateBtn: Button
    private lateinit var refreshBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        supabase = SupabaseClient.getInstance()
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 50, 30, 30)
        }
        
        // Title
        val title = TextView(this).apply {
            text = "🔐 ADMIN PANEL"
            textSize = 24f
            setPadding(0, 0, 0, 30)
        }
        layout.addView(title)
        
        // Generate PIN section
        val generateLabel = TextView(this).apply {
            text = "Generate New PIN"
            textSize = 18f
            setPadding(0, 20, 0, 10)
        }
        layout.addView(generateLabel)
        
        val pinRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 10)
        }
        
        generatePinInput = EditText(this).apply {
            hint = "PIN (leave blank for random)"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(10, 10, 10, 10)
        }
        pinRow.addView(generatePinInput)
        
        generateMinutesInput = EditText(this).apply {
            hint = "Minutes"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f)
            setPadding(10, 10, 10, 10)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        pinRow.addView(generateMinutesInput)
        layout.addView(pinRow)
        
        generateBtn = Button(this).apply {
            text = "GENERATE PIN"
            setOnClickListener { generatePin() }
        }
        layout.addView(generateBtn)
        
        // Refresh button
        refreshBtn = Button(this).apply {
            text = "🔄 REFRESH LIST"
            setPadding(0, 20, 0, 20)
            setOnClickListener { loadPins() }
        }
        layout.addView(refreshBtn)
        
        // PIN list
        pinListText = TextView(this).apply {
            text = "Loading PINs..."
            textSize = 14f
            setPadding(0, 20, 0, 0)
        }
        layout.addView(pinListText)
        
        setContentView(layout)
        
        loadPins()
    }
    
    private fun generatePin() {
        val minutes = generateMinutesInput.text.toString().toIntOrNull()
        if (minutes == null || minutes <= 0) {
            Toast.makeText(this, "Enter valid minutes (1-1440)", Toast.LENGTH_SHORT).show()
            return
        }
        
        val customPin = generatePinInput.text.toString().trim()
        val pin = if (customPin.length == 6) customPin else null
        
        generateBtn.isEnabled = false
        generateBtn.text = "GENERATING..."
        
        CoroutineScope(Dispatchers.IO).launch {
            val result = supabase.generatePin(pin, minutes * 60)
            withContext(Dispatchers.Main) {
                generateBtn.isEnabled = true
                generateBtn.text = "GENERATE PIN"
                if (result != null) {
                    Toast.makeText(this@AdminActivity, "PIN: $result ($minutes min)", Toast.LENGTH_LONG).show()
                    generatePinInput.text.clear()
                    generateMinutesInput.text.clear()
                    loadPins()
                } else {
                    Toast.makeText(this@AdminActivity, "Failed to generate PIN", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun loadPins() {
        pinListText.text = "Loading..."
        CoroutineScope(Dispatchers.IO).launch {
            val pins = supabase.getActivePins()
            withContext(Dispatchers.Main) {
                if (pins.isEmpty()) {
                    pinListText.text = "No active PINs"
                } else {
                    val sb = StringBuilder()
                    for (p in pins) {
                        val minutes = p.secondsLeft / 60
                        sb.append("🔑 ${p.pin} - ${minutes} min left\n")
                    }
                    pinListText.text = sb.toString()
                }
            }
        }
    }
}
