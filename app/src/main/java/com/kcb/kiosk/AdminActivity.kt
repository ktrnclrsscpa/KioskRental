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
        val btnSelectApps = findViewById<Button>(R.id.btnSelectApps)

        btnCheckPin.setOnClickListener {
            // Gumamit ng .trim() para tanggalin ang invisible spaces
            val pin = etTestPin.text.toString().trim() 
            
            if (pin.isNotEmpty()) {
                testPin(pin)
            } else {
                Toast.makeText(this, "Please enter a PIN", Toast.LENGTH_SHORT).show()
            }
        }

        btnSelectApps.setOnClickListener {
            // TODO: Dito tayo susunod para sa App Selection logic
            Toast.makeText(this, "App Selection coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun testPin(pin: String) {
        lifecycleScope.launch {
            try {
                val result = supabase.validatePin(pin)
                
                // I-check kung may nahanap na PIN (hindi empty ang string)
                if (result.pin.isNotEmpty()) {
                    Toast.makeText(
                        this@AdminActivity, 
                        "Success! Time left: ${result.seconds_left}s", 
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@AdminActivity, 
                        "PIN '$pin' not found in Database", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                // Para malaman natin kung may network error o iba pang problema
                Toast.makeText(
                    this@AdminActivity, 
                    "Error: ${e.localizedMessage}", 
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}