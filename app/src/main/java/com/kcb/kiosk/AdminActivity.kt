package com.kcb.kiosk

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class AdminActivity : AppCompatActivity() {
    
    private lateinit var supabase: SupabaseClient
    private lateinit var contentContainer: LinearLayout
    private lateinit var extendPinInput: EditText
    private lateinit var extendMinutesInput: EditText
    private lateinit var extendAmountInput: EditText
    private lateinit var extendBtn: Button
    private lateinit var pinsText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                if (input.text.toString() == "admin123") initAdminPanel()
                else { Toast.makeText(this, "Wrong Password!", Toast.LENGTH_SHORT).show(); finish() }
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

        val tab = TextView(this).apply {
            text = "PINS MANAGEMENT"
            setPadding(0, 40, 0, 40)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
        }
        
        contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
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

        contentContainer.addView(extendPinInput); contentContainer.addView(extendMinutesInput)
        contentContainer.addView(extendAmountInput); contentContainer.addView(extendBtn); contentContainer.addView(pinsText)
        
        root.addView(tab); root.addView(contentContainer)
        setContentView(root)
        loadActivePins()
    }

    private fun processExtension() {
        val pin = extendPinInput.text.toString().trim()
        val mins = extendMinutesInput.text.toString().toIntOrNull() ?: 0
        val amt = extendAmountInput.text.toString().toDoubleOrNull() ?: 0.0

        if (pin.isEmpty() || mins <= 0) return

        extendBtn.isEnabled = false
        CoroutineScope(Dispatchers.IO).launch {
            val res = supabase.validatePin(pin)
            if (res.isValid) {
                val newSec = res.secondsLeft + (mins * 60)
                if (supabase.updatePinSeconds(pin, newSec)) {
                    supabase.recordExtension(pin, mins, amt)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AdminActivity, "Success!", Toast.LENGTH_SHORT).show()
                        loadActivePins()
                    }
                }
            }
            withContext(Dispatchers.Main) { extendBtn.isEnabled = true }
        }
    }

    private fun loadActivePins() {
        CoroutineScope(Dispatchers.IO).launch {
            val list = supabase.getActivePins()
            withContext(Dispatchers.Main) {
                val sb = StringBuilder("🔑 ACTIVE PINS:\n\n")
                list.forEach { sb.append("${it.pin} - ${it.secondsLeft / 60} mins\n") }
                pinsText.text = if (list.isEmpty()) "No active pins." else sb.toString()
            }
        }
    }
}
