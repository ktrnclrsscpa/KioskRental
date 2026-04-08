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

    data class PinValidationResult(val isValid: Boolean, val secondsLeft: Int)
    private lateinit var supabase: SupabaseClient
    private lateinit var timerText: TextView
    private lateinit var pinInput: EditText
    private lateinit var appRecycler: RecyclerView
    private var countDownTimer: CountDownTimer? = null

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
            text = "KCB RENTAL"
            textSize = 30f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 40)
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2)
        }
        pinInput = EditText(this).apply { 
            hint = "6-digit PIN"
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            gravity = Gravity.CENTER
        }
        val activateBtn = Button(this).apply { 
            text = "ACTIVATE"
            setOnClickListener { handleActivation() }
        }
        row.addView(pinInput); row.addView(activateBtn)

        timerText = TextView(this).apply {
            text = "00 : 00"
            textSize = 45f
            setPadding(0, 40, 0, 40)
            setTextColor(android.graphics.Color.RED)
        }

        appRecycler = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@MainActivity, 3)
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }

        root.addView(title); root.addView(row); root.addView(timerText); root.addView(appRecycler)
        setContentView(root)
        
        loadApps()
    }

    private fun loadApps() {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(mainIntent, 0).map {
            AppInfo(it.loadLabel(packageManager).toString(), it.activityInfo.packageName)
        }
        appRecycler.adapter = AppAdapter(apps, packageManager)
    }

    private fun handleActivation() {
        val pin = pinInput.text.toString().trim()
        if (pin == "000000") {
            startActivity(Intent(this, AdminActivity::class.java))
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val res = supabase.validatePin(pin)
            withContext(Dispatchers.Main) {
                if (res.isValid) {
                    startTimer(res.secondsLeft.toLong())
                    pinInput.text.clear()
                } else {
                    Toast.makeText(this@MainActivity, "Invalid PIN", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startTimer(sec: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(sec * 1000, 1000) {
            override fun onTick(ms: Long) {
                val total = ms / 1000
                timerText.text = String.format("%02d : %02d", total / 60, total % 60)
            }
            override fun onFinish() { timerText.text = "EXPIRED" }
        }.start()
    }
}
