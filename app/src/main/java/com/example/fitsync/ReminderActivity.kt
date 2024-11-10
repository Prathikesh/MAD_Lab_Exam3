package com.example.fitsync

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.*

class ReminderActivity : AppCompatActivity() {
    private lateinit var etTask: EditText
    private lateinit var timePicker: TimePicker
    private lateinit var btnSetReminder: Button
    private lateinit var tvCurrentTime: TextView
    private lateinit var reminderContainer: LinearLayout
    private var alarmManager: AlarmManager? = null
    private var pendingIntent: PendingIntent? = null
    private val prefs by lazy { getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder)

        window.setStatusBarColor(getResources().getColor(R.color.custom_color_primary, null));

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.bottom_reminder

        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.bottom_reminder -> {
                    true
                }
                R.id.bottom_timer -> {
                    startActivity(Intent(applicationContext, TimerActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                R.id.bottom_task -> {
                    startActivity(Intent(applicationContext, MainActivity::class.java))
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

        etTask = findViewById(R.id.etTask)
        timePicker = findViewById(R.id.timePicker)
        btnSetReminder = findViewById(R.id.btnSetReminder)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        reminderContainer = findViewById(R.id.reminderContainer)

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        updateCurrentTime()


        btnSetReminder.setOnClickListener {
            setReminder()
        }

        loadUpcomingReminders()
    }

    private fun setReminder() {
        val task = etTask.text.toString()
        if (task.isEmpty()) {
            Toast.makeText(this, "Please enter a task", Toast.LENGTH_SHORT).show()
            return
        }

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
        calendar.set(Calendar.MINUTE, timePicker.minute)
        calendar.set(Calendar.SECOND, 0)

        val reminderTimeInMillis = calendar.timeInMillis

        println("Reminder time (millis): $reminderTimeInMillis")
        println("Current time (millis): ${System.currentTimeMillis()}")

        if (reminderTimeInMillis <= System.currentTimeMillis()) {
            Toast.makeText(this, "Please select a future time", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                requestExactAlarmPermission()
                return
            }
        }

        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("task", task)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            reminderTimeInMillis.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let { pi ->
            alarmManager?.setExact(AlarmManager.RTC_WAKEUP, reminderTimeInMillis, pi)
        }

        saveReminder(task, reminderTimeInMillis)

        loadUpcomingReminders()

        Toast.makeText(this, "Reminder set for $task", Toast.LENGTH_SHORT).show()
    }


    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            startActivity(intent)
        }
    }

    private fun updateCurrentTime() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                val calendar = Calendar.getInstance()
                val hours = calendar.get(Calendar.HOUR_OF_DAY)
                val minutes = calendar.get(Calendar.MINUTE)
                val seconds = calendar.get(Calendar.SECOND)
                tvCurrentTime.text = String.format("Current Time: %02d:%02d:%02d", hours, minutes, seconds)

                handler.postDelayed(this, 1000)
            }
        }, 0)
    }

    private fun saveReminder(task: String, reminderTimeInMillis: Long) {
        val editor = prefs.edit()
        val reminderDetails = "$task at ${timePicker.hour}:${timePicker.minute}"
        editor.putString(reminderTimeInMillis.toString(), reminderDetails)
        editor.apply()
    }

    private fun loadUpcomingReminders() {
        val allReminders = prefs.all
        reminderContainer.removeAllViews()

        for ((key, value) in allReminders) {
            // Create TextView for each reminder
            val reminderTextView = TextView(this).apply {
                text = value.toString()
                textSize = 16f  // Customize text size
                setTextColor(resources.getColor(R.color.black, null))  // Customize text color
                setPadding(16, 16, 16, 16)  // Add padding inside the TextView
                setBackgroundColor(resources.getColor(R.color.custom_color_secondary_light, null))  // Set background color
            }

            val reminderLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 8, 16, 8)
                }
            }


            val deleteButton = Button(this).apply {
                text = "Delete"
                textSize = 14f
                setPadding(16, 8, 16, 8)
                setBackgroundColor(resources.getColor(R.color.custom_color_primary, null))
                setTextColor(resources.getColor(R.color.white, null))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )

                setOnClickListener {
                    prefs.edit().remove(key).apply()
                    cancelAlarm(key.toLong())
                    loadUpcomingReminders()
                }
            }

            reminderLayout.addView(reminderTextView, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
            reminderLayout.addView(deleteButton)

            reminderContainer.addView(reminderLayout)
        }
    }


    private fun cancelAlarm(reminderTimeInMillis: Long) {
        val intent = Intent(this, ReminderReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(
            this,
            reminderTimeInMillis.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager?.cancel(it)
        }
    }

}
