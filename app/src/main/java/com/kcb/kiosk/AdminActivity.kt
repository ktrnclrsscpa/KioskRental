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
            val pinInput = etTestPin.text.toString().trim()
            if (pinInput.isNotEmpty()) {
                testPin(pinInput)
            } else {
                Toast.makeText(this, "Type a PIN (031399)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun testPin(pin: String) {
        lifecycleScope.launch {
            try {
                val result = supabase.validatePin(pin)
                if (result != null) {
                    Toast.makeText(
                        this@AdminActivity, 
                        "SUCCESS!\nFound PIN: ${result.pin}\nTime: ${result.seconds_left}s", 
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@AdminActivity, 
                        "NOT FOUND: PIN '$pin' doesn't exist in DB.", 
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                // Makikita rito kung bakit nabigo ang connection
                Toast.makeText(
                    this@AdminActivity, 
                    "CONNECTION ERROR: ${e.localizedMessage}", 
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}