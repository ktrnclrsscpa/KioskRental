package com.kcb.kiosk

import android.content.*
import android.os.*
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var supabase: SupabaseClient
    private lateinit var timerText: TextView
    private lateinit var pinInput: EditText
    private lateinit var appRecycler: RecyclerView
    private var currentLocalSeconds = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supabase = SupabaseClient.getInstance()
        setupUI()
        if (intent.getBooleanExtra("locked", false)) applyLockUI()
    }

    private fun setupUI() {
        // ... (UI construction similar to previous examples)
    }

    override fun onResume() {
        super.onResume()
        loadWhitelistedApps()
    }

    private fun handleAction() {
        val pin = pinInput.text.toString().trim()
        if (pin == "000000") { startActivity(Intent(this, AdminActivity::class.java)); return }
        
        CoroutineScope(Dispatchers.IO).launch {
            val res = supabase.validatePin(pin)
            if (res.isValid) {
                withContext(Dispatchers.Main) {
                    startGlobalTimer(res.secondsLeft.toLong(), false)
                    listenForRemoteExtension(pin)
                }
            }
        }
    }

    private fun listenForRemoteExtension(pin: String) {
        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(10000)
                val updated = supabase.getCurrentRemainingSeconds(pin)
                if (updated > currentLocalSeconds + 15) { // Threshold for extension
                    withContext(Dispatchers.Main) { startGlobalTimer(updated, true) }
                }
            }
        }
    }

    private fun startGlobalTimer(seconds: Long, isExt: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            return
        }
        currentLocalSeconds = seconds
        val intent = Intent(this, FloatingTimerService::class.java).apply {
            putExtra("seconds", seconds); putExtra("isExtension", isExt)
        }
        startService(intent)
    }

    private fun applyLockUI() {
        appRecycler.alpha = 0.2f
        appRecycler.isEnabled = false
        Toast.makeText(this, "LOCKED: Please Extend Time", Toast.LENGTH_LONG).show()
    }
}