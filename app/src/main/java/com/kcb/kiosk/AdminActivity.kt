package com.kcb.kiosk

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        val btnGenerate = findViewById<Button>(R.id.btnGenerate)
        val tvGeneratedPin = findViewById<TextView>(R.id.tvGeneratedPin)
        val etExtendPin = findViewById<EditText>(R.id.etExtendPin)
        val btnExtend = findViewById<Button>(R.id.btnExtend)

        // Generate PIN Logic
        btnGenerate.setOnClickListener {
            val newPin = (1..6).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")
            lifecycleScope.launch {
                try {
                    SupabaseClient.getInstance().createNewPin(newPin, 3600) // Default 1 hour
                    tvGeneratedPin.text = "Generated: $newPin"
                    Toast.makeText(this@AdminActivity, "PIN Saved to Database", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@AdminActivity, "Error saving PIN", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Extend Logic
        btnExtend.setOnClickListener {
            val pin = etExtendPin.text.toString().trim().uppercase()
            if (pin.isNotEmpty()) {
                lifecycleScope.launch {
                    val success = SupabaseClient.getInstance().extendPinTime(pin, 1800) // Add 30 mins
                    if (success) {
                        Toast.makeText(this@AdminActivity, "PIN $pin Extended!", Toast.LENGTH_SHORT).show()
                        etExtendPin.text.clear()
                    } else {
                        Toast.makeText(this@AdminActivity, "PIN not found!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}