package com.example.fitsync

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AddTaskActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        window.setStatusBarColor(getResources().getColor(R.color.custom_color_primary, null));


        val etTaskName = findViewById<EditText>(R.id.etTaskName)
        val etTaskDate = findViewById<EditText>(R.id.etTaskDate)

        val existingTask = intent.getStringExtra("task")
        val existingDate = intent.getStringExtra("date")
        if (existingTask != null) {
            etTaskName.setText(existingTask)
        }
        if (existingDate != null) {
            etTaskDate.setText(existingDate)
        }


        val btnSaveTask = findViewById<Button>(R.id.btnSaveTask)
        btnSaveTask.setOnClickListener {
            val taskName = etTaskName.text.toString()
            val taskDate = etTaskDate.text.toString()
            if (taskName.isNotEmpty() && taskDate.isNotEmpty()) {
                val resultIntent = Intent()
                resultIntent.putExtra("task", taskName)
                resultIntent.putExtra("date", taskDate)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }
}
