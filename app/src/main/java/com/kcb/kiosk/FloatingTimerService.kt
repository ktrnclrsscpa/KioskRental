package com.kcb.kiosk

import android.app.Service
import android.content.*
import android.graphics.PixelFormat
import android.os.*
import android.speech.tts.TextToSpeech
import android.view.*
import android.widget.TextView
import java.util.*

class FloatingTimerService : Service(), TextToSpeech.OnInitListener {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var timerText: TextView
    private var countDownTimer: CountDownTimer? = null
    private var tts: TextToSpeech? = null
    private var said1Min = false; private var said30Sec = false

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        floatingView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, null)
        timerText = floatingView.findViewById(android.R.id.text1).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#E6000000"))
            setTextColor(android.graphics.Color.YELLOW)
            setPadding(20, 10, 20, 10)
            textSize = 14f
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; x = 50; y = 200 }

        floatingView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0; private var initialY = 0
            private var initialTouchX = 0f; private var initialTouchY = 0f
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x; initialY = params.y
                        initialTouchX = event.rawX; initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (initialTouchX - event.rawX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                }
                return false
            }
        })
        windowManager.addView(floatingView, params)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val seconds = intent?.getLongExtra("seconds", 0) ?: 0
        if (intent?.getBooleanExtra("isExtension", false) == true) {
            tts?.speak("Time extended. Enjoy your game.", TextToSpeech.QUEUE_FLUSH, null, null)
            said1Min = false; said30Sec = false
        }
        startTimer(seconds)
        return START_NOT_STICKY
    }

    private fun startTimer(seconds: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(seconds * 1000, 1000) {
            override fun onTick(ms: Long) {
                val s = ms / 1000
                timerText.text = String.format("%02d:%02d", s / 60, s % 60)
                if (s == 60L && !said1Min) { tts?.speak("One minute remaining", TextToSpeech.QUEUE_FLUSH, null, null); said1Min = true }
                if (s == 30L && !said30Sec) { tts?.speak("30 seconds left", TextToSpeech.QUEUE_FLUSH, null, null); said30Sec = true }
            }
            override fun onFinish() {
                tts?.speak("Time is up. Session expired.", TextToSpeech.QUEUE_FLUSH, null, null)
                val lockIntent = Intent(this@FloatingTimerService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    putExtra("locked", true)
                }
                startActivity(lockIntent)
                stopSelf()
            }
        }.start()
    }

    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US }
    override fun onDestroy() { super.onDestroy(); tts?.shutdown(); windowManager.removeView(floatingView) }
}