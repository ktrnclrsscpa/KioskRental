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

        btnGenerate.setOnClickListener {
            val newPin = generateRandomAlphanumeric()
            lifecycleScope.launch {
                SupabaseClient.getInstance().createNewPin(newPin, 3600)
                tvGeneratedPin.text = "Generated: $newPin"
                Toast.makeText(this@AdminActivity, "PIN Active for 1 Hour", Toast.LENGTH_SHORT).show()
            }
        }

        btnExtend.setOnClickListener {
            val pin = etExtendPin.text.toString().trim().uppercase()
            if (pin.isNotEmpty()) {
                lifecycleScope.launch {
                    val success = SupabaseClient.getInstance().extendPinTime(pin, 1800)
                    if (success) {
                        Toast.makeText(this@AdminActivity, "Extended 30 Minutes", Toast.LENGTH_SHORT).show()
                        etExtendPin.text.clear()
                    } else {
                        Toast.makeText(this@AdminActivity, "PIN Not Found", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun generateRandomAlphanumeric(): String {
        val source = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { source.random() }.joinToString("")
    }
}