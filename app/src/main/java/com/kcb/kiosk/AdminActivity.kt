package com.kcb.kiosk

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class AdminActivity : AppCompatActivity() {
    
    private lateinit var supabase: SupabaseClient
    private val prefs by lazy { getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE) }
    
    private lateinit var contentContainer: LinearLayout
    private lateinit var tabBar: LinearLayout
    
    // UI Elements for Extension
    private lateinit var extendPinInput: EditText
    private lateinit var extendMinutesInput: EditText
    private lateinit var extendAmountInput: EditText
    private lateinit var extendBtn: Button
    private lateinit var pinsText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Login prompt bago ipakita ang Admin Panel
        checkAdminPassword()
    }

    private fun checkAdminPassword() {
        val input = EditText(this).apply { 
            hint = "Admin Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD 
        }
        
        AlertDialog.Builder(this)
            .setTitle("🔒 Admin Access")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Login") { _, _ ->
                if (input.text.toString() == prefs.getString("admin_password", "admin123")) {
                    initAdminPanel()
                } else {
                    Toast.makeText(this, "Wrong Password!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("Exit") { _, _ -> finish() }
            .show()
    }

    private fun initAdminPanel() {
        supabase = SupabaseClient.getInstance()
        
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F5F7FA"))
        }

        // --- Custom Tab Bar ---
        tabBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            elevation = 8f
        }
        addTab("PINS", "pins")
        addTab("SALES", "sales")
        
        root.addView(tabBar)

        contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }
        root.addView(contentContainer)

        setContentView(root)
        selectTab("pins") // Default tab
    }

    private fun addTab(label: String, tag: String) {
        val tab = TextView(this).apply {
            text = label
            setPadding(0, 40, 0, 40)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            this.tag = tag
            setOnClickListener { selectTab(tag) }
        }
        tabBar.addView(tab)
    }

    private fun selectTab(tag: String) {
        contentContainer.removeAllViews()
        if (tag == "pins") {
            showPinsUI()
        } else {
            val tv = TextView(this).apply { text = "Sales logic here..." }
            contentContainer.addView(tv)
        }
    }

    private fun showPinsUI() {
        val label = TextView(this).apply { 
            text = "⏰ ADD TIME TO ACTIVE PIN"
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 20)
        }
        
        extendPinInput = EditText(this).apply { hint = "Enter PIN" }
        extendMinutesInput = EditText(this).apply { hint = "Minutes to add"; inputType = 2 }
        extendAmountInput = EditText(this).apply { hint = "Amount (₱)"; inputType = 8194 }
        
        extendBtn = Button(this).apply {
            text = "EXTEND TIME"
            setBackgroundColor(Color.parseColor("#2ECC71"))
            setTextColor(Color.WHITE)
            setOnClickListener { processExtension() }
        }

        pinsText = TextView(this).apply { 
            text = "Loading active pins..."
            setPadding(0, 40, 0, 0)
        }

        contentContainer.addView(label)
        contentContainer.addView(extendPinInput)
        contentContainer.addView(extendMinutesInput)
        contentContainer.addView(extendAmountInput)
        contentContainer.addView(extendBtn)
        contentContainer.addView(pinsText)
        
        loadActivePins()
    }

    private fun processExtension() {
        val pin = extendPinInput.text.toString().trim()
        val minutes = extendMinutesInput.text.toString().toIntOrNull() ?: 0
        val amount = extendAmountInput.text.toString().toDoubleOrNull() ?: 0.0

        if (pin.isEmpty() || minutes <= 0) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        extendBtn.isEnabled = false
        extendBtn.text = "Updating..."

        CoroutineScope(Dispatchers.IO).launch {
            val result = supabase.validatePin(pin)
            if (result.isValid) {
                val newSeconds = result.secondsLeft + (minutes * 60)
                val success = supabase.updatePinSeconds(pin, newSeconds)
                
                if (success) {
                    supabase.recordExtension(pin, minutes, amount)
                    supabase.sendTelegramNotification("✅ *PIN Extended*\nPIN: $pin\nAdded: $minutes mins")
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AdminActivity, "Extension Success!", Toast.LENGTH_SHORT).show()
                        extendPinInput.text.clear()
                        extendMinutesInput.text.clear()
                        extendAmountInput.text.clear()
                        loadActivePins()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AdminActivity, "Invalid PIN!", Toast.LENGTH_SHORT).show()
                }
            }
            withContext(Dispatchers.Main) {
                extendBtn.isEnabled = true
                extendBtn.text = "EXTEND TIME"
            }
        }
    }

    private fun loadActivePins() {
        CoroutineScope(Dispatchers.IO).launch {
            val list = supabase.getActivePins()
            withContext(Dispatchers.Main) {
                if (list.isEmpty()) {
                    pinsText.text = "No active pins found."
                } else {
                    val sb = StringBuilder("🔑 ACTIVE PINS:\n\n")
                    list.forEach { sb.append("${it.pin} - ${it.secondsLeft / 60} mins left\n") }
                    pinsText.text = sb.toString()
                }
            }
        }
    }
}
