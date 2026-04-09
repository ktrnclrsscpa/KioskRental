package com.kcb.kiosk

import android.content.*
import android.os.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import androidx.lifecycle.lifecycleScope

class MainActivity : AppCompatActivity() {
    private lateinit var supabase: SupabaseClient
    private lateinit var appRecycler: RecyclerView
    private var currentLocalSeconds = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. I-load ang layout (Dapat may ConstraintLayout at RecyclerView ito)
        setContentView(R.layout.activity_main) 

        // 2. Initialize Supabase Client
        supabase = SupabaseClient.getInstance()
        
        // 3. Setup UI components at Admin Access
        setupUI()
        
        // Check kung galing sa Lock screen
        if (intent.getBooleanExtra("locked", false)) {
            applyLockUI()
        }
    }

    private fun setupUI() {
        // I-bind ang Title Text para sa Hidden Admin Access
        val titleText = findViewById<TextView>(R.id.titleText)
        
        // HIDDEN ADMIN ACCESS: Long press sa "KCB RENTAL" para pumasok sa Admin Settings
        titleText.setOnLongClickListener {
            val intent = Intent(this, AdminActivity::class.java)
            startActivity(intent)
            true // Ibig sabihin na-consume ang long click
        }

        // I-bind at i-setup ang RecyclerView para sa App Icons
        appRecycler = findViewById(R.id.appRecycler)
        appRecycler.layoutManager = GridLayoutManager(this, 4) // 4 icons per row
    }

    override fun onResume() {
        super.onResume()
        // I-refresh ang listahan ng apps tuwing babalik sa home
        loadWhitelistedApps()
    }

    private fun loadWhitelistedApps() {
        val prefs = getSharedPreferences("KioskPrefs", Context.MODE_PRIVATE)
        val savedApps = prefs.getStringSet("allowed_packages", setOf())
        
        if (savedApps.isNullOrEmpty()) {
            // Paalala kung wala pang napiling apps sa Admin Settings
            Toast.makeText(this, "Go to Admin Settings to select apps", Toast.LENGTH_LONG).show()
        } else {
            // TODO: Dito mo ikakabit ang Adapter mo sa susunod na step
            // Halimbawa: appRecycler.adapter = AppAdapter(savedApps.toList())
        }
    }

    private fun applyLockUI() {
        // Logic para sa Kiosk Lock Mode
        Toast.makeText(this, "Kiosk Mode Active", Toast.LENGTH_SHORT).show()
    }
}