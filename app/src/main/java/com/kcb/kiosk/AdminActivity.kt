package com.kcb.kiosk

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AdminActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val textView = TextView(this)
        textView.text = "ADMIN ACTIVITY IS WORKING!\n\nIf you see this, the admin activity opens successfully."
        textView.textSize = 20f
        textView.gravity = android.view.Gravity.CENTER
        setContentView(textView)
    }
}
