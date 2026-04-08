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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supabase = SupabaseClient.getInstance()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val t = TextView(this).apply { text = "KCB RENTAL"; textSize = 30f; setPadding(0,0,0,40) }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        pinInput = EditText(this).apply { hint = "PIN"; layoutParams = LinearLayout.LayoutParams(0,-2,1f) }
        val btn = Button(this).apply { text = "GO"; setOnClickListener { act() } }
        row.addView(pinInput); row.addView(btn)
        timerText = TextView(this).apply { text = "00:00"; textSize = 40f; setPadding(0,40,0,40) }
        appRecycler = RecyclerView(this).apply { layoutManager = GridLayoutManager(this@MainActivity, 3) }
        root.addView(t); root.addView(row); root.addView(timerText); root.addView(appRecycler)
        setContentView(root)
        load()
    }

    private fun load() {
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(intent, 0).map {
            AppInfo(it.loadLabel(packageManager).toString(), it.activityInfo.packageName)
        }
        appRecycler.adapter = AppAdapter(apps, packageManager)
    }

    private fun act() {
        val p = pinInput.text.toString()
        if (p == "000000") { startActivity(Intent(this, AdminActivity::class.java)); return }
        CoroutineScope(Dispatchers.IO).launch {
            val res = supabase.validatePin(p)
            withContext(Dispatchers.Main) {
                if (res.isValid) start(res.secondsLeft.toLong())
                else Toast.makeText(this@MainActivity, "Invalid", 0).show()
            }
        }
    }

    private fun start(s: Long) {
        timer?.cancel()
        timer = object : CountDownTimer(s * 1000, 1000) {
            override fun onTick(ms: Long) {
                val sec = ms / 1000
                timerText.text = String.format("%02d:%02d", sec/60, sec%60)
            }
            override fun onFinish() { timerText.text = "EXPIRED" }
        }.start()
    }
}