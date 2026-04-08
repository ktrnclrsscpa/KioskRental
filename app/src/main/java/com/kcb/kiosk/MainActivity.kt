package com.kcb.kiosk

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    data class PinRes(val isValid: Boolean, val secondsLeft: Int)
    private lateinit var supabase: SupabaseClient
    private lateinit var timerText: TextView
    private lateinit var pinInput: EditText
    private lateinit var appRecycler: RecyclerView
    private var timer: CountDownTimer? = null

    // --- DITO MO ILALAGAY ANG WHITELIST ---
    // Magdagdag o magbawas ng package names dito
    private val allowedApps = listOf(
        "com.android.chrome",           // Chrome
        "com.google.android.youtube",    // YouTube
        "com.brave.browser",            // Brave
        "com.kcb.kiosk",                // Itong App mo
        "com.facebook.katana"           // Facebook (sample)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supabase = SupabaseClient.getInstance()
        
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(android.graphics.Color.WHITE)
        }

        val title = TextView(this).apply { 
            text = "KCB RENTAL"; textSize = 30f; setPadding(0, 0, 0, 40)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        pinInput = EditText(this).apply { 
            hint = "Enter PIN"; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) 
        }
        val btn = Button(this).apply { 
            text = "GO"
            setOnClickListener { handleAction() } 
        }
        row.addView(pinInput); row.addView(btn)

        timerText = TextView(this).apply { 
            text = "00 : 00"; textSize = 45f; setPadding(0, 40, 0, 40)
            setTextColor(android.graphics.Color.RED)
        }
        
        appRecycler = RecyclerView(this).apply { 
            layoutManager = GridLayoutManager(this@MainActivity, 3) 
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }

        root.addView(title); root.addView(row); root.addView(timerText); root.addView(appRecycler)
        setContentView(root)
        
        loadFilteredApps()
    }

    private fun loadFilteredApps() {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val allApps = packageManager.queryIntentActivities(mainIntent, 0)
        
        val filteredList = allApps.filter { 
            allowedApps.contains(it.activityInfo.packageName) 
        }.map {
            AppInfo(it.loadLabel(packageManager).toString(), it.activityInfo.packageName)
        }
        
        appRecycler.adapter = AppAdapter(filteredList, packageManager)
    }

    private fun handleAction() {
        val pin = pinInput.text.toString().trim()
        
        // --- ADMIN SHORTCUT ---
        if (pin == "000000") {
            startActivity(Intent(this, AdminActivity::class.java))
            return
        }

        if (pin.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            val res = supabase.validatePin(pin)
            withContext(Dispatchers.Main) {
                if (res.isValid && res.secondsLeft > 0) {
                    startTimer(res.secondsLeft.toLong())
                    pinInput.text.clear()
                } else {
                    Toast.makeText(this@MainActivity, "Invalid PIN", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startTimer(seconds: Long) {
        timer?.cancel()
        timer = object : CountDownTimer(seconds * 1000, 1000) {
            override fun onTick(ms: Long) {
                val s = ms / 1000
                timerText.text = String.format("%02d : %02d", s / 60, s % 60)
            }
            override fun onFinish() { timerText.text = "EXPIRED" }
        }.start()
    }
}
