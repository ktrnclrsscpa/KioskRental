package com.kcb.kiosk

import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class AdminActivity : AppCompatActivity() {
    private lateinit var supabase: SupabaseClient
    private lateinit var logText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLoginDialog()
    }

    private fun showLoginDialog() {
        val input = EditText(this).apply { 
            hint = "Admin Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD 
        }
        
        AlertDialog.Builder(this)
            .setTitle("Admin Login")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Login") { _, _ ->
                if (input.text.toString() == "admin123") {
                    setupAdminUI()
                } else {
                    Toast.makeText(this, "Wrong Password!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("Cancel") { _, _ -> finish() }
            .show()
    }

    private fun setupAdminUI() {
        supabase = SupabaseClient.getInstance()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
        }

        val title = TextView(this).apply { 
            text = "ADMIN DASHBOARD"; textSize = 24f; setPadding(0,0,0,30) 
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        
        logText = TextView(this).apply { 
            text = "Loading Active PINs..."; textSize = 16f
        }
        
        root.addView(title); root.addView(logText)
        setContentView(root)
        refreshData()
    }

    private fun refreshData() {
        CoroutineScope(Dispatchers.IO).launch {
            val list = supabase.getActivePins()
            withContext(Dispatchers.Main) {
                if (list.isEmpty()) {
                    logText.text = "No active pins found."
                } else {
                    val sb = StringBuilder("🔑 ACTIVE SESSIONS:\n\n")
                    list.forEach { sb.append("• PIN: ${it.pin} (${it.secondsLeft / 60}m left)\n") }
                    logText.text = sb.toString()
                }
            }
        }
    }
}
