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

        sharedPref = getSharedPreferences(
            "UserSession",
            MODE_PRIVATE
        )

        Handler(Looper.getMainLooper()).postDelayed({
            checkUserStatus()
        }, 1500)
    }

    private fun checkUserStatus() {

        val isLoggedIn =
            sharedPref.getBoolean(
                "isLoggedIn",
                false
            )

        if (!isLoggedIn) {

            startActivity(
                Intent(
                    this,
                    RegisterActivity::class.java
                )
            )

            finish()
            return
        }

        val savedRole =
            sharedPref.getString(
                "userRole",
                null
            )

        if (savedRole.isNullOrEmpty()) {

            startActivity(
                Intent(
                    this,
                    RoleSelection::class.java
                )
            )

            finish()
            return
        }

        openDashboard(savedRole)
    }

    private fun openDashboard(role: String) {

        val target =
            when (role) {

                UserRole.DOCTOR.name ->
                    DoctorDashboard::class.java

                UserRole.PATIENT.name ->
                    MainActivity::class.java

                UserRole.ADMIN.name ->
                    AdminDashboardActivity::class.java

                UserRole.ASHA_WORKER.name ->
                    AshaDashboardActivity::class.java

                else -> {
                    RoleSelection::class.java
                }
            }

        val intent =
            Intent(
                this,
                target
            )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }
}