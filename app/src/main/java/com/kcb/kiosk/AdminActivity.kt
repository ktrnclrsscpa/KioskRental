package com.kcb.kiosk

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AdminActivity : AppCompatActivity() {
    private lateinit var supabase: SupabaseClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        supabase = SupabaseClient.getInstance()

        val btnCheckPin = findViewById<Button>(R.id.btnCheckPin)
        val etTestPin = findViewById<EditText>(R.id.etTestPin)

        btnCheckPin.setOnClickListener {
            val pin = etTestPin.text.toString()
            if (pin.isNotEmpty()) {
                testPin(pin)
            } else {
                Toast.makeText(this, "Please enter a PIN", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun testPin(pin: String) {
        lifecycleScope.launch {
            val result = supabase.validatePin(pin)
            if (result.pin.isNotEmpty()) {
                Toast.makeText(this@AdminActivity, "Success! Time left: ${result.seconds_left}s", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@AdminActivity, "PIN not found in Supabase", Toast.LENGTH_SHORT).show()
            }
        }
    }
}