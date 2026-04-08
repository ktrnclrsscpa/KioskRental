package com.kcb.kiosk

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var supabase: SupabaseClient
    private lateinit var timerText: TextView
    private lateinit var pinInput: EditText
    private lateinit var activateBtn: Button
    private lateinit var appRecycler: RecyclerView
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // --- UI RECOVERY ---
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }

        val title = TextView(this).apply {
            text = "KCB RENTAL"
            textSize = 32f
            setPadding(0, 0, 0, 40)
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2)
        }
        pinInput = EditText(this).apply { 
            hint = "6-digit PIN"
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        activateBtn = Button(this).apply { text = "ACTIVATE" }
        row.addView(pinInput); row.addView(activateBtn)

        timerText = TextView(this).apply {
            text = "-- : --"
            textSize = 48f
            setPadding(0, 50, 0, 50)
        }

        appRecycler = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@MainActivity, 3)
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }

        root.addView(title); root.addView(row); root.addView(timerText); root.addView(appRecycler)
        setContentView(root)

        // --- LOGIC ---
        supabase = SupabaseClient.getInstance()
        
        activateBtn.setOnClickListener {
            val pin = pinInput.text.toString()
            if (pin == "000000") { // Default Admin Shortcut
                startActivity(Intent(this, AdminActivity::class.java))
            } else {
                handlePinActivation(pin)
            }
        }
    }

    private fun handlePinActivation(pin: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = supabase.validatePin(pin)
            withContext(Dispatchers.Main) {
                if (result.isValid) {
                    startTimer(result.secondsLeft.toLong())
                    Toast.makeText(this@MainActivity, "Activated!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Invalid PIN", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startTimer(seconds: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(seconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = millisUntilFinished / 1000
                val min = sec / 60
                val remainingSec = sec % 60
                timerText.text = String.format("%02d : %02d", min, remainingSec)
            }
            override fun onFinish() {
                timerText.text = "EXPIRED"
                // Logic to lock apps or clear session
            }
        }.start()
    }
}
