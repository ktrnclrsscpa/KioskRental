package com.kcb.kiosk

import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class AdminActivity : AppCompatActivity() {
    private lateinit var s: SupabaseClient
    private lateinit var pInput: EditText
    private lateinit var mInput: EditText
    private lateinit var aInput: EditText
    private lateinit var log: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        s = SupabaseClient.getInstance()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40,40,40,40) }
        pInput = EditText(this).apply { hint = "PIN" }
        mInput = EditText(this).apply { hint = "Mins"; inputType = 2 }
        aInput = EditText(this).apply { hint = "₱"; inputType = 8194 }
        val btn = Button(this).apply { text = "EXTEND"; setOnClickListener { process() } }
        log = TextView(this).apply { text = "Active pins..."; setPadding(0,40,0,0) }
        root.addView(pInput); root.addView(mInput); root.addView(aInput); root.addView(btn); root.addView(log)
        setContentView(root)
        refresh()
    }

    private fun process() {
        val p = pInput.text.toString()
        val m = mInput.text.toString().toIntOrNull() ?: 0
        val a = aInput.text.toString().toDoubleOrNull() ?: 0.0
        CoroutineScope(Dispatchers.IO).launch {
            val res = s.validatePin(p)
            if (res.isValid) {
                if (s.updatePinSeconds(p, res.secondsLeft + (m*60))) {
                    s.recordExtension(p, m, a)
                    withContext(Dispatchers.Main) { refresh() }
                }
            }
        }
    }

    private fun refresh() {
        CoroutineScope(Dispatchers.IO).launch {
            val list = s.getActivePins()
            withContext(Dispatchers.Main) {
                log.text = list.joinToString("\n") { "${it.pin}: ${it.secondsLeft/60}m" }
            }
        }
    }
}