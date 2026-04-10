package com.kcb.kiosk

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AdminActivity : AppCompatActivity() {

    private lateinit var supabase: SupabaseClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        supabase = SupabaseClient.getInstance()

        val etTestPin = findViewById<EditText>(R.id.etTestPin)
        val btnCheckPin = findViewById<Button>(R.id.btnCheckPin)
        val btnExitAdmin = findViewById<Button>(R.id.btnExitAdmin)

        // Button para i-test kung gumagana ang PIN sa database
        btnCheckPin.setOnClickListener {
            val pin = etTestPin.text.toString().trim()
            if (pin.isNotEmpty()) {
                testPinConnection(pin)
            } else {
                Toast.makeText(this, "Enter a PIN to test", Toast.LENGTH_SHORT).show()
            }
        }

        // Button para bumalik sa Main Launcher
        btnExitAdmin.setOnClickListener {
            finish()
        }
    }

    private fun testPinConnection(pin: String) {
        lifecycleScope.launch {
            try {
                val result = supabase.validatePin(pin)
                if (result != null) {
                    Toast.makeText(
                        this@AdminActivity,
                        "DATABASE OK!\nPIN: ${result.pin}\nSeconds: ${result.seconds_left}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(this@AdminActivity, "PIN not found in database.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}