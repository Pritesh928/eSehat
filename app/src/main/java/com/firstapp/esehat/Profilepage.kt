package com.firstapp.esehat

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class Profilepage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profilepage)

        val logoutBtn = findViewById<Button>(R.id.logoutBtn)
        logoutBtn.setOnClickListener {

            val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
            prefs.edit().putBoolean("isLoggedIn", false).apply()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        val homeButton = findViewById<ImageButton>(R.id.homebtn)
        homeButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        val healthButton = findViewById<ImageButton>(R.id.healthtrackbtn)
        healthButton.setOnClickListener {
            startActivity(Intent(this, HealthTracker::class.java))
        }

        val videoconsultButton = findViewById<ImageButton>(R.id.videoconsult)
        videoconsultButton.setOnClickListener {
            startActivity(Intent(this, VideoConsult::class.java))
        }

        val AIButton = findViewById<ImageButton>(R.id.baymaxAI)
        AIButton.setOnClickListener {
            startActivity(Intent(this, BaymaxAI::class.java))
        }

        val profileButton = findViewById<ImageButton>(R.id.profileBtn)
        profileButton.setOnClickListener {
            startActivity(Intent(this, Profilepage::class.java))
        }
    }
}