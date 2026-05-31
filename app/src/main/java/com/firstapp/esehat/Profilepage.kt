package com.firstapp.esehat

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Profilepage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profilepage)


        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val username = prefs.getString("username", "User")
        val email = prefs.getString("userEmail", "No email saved")

        findViewById<TextView>(R.id.username_text).text = username
        findViewById<TextView>(R.id.userEmail).text = email


        findViewById<LinearLayout>(R.id.logoutBtn).setOnClickListener {
            prefs.edit().putBoolean("isLoggedIn", false).apply()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }


        findViewById<androidx.cardview.widget.CardView>(R.id.medic_card).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<androidx.cardview.widget.CardView>(R.id.consult_card).setOnClickListener {
            startActivity(Intent(this, VideoConsult::class.java))
        }
        findViewById<androidx.cardview.widget.CardView>(R.id.baymax_card).setOnClickListener {
            startActivity(Intent(this, BaymaxAI::class.java))
        }

        findViewById<LinearLayout>(R.id.btnEditProfile).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }


        findViewById<ImageButton>(R.id.homebtn).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<ImageButton>(R.id.healthtrackbtn).setOnClickListener {
            startActivity(Intent(this, HealthTracker::class.java))
        }
        findViewById<ImageButton>(R.id.videoconsult).setOnClickListener {
            startActivity(Intent(this, VideoConsult::class.java))
        }
        findViewById<ImageButton>(R.id.baymaxAI).setOnClickListener {
            startActivity(Intent(this, BaymaxAI::class.java))
        }
        findViewById<ImageButton>(R.id.profileBtn).setOnClickListener {
            startActivity(Intent(this, Profilepage::class.java))
        }
    }
}