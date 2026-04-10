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
        val btnExitAdmin = findViewById<Button>(R.id.btnExitAdmin) // Dito nag-error kanina

        btnCheckPin.setOnClickListener {
            val pin = etTestPin.text.toString().trim()
            if (pin.isNotEmpty()) {
                testPinConnection(pin)
            }
        }

        btnExitAdmin.setOnClickListener {
            finish() // Babalik sa MainActivity (Launcher)
        }
    }

    private fun testPinConnection(pin: String) {
        lifecycleScope.launch {
            try {
                val result = supabase.validatePin(pin)
                if (result != null) {
                    Toast.makeText(this@AdminActivity, "DB Online: ${result.seconds_left}s", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AdminActivity, "PIN not found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}