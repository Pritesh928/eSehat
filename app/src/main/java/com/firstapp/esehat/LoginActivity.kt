package com.firstapp.esehat

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private val BASE_URL = "https://esehat-auth.onrender.com"

    private lateinit var loginButton: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_login
        )

        val usernameField =
            findViewById<EditText>(
                R.id.login_username
            )

        val passwordField =
            findViewById<EditText>(
                R.id.login_password
            )

        loginButton =
            findViewById(
                R.id.login_button
            )

        progressBar =
            findViewById(
                R.id.login_progress
            )

        val passwordEye =
            findViewById<ImageButton>(
                R.id.login_password_eye
            )

        val signupRedirect =
            findViewById<TextView>(
                R.id.signupRedirectText
            )

        var passwordVisible = false

        passwordEye.setOnClickListener {

            passwordVisible =
                !passwordVisible

            if (passwordVisible) {

                passwordField.inputType =
                    InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

            } else {

                passwordField.inputType =
                    InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            passwordField.setSelection(
                passwordField.text.length
            )
        }

        loginButton.setOnClickListener {

            val username =
                usernameField.text
                    .toString()
                    .trim()

            val password =
                passwordField.text
                    .toString()
                    .trim()

            if (username.isEmpty()) {

                usernameField.error =
                    "Enter username"

                usernameField.requestFocus()

                return@setOnClickListener
            }

            if (password.isEmpty()) {

                passwordField.error =
                    "Enter password"

                passwordField.requestFocus()

                return@setOnClickListener
            }

            setLoading(true)

            val json =
                JSONObject().apply {

                    put(
                        "username",
                        username
                    )

                    put(
                        "password",
                        password
                    )
                }

            val body =
                json.toString()
                    .toRequestBody(
                        "application/json".toMediaType()
                    )

            val request =
                Request.Builder()
                    .url("$BASE_URL/login")
                    .post(body)
                    .build()

            OkHttpClient()
                .newCall(request)
                .enqueue(
                    object : Callback {

                        override fun onFailure(
                            call: Call,
                            e: IOException
                        ) {

                            runOnUiThread {

                                setLoading(false)

                                Toast.makeText(
                                    this@LoginActivity,
                                    "Server is waking up. Please try again in a few seconds.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        override fun onResponse(
                            call: Call,
                            response: Response
                        ) {

                            val responseText =
                                response.body
                                    ?.string()
                                    ?: ""

                            runOnUiThread {

                                setLoading(false)

                                if (response.isSuccessful) {

                                    val prefs =
                                        getSharedPreferences(
                                            "UserSession",
                                            MODE_PRIVATE
                                        )

                                    prefs.edit()
                                        .putBoolean(
                                            "isLoggedIn",
                                            true
                                        )
                                        .putString(
                                            "username",
                                            username
                                        )
                                        .apply()

                                    val savedRole =
                                        prefs.getString(
                                            "userRole",
                                            null
                                        )

                                    if (
                                        savedRole.isNullOrEmpty()
                                    ) {

                                        val intent =
                                            Intent(
                                                this@LoginActivity,
                                                RoleSelection::class.java
                                            )

                                        intent.flags =
                                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                                    Intent.FLAG_ACTIVITY_CLEAR_TASK

                                        startActivity(intent)

                                    } else {

                                        openDashboard(
                                            savedRole
                                        )
                                    }

                                    finish()

                                } else {

                                    val message =
                                        when {

                                            response.code == 401 ->
                                                "Invalid username or password"

                                            response.code == 404 ->
                                                "Login service is unavailable"

                                            response.code >= 500 ->
                                                "Server is waking up. Please try again."

                                            responseText.isNotEmpty() ->
                                                responseText

                                            else ->
                                                "Login failed. Please try again."
                                        }

                                    Toast.makeText(
                                        this@LoginActivity,
                                        message,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                )
        }

        signupRedirect.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    RegisterActivity::class.java
                )
            )
        }
    }

    private fun setLoading(
        loading: Boolean
    ) {

        loginButton.isEnabled =
            !loading

        loginButton.text =
            if (loading)
                "Signing in..."
            else
                "Login"

        progressBar.visibility =
            if (loading)
                View.VISIBLE
            else
                View.GONE
    }

    private fun openDashboard(
        role: String
    ) {

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

                else ->
                    RoleSelection::class.java
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
    }
}