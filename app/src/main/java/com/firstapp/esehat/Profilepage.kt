package com.firstapp.esehat

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.widget.ImageViewCompat
import androidx.core.content.ContextCompat
import android.app.Activity
import android.graphics.Bitmap
import androidx.activity.result.contract.ActivityResultContracts

class Profilepage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profilepage)

        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val username = prefs.getString("username", "User")
        val email = prefs.getString("userEmail", "No email saved")

        findViewById<TextView>(R.id.username_text).text = username
        findViewById<TextView>(R.id.userEmail).text = email

        findViewById<LinearLayout>(R.id.btnEditProfile).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnNotifications).setOnClickListener {
            startActivity(Intent(this, HelplineCallActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnPrivacy).setOnClickListener {
            startActivity(Intent(this, HealthTracker::class.java))
        }

        findViewById<CardView>(R.id.editPhotoBadge).setOnClickListener {
            val startCameraLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    // Get the low-resolution thumbnail bitmap from the intent extras
                    val imageBitmap = result.data?.extras?.get("data") as? Bitmap

                    // Do something with the bitmap (e.g., display it in an ImageView)
                    // myImageView.setImageBitmap(imageBitmap)
                }
            }
        }

        findViewById<LinearLayout>(R.id.logoutBtn).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Log out?")
                .setMessage("You'll need to sign in again to access your account.")
                .setPositiveButton("Logout") { _, _ ->
                    prefs.edit().putBoolean("isLoggedIn", false).apply()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        setupBottomNav()
    }

    private fun setupBottomNav() {
        val home = findViewById<ImageButton>(R.id.homebtn)
        val healthTrack = findViewById<ImageButton>(R.id.healthtrackbtn)
        val videoConsult = findViewById<ImageButton>(R.id.videoconsult)
        val baymax = findViewById<ImageButton>(R.id.baymaxAI)
        val profile = findViewById<ImageButton>(R.id.profileBtn)

        home.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
        healthTrack.setOnClickListener { startActivity(Intent(this, HealthTracker::class.java)) }
        videoConsult.setOnClickListener { startActivity(Intent(this, VideoConsult::class.java)) }
        baymax.setOnClickListener { startActivity(Intent(this, BaymaxAI::class.java)) }

        profile.isEnabled = false
    }
}