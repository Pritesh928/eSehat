package com.firstapp.esehat

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class SplashScreen : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)

        sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE)

        Handler(Looper.getMainLooper()).postDelayed({
            checkUserStatus()
        }, 3000)
    }

    private fun checkUserStatus() {
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)
        if (isLoggedIn) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        finish()
    }
}