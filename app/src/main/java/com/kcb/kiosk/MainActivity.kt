package com.kcb.kiosk

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kcb.kiosk.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var countDownTimer: CountDownTimer? = null
    private val supabase = SupabaseClient.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStartRental.setOnClickListener {
            val pin = binding.etPin.text.toString().trim()
            if (pin.isNotEmpty()) {
                validateAndStart(pin)
            } else {
                Toast.makeText(this, "Please enter a PIN", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateAndStart(pin: String) {
        binding.btnStartRental.isEnabled = false
        lifecycleScope.launch {
            try {
                val result = supabase.validatePin(pin)
                if (result != null) {
                    supabase.usePin(pin)
                    startTimer(result.seconds_left)
                    binding.layoutLock.visibility = View.GONE
                    binding.layoutTimer.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this@MainActivity, "Invalid or Expired PIN", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Connection Error", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnStartRental.isEnabled = true
            }
        }
    }

    private fun startTimer(seconds: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(seconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = millisUntilFinished / 1000
                binding.tvTimerDisplay.text = String.format("%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60)
            }
            override fun onFinish() {
                binding.layoutLock.visibility = View.VISIBLE
                binding.layoutTimer.visibility = View.GONE
            }
        }.start()
    }
}