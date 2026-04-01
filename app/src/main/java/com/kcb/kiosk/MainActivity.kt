package com.kcb.kiosk

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create a simple TextView programmatically
        val textView = TextView(this)
        textView.text = "KCB RENTAL\n\nApp is working!"
        textView.textSize = 24f
        textView.gravity = android.view.Gravity.CENTER
        
        setContentView(textView)
    }
}
