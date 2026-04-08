package com.kcb.kiosk

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class AdminActivity : AppCompatActivity() {
    private lateinit var supabase: SupabaseClient
    private val botToken = "YOUR_TELEGRAM_BOT_TOKEN"
    private val chatId = "YOUR_CHAT_ID"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supabase = SupabaseClient.getInstance()
        setupUI()
    }

    private fun setupUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val title = TextView(this).apply { text = "ADMIN DASHBOARD"; textSize = 22f; setTypeface(null, android.graphics.Typeface.BOLD) }
        
        // --- ADDITIVE EXTENSION SECTION ---
        val etPin = EditText(this).apply { hint = "Customer PIN" }
        val etAmt = EditText(this).apply { hint = "Amount (₱)"; inputType = 2 }
        val etMins = EditText(this).apply { hint = "Minutes to Add"; inputType = 2 }

        val btnExtend = Button(this).apply {
            text = "EXTEND (ADDITIVE)"
            setOnClickListener {
                val pin = etPin.text.toString()
                val mins = etMins.text.toString().toLongOrNull() ?: 0
                val amt = etAmt.text.toString().toDoubleOrNull() ?: 0.0
                if (pin.isNotEmpty() && mins > 0) processExtension(pin, mins, amt)
            }
        }
        
        val btnWhitelist = Button(this).apply { 
            text = "SELECT ALLOWED APPS"
            setOnClickListener { showAppSelector() }
        }

        root.addView(title); root.addView(etPin); root.addView(etAmt); root.addView(etMins); root.addView(btnExtend); root.addView(btnWhitelist)
        setContentView(root)
    }

    private fun processExtension(pin: String, minsToAdd: Long, amt: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            val currentSecs = supabase.getCurrentRemainingSeconds(pin) // Actual remaining
            val newTotal = currentSecs + (minsToAdd * 60)
            val success = supabase.updatePinTime(pin, newTotal, amt, true)
            
            if (success) {
                sendTelegramAlert(pin, minsToAdd, amt, newTotal)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AdminActivity, "Extended! New Total: ${newTotal/60}m", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendTelegramAlert(pin: String, added: Long, amt: Double, total: Long) {
        val msg = """
            💰 **SALES UPDATE (EXTENSION)** 💰
            PIN: `$pin`
            Added: $added mins | Amt: ₱$amt
            **New Total: ${total/60}:${String.format("%02d", total%60)}**
            📅 ${SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date())}
        """.trimIndent()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                java.net.URL("https://api.telegram.org/bot$botToken/sendMessage?chat_id=$chatId&text=${java.net.URLEncoder.encode(msg, "UTF-8")}&parse_mode=Markdown").readText()
            } catch (e: Exception) {}
        }
    }

    private fun showAppSelector() {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val allApps = packageManager.queryIntentActivities(mainIntent, 0)
        val prefs = getSharedPreferences("KioskPrefs", Context.MODE_PRIVATE)
        val current = prefs.getStringSet("allowed_packages", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        allApps.sortedBy { it.loadLabel(packageManager).toString() }.forEach { app ->
            val pkg = app.activityInfo.packageName
            val cb = CheckBox(this).apply {
                text = app.loadLabel(packageManager).toString()
                isChecked = current.contains(pkg)
                setOnCheckedChangeListener { _, checked -> if (checked) current.add(pkg) else current.remove(pkg) }
            }
            container.addView(cb)
        }
        val scroll = ScrollView(this).apply { addView(container) }
        android.app.AlertDialog.Builder(this).setTitle("Whitelist").setView(scroll)
            .setPositiveButton("SAVE") { _, _ -> prefs.edit().putStringSet("allowed_packages", current).apply() }.show()
    }
}