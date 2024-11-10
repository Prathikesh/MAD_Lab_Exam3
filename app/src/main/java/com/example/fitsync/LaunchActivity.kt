package com.example.fitsync

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LaunchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch);

        window.setStatusBarColor(getResources().getColor(R.color.custom_color_primary, null));

        Handler().postDelayed({
            val intent = Intent(this@LaunchActivity, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }
}