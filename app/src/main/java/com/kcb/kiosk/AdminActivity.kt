package com.kcb.kiosk

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
            val pinInput = etTestPin.text.toString().trim()
            if (pinInput.isNotEmpty()) {
                testPin(pinInput)
            } else {
                Toast.makeText(this, "Type a PIN first (e.g., 031399)", Toast.LENGTH_SHORT).show()
            }
        }

        btnSelectApps.setOnClickListener {
            // Pag-open ng App Selection screen mamaya
            Toast.makeText(this, "App Selection logic is next!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun testPin(pin: String) {
        lifecycleScope.launch {
            try {
                // I-call ang Supabase
                val result = supabase.validatePin(pin)
                
                if (result != null) {
                    // SUCCESS!
                    Toast.makeText(
                        this@AdminActivity, 
                        "SUCCESS!\nPIN: ${result.pin}\nTime: ${result.seconds_left}s", 
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // EMPTY RESULT (Walang error pero walang nahanap)
                    Toast.makeText(
                        this@AdminActivity, 
                        "DATABASE EMPTY: PIN '$pin' not found. Check if PIN exists in Supabase table.", 
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                // ACTUAL ERROR (e.g. No Internet, Wrong API Key, RLS Policy Issue)
                val errorMsg = e.localizedMessage ?: "Unknown connection error"
                Log.e("SUPABASE_DEBUG", "Error: ", e)
                Toast.makeText(
                    this@AdminActivity, 
                    "CRITICAL ERROR: $errorMsg", 
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}