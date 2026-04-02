package com.kcb.kiosk

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AdminActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val textView = TextView(this)
        textView.text = "✅ ADMIN PANEL\n\nPIN Management and App Selection coming soon!"
        textView.textSize = 20f
        textView.gravity = android.view.Gravity.CENTER
        setContentView(textView)
    }
}
