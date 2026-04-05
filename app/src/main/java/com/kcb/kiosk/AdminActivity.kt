package com.kcb.kiosk

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.text.NumberFormat
import java.util.*

class AdminActivity : AppCompatActivity() {
    
    private lateinit var supabase: SupabaseClient
    private lateinit var scrollView: ScrollView
    private lateinit var mainContainer: LinearLayout
    
    private val prefs by lazy { getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Simple test view muna para malaman kung gumagana ang activity
        val testLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 100, 50, 50)
        }
        
        val welcomeText = TextView(this).apply {
            text = "ADMIN PANEL - WORKING!"
            textSize = 24f
            gravity = android.view.Gravity.CENTER
        }
        testLayout.addView(welcomeText)
        
        val closeBtn = Button(this).apply {
            text = "CLOSE"
            setOnClickListener {
                finish()
            }
        }
        testLayout.addView(closeBtn)
        
        setContentView(testLayout)
        
        Toast.makeText(this, "Admin panel opened successfully!", Toast.LENGTH_LONG).show()
    }
}
