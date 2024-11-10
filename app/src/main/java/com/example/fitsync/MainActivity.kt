package com.example.fitsync

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var taskList: MutableList<Pair<String, String>>
    private var editTaskIndex: Int? = null

    private val taskLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val newTaskName = result.data?.getStringExtra("task")
                val newTaskDate = result.data?.getStringExtra("date")
                editTaskIndex?.let { index ->
                    // If editing, update the task at the correct index
                    newTaskName?.let { name ->
                        taskList[index] = Pair(name, newTaskDate ?: "")
                        saveTasks(taskList)
                        taskAdapter.notifyItemChanged(index)
                    }
                    editTaskIndex = null
                } ?: run {
                    // If adding, append the task to the list
                    newTaskName?.let { name ->
                        taskList.add(Pair(name, newTaskDate ?: ""))
                        saveTasks(taskList)
                        taskAdapter.notifyDataSetChanged()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.setStatusBarColor(getResources().getColor(R.color.custom_color_primary, null))

        taskList = loadTasks()

        val rvTasks = findViewById<RecyclerView>(R.id.rvTasks)
        taskAdapter = TaskAdapter(taskList, this) { task, position ->
            val intent = Intent(this, AddTaskActivity::class.java)
            intent.putExtra("task", task.first)
            intent.putExtra("date", task.second)
            taskLauncher.launch(intent)
            editTaskIndex = position
        }
        rvTasks.layoutManager = LinearLayoutManager(this)
        rvTasks.adapter = taskAdapter

        val fabAddTask: FloatingActionButton = findViewById(R.id.fabAddTask)
        fabAddTask.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java)
            taskLauncher.launch(intent)
        }

        // Add Bottom Navigation logic
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.bottom_task

        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.bottom_task -> true
                R.id.bottom_timer -> {
                    startActivity(Intent(applicationContext, TimerActivity::class.java))
                    finish()
                    true
                }
                R.id.bottom_reminder -> {
                    startActivity(Intent(applicationContext, ReminderActivity::class.java))
                    finish()
                    true
                }
                R.id.bottom_home -> {
                    startActivity(Intent(applicationContext, HomeActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadTasks(): MutableList<Pair<String, String>> {
        val sharedPreferences = getSharedPreferences("taskList", MODE_PRIVATE)
        val tasksSet = sharedPreferences.getStringSet("tasks", emptySet())
        return tasksSet?.mapNotNull {
            val parts = it.split("|")
            if (parts.size == 2) {
                Pair(parts[0], parts[1]) // First is task name, second is date
            } else {
                null
            }
        }?.toMutableList() ?: mutableListOf()
    }

    internal fun saveTasks(tasks: List<Pair<String, String>>) {
        val sharedPreferences = getSharedPreferences("taskList", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putStringSet("tasks", tasks.map { "${it.first}|${it.second}" }.toSet())
        editor.apply()
    }
}
