package com.example.fitsync

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView


class TimerActivity : AppCompatActivity() {
    private lateinit var tvTimer: TextView
    private lateinit var etTimeLimit: EditText
    private lateinit var btnStart: Button
    private lateinit var btnPause: Button
    private lateinit var btnReset: Button
    private lateinit var btnTaskRun: Button
    private lateinit var btnTaskCycle: Button

    private var isRunning = false
    private var isCountdown = false
    private var startTime = 0L
    private var elapsedTime = 0L
    private var pauseTime = 0L
    private var countdownTimeInMillis: Long = 0L
    private var remainingCountdownTime = 0L
    private var timeLimitInMillis: Long? = null
    private var handler = Handler(Looper.getMainLooper())
    private var alarmManager: AlarmManager? = null
    private var pendingIntent: PendingIntent? = null

    // SharedPreferences to store the state of the timer
    private val prefs by lazy { getSharedPreferences("timer_prefs", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        window.setStatusBarColor(getResources().getColor(R.color.custom_color_primary, null));

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.bottom_timer

        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.bottom_timer -> {
                    true
                }
                R.id.bottom_task -> {
                    startActivity(Intent(applicationContext, MainActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                R.id.bottom_reminder -> {
                    startActivity(Intent(applicationContext, ReminderActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                R.id.bottom_home -> {
                    startActivity(Intent(applicationContext, HomeActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                else -> false
            }
        }

        tvTimer = findViewById(R.id.tvTimer)
        etTimeLimit = findViewById(R.id.etTimeLimit)
        btnStart = findViewById(R.id.btnStart)
        btnPause = findViewById(R.id.btnPause)
        btnReset = findViewById(R.id.btnReset)
        btnTaskRun = findViewById(R.id.btnTaskRun)
        btnTaskCycle = findViewById(R.id.btnTaskCycle)

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        restoreTimerState()

        btnTaskRun.setOnClickListener {
            countdownTimeInMillis = 2 * 60 * 1000
            isCountdown = true
            startCountdownTimer(countdownTimeInMillis)
        }

        btnTaskCycle.setOnClickListener {
            countdownTimeInMillis = 5 * 60 * 1000
            isCountdown = true
            startCountdownTimer(countdownTimeInMillis)
        }

        btnStart.setOnClickListener {
            if (!isRunning) {
                val timeLimitInSeconds = etTimeLimit.text.toString().toLongOrNull()
                timeLimitInMillis = timeLimitInSeconds?.times(1000) ?: 0L
                isCountdown = false
                startStopwatch(timeLimitInMillis!!)
            }
        }

        btnPause.setOnClickListener {
            if (isRunning) {
                pauseTimer()
            }
        }

        btnReset.setOnClickListener {
            resetTimer()
        }
    }

    private fun startStopwatch(timeLimit: Long) {
        isRunning = true
        startTime = System.currentTimeMillis() - pauseTime
        saveTimerState()

        handler.postDelayed(object : Runnable {
            override fun run() {
                elapsedTime = System.currentTimeMillis() - startTime
                updateTimerUI(elapsedTime)

                if (timeLimit > 0 && elapsedTime >= timeLimit) {
                    stopTimer()
                    triggerAlarm()
                } else {
                    handler.postDelayed(this, 1000)
                }
            }
        }, 0)

        if (timeLimit > 0) {
            setAlarmForTimer(timeLimit)
        }
    }

    private fun startCountdownTimer(timeInMillis: Long) {
        isRunning = true
        remainingCountdownTime = if (remainingCountdownTime > 0) remainingCountdownTime else timeInMillis
        startTime = System.currentTimeMillis()
        saveTimerState()

        handler.postDelayed(object : Runnable {
            override fun run() {
                val currentElapsedTime = System.currentTimeMillis() - startTime
                val remainingTime = remainingCountdownTime - currentElapsedTime
                updateTimerUI(remainingTime)

                if (remainingTime <= 0) {
                    stopTimer()
                    triggerAlarm()
                } else {
                    handler.postDelayed(this, 1000)
                }
            }
        }, 0)

        setAlarmForTimer(timeInMillis)
    }

    private fun updateTimerUI(timeInMillis: Long) {
        val hours = (timeInMillis / (1000 * 60 * 60)) % 24
        val minutes = (timeInMillis / (1000 * 60)) % 60
        val seconds = (timeInMillis / 1000) % 60
        tvTimer.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun pauseTimer() {
        isRunning = false
        if (isCountdown) {
            remainingCountdownTime -= System.currentTimeMillis() - startTime
        } else {
            pauseTime = System.currentTimeMillis() - startTime  // Save the elapsed time when paused
        }
        handler.removeCallbacksAndMessages(null)
        cancelAlarm()
        saveTimerState()
    }

    private fun resetTimer() {
        isRunning = false
        pauseTime = 0L
        elapsedTime = 0L
        countdownTimeInMillis = 0L
        remainingCountdownTime = 0L
        timeLimitInMillis = null
        handler.removeCallbacksAndMessages(null)
        tvTimer.text = "00:00:00"
        cancelAlarm()
        clearTimerState()
    }

    private fun stopTimer() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        cancelAlarm()
        clearTimerState()
    }

    private fun setAlarmForTimer(timeInMillis: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                requestExactAlarmPermission()
                return
            }
        }

        val intent = Intent(this, AlarmReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let { pi ->
            alarmManager?.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeInMillis, pi)
        }
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            startActivity(intent)
        }
    }

    private fun cancelAlarm() {
        pendingIntent?.let { alarmManager?.cancel(it) }
    }

    private fun triggerAlarm() {
        Toast.makeText(this, "Time's up!", Toast.LENGTH_SHORT).show()
        cancelAlarm()
    }

    private fun saveTimerState() {
        val editor = prefs.edit()
        editor.putBoolean("isRunning", isRunning)
        editor.putLong("startTime", startTime)
        editor.putLong("pauseTime", pauseTime)
        editor.putLong("elapsedTime", elapsedTime)
        editor.putBoolean("isCountdown", isCountdown)
        editor.putLong("remainingCountdownTime", remainingCountdownTime)
        editor.putLong("timeLimitInMillis", timeLimitInMillis ?: 0L)
        editor.putLong("savedSystemTime", System.currentTimeMillis())
        editor.apply()
    }


    private fun restoreTimerState() {
        isRunning = prefs.getBoolean("isRunning", false)
        pauseTime = prefs.getLong("pauseTime", 0L)
        elapsedTime = prefs.getLong("elapsedTime", 0L)
        remainingCountdownTime = prefs.getLong("remainingCountdownTime", 0L)
        isCountdown = prefs.getBoolean("isCountdown", false)
        timeLimitInMillis = prefs.getLong("timeLimitInMillis", 0L)
        val savedSystemTime = prefs.getLong("savedSystemTime", 0L)
        val currentSystemTime = System.currentTimeMillis()

        if (isRunning) {
            val timeDiff = currentSystemTime - savedSystemTime

            if (isCountdown) {
                remainingCountdownTime -= timeDiff
                startCountdownTimer(remainingCountdownTime)
            } else {
                startTime += timeDiff
                startStopwatch(timeLimitInMillis!!)
            }
        }
    }

    private fun clearTimerState() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}






