package com.kcb.kiosk

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AdminActivity : AppCompatActivity() {
    
    private val prefs by lazy { getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAdminPassword()
    }
    
    private fun checkAdminPassword() {
        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = "Enter admin password"
        
        AlertDialog.Builder(this)
            .setTitle("🔐 Admin Access")
            .setMessage("Enter password to continue")
            .setView(input)
            .setPositiveButton("Login") { _, _ ->
                val password = input.text.toString()
                val savedPassword = prefs.getString("admin_password", "admin123")
                if (password == savedPassword) {
                    showAdminPanel()
                } else {
                    Toast.makeText(this, "Wrong password!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showAdminPanel() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 100, 50, 50)
        }
        
        val title = TextView(this).apply {
            text = "✅ ADMIN PANEL IS WORKING!"
            textSize = 24f
            gravity = android.view.Gravity.CENTER
        }
        layout.addView(title)
        
        val message = TextView(this).apply {
            text = "Successfully logged in as admin."
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 30, 0, 30)
        }
        layout.addView(message)
        
        val closeBtn = Button(this).apply {
            text = "CLOSE ADMIN PANEL"
            setOnClickListener {
                finish()
            }
        }
        layout.addView(closeBtn)
        
        setContentView(layout)
        
        Toast.makeText(this, "Welcome Admin!", Toast.LENGTH_LONG).show()
    }
}
