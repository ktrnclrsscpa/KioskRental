package com.kcb.kiosk

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class AdminActivity : AppCompatActivity() {
    
    private lateinit var supabase: SupabaseClient
    private val prefs by lazy { getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE) }
    
    private lateinit var contentContainer: LinearLayout
    private lateinit var tabBar: LinearLayout
    
    private lateinit var salesContent: LinearLayout
    private lateinit var pinsContent: LinearLayout
    private lateinit var appsContent: LinearLayout
    private lateinit var settingsContent: LinearLayout
    
    private lateinit var dailyTotalText: TextView
    private lateinit var transactionRecycler: RecyclerView
    private lateinit var pinsText: TextView
    private lateinit var appContainer: LinearLayout
    private lateinit var appStatusText: TextView
    private lateinit var saveAppsBtn: Button
    private val checkBoxes = mutableListOf<Pair<CheckBox, String>>()
    private lateinit var telegramTokenInput: EditText
    private lateinit var telegramChatIdInput: EditText
    private lateinit var saveTelegramBtn: Button
    private lateinit var testTelegramBtn: Button
    
    private lateinit var extendPinInput: EditText
    private lateinit var extendMinutesInput: EditText
    private lateinit var extendAmountInput: EditText
    private lateinit var extendBtn: Button
    
    private var currentTab = "sales"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAdminPassword()
    }
    
    private fun checkAdminPassword() {
        val input = EditText(this)
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
                    initAdminPanel()
                } else {
                    Toast.makeText(this, "Wrong password!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("Cancel") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }
    
    private fun initAdminPanel() {
        supabase = SupabaseClient.getInstance()
        
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F5F7FA"))
        }
        
        // --- Header Section ---
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(30, 30, 30, 20)
        }
        val title = TextView(this).apply {
            text = "ADMIN PANEL"
            textSize = 22f
            setTextColor(Color.parseColor("#2C3E50"))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        headerRow.addView(title)
        rootLayout.addView(headerRow)
        
        // --- Tab Bar ---
        tabBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            elevation = 4f
        }
        addTab("SALES", "sales")
        addTab("PINS", "pins")
        addTab("APPS", "apps")
        addTab("SETTINGS", "settings")
        rootLayout.addView(tabBar)
        
        contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        rootLayout.addView(contentContainer)
        
        // Pre-create contents
        salesContent = createSalesContent()
        pinsContent = createPinsContent()
        appsContent = createAppsContent()
        settingsContent = createSettingsContent()
        
        setContentView(rootLayout)
        selectTab("sales")
        loadInstalledApps()
        loadTelegramSettings()
    }

    private fun addTab(label: String, tag: String) {
        val tab = TextView(this).apply {
            text = label
            textSize = 13f
            setPadding(20, 30, 20, 30)
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            this.tag = tag
            setOnClickListener { selectTab(tag) }
        }
        tabBar.addView(tab)
    }

    private fun selectTab(tag: String) {
        currentTab = tag
        contentContainer.removeAllViews()
        for (i in 0 until tabBar.childCount) {
            val tv = tabBar.getChildAt(i) as TextView
            if (tv.tag == tag) {
                tv.setTextColor(Color.parseColor("#2ECC71"))
                tv.setTypeface(null, Typeface.BOLD)
            } else {
                tv.setTextColor(Color.GRAY)
                tv.setTypeface(null, Typeface.NORMAL)
            }
        }
        when (tag) {
            "sales" -> { contentContainer.addView(salesContent); loadTransactionHistory() }
            "pins" -> { contentContainer.addView(pinsContent); loadActivePins() }
            "apps" -> contentContainer.addView(appsContent)
            "settings" -> contentContainer.addView(settingsContent)
        }
    }

    // ==================== CORE LOGIC: EXTEND TIME ====================
    private fun extendTime(pin: String, minutes: Int, amount: Double) {
        extendBtn.isEnabled = false
        extendBtn.text = "PROCESSING..."
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // STEP 1: Kunin ang saktong oras na natitira sa DB ngayon
                val currentData = supabase.validatePin(pin)
                
                if (currentData.isValid) {
                    // STEP 2: I-compute ang New Total (Current DB Oras + Dagdag)
                    val addedSeconds = minutes * 60
                    val newTotalSeconds = currentData.secondsLeft + addedSeconds
                    
                    // STEP 3: I-save sa Supabase
                    val success = supabase.updatePinSeconds(pin, newTotalSeconds)
                    
                    if (success) {
                        supabase.recordExtension(pin, minutes, amount)
                        supabase.sendTelegramNotification("⏰ *Extended!*%0APIN: $pin%0AAdded: $minutes min%0ANew Total: ${newTotalSeconds/60} min")
                        
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@AdminActivity, "Successfully added $minutes min!", Toast.LENGTH_SHORT).show()
                            clearExtendInputs()
                            loadActivePins()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AdminActivity, "PIN not found or expired!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AdminActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    extendBtn.isEnabled = true
                    extendBtn.text = "EXTEND TIME"
                }
            }
        }
    }

    private fun clearExtendInputs() {
        extendPinInput.text.clear()
        extendMinutesInput.text.clear()
        extendAmountInput.text.clear()
    }

    // ==================== UI GENERATORS ====================
    private fun createPinsContent(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
            
            addView(TextView(context).apply { text = "⏰ EXTEND ACTIVE PIN"; typeface = Typeface.DEFAULT_BOLD })
            
            extendPinInput = EditText(context).apply { hint = "Enter PIN" }
            extendMinutesInput = EditText(context).apply { hint = "Minutes to add"; inputType = 2 }
            extendAmountInput = EditText(context).apply { hint = "Amount (₱)"; inputType = 8194 }
            
            extendBtn = Button(context).apply {
                text = "EXTEND TIME"
                setBackgroundColor(Color.parseColor("#E67E22"))
                setTextColor(Color.WHITE)
                setOnClickListener {
                    val pin = extendPinInput.text.toString()
                    val min = extendMinutesInput.text.toString().toIntOrNull() ?: 0
                    val amt = extendAmountInput.text.toString().toDoubleOrNull() ?: 0.0
                    if (pin.isNotEmpty() && min > 0) extendTime(pin, min, amt)
                }
            }
            
            addView(extendPinInput); addView(extendMinutesInput); addView(extendAmountInput); addView(extendBtn)
            
            addView(TextView(context).apply { text = "\n🔑 ACTIVE PINS"; setPadding(0, 20, 0, 10) })
            pinsText = TextView(context).apply { text = "Loading..." }
            addView(pinsText)
        }
    }

    // (Ilagay dito ang iba pang functions gaya ng createSalesContent, loadTransactionHistory, atbp. mula sa dating code)
    private fun createSalesContent() = LinearLayout(this).apply { /* Logic same as before */ }
    private fun createAppsContent() = LinearLayout(this).apply { /* Logic same as before */ }
    private fun createSettingsContent() = LinearLayout(this).apply { /* Logic same as before */ }
    private fun loadTransactionHistory() { /* Logic same as before */ }
    private fun loadActivePins() { /* Logic same as before */ }
    private fun loadInstalledApps() { /* Logic same as before */ }
    private fun loadTelegramSettings() { /* Logic same as before */ }
}
