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

class RegisterActivity : AppCompatActivity() {

    private val BASE_URL = "https://esehat-auth.onrender.com"

    private lateinit var signupButton: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val usernameField =
            findViewById<EditText>(R.id.signup_username)

        val emailField =
            findViewById<EditText>(R.id.signup_email)

        val passwordField =
            findViewById<EditText>(R.id.signup_pass)

        signupButton =
            findViewById(R.id.signup_button)

        progressBar =
            findViewById(R.id.signup_progress)

        val passwordEye =
            findViewById<ImageButton>(R.id.signup_password_eye)

        val loginRedirect =
            findViewById<TextView>(R.id.loginRedirectText)

        var passwordVisible = false

        passwordEye.setOnClickListener {

            passwordVisible = !passwordVisible

            if (passwordVisible) {

                passwordField.inputType =
                    InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

                passwordEye.setImageResource(
                    android.R.drawable.ic_menu_view
                )

                passwordField.setSelection(
                    passwordField.text.length
                )

            } else {

                passwordField.inputType =
                    InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_VARIATION_PASSWORD

                passwordEye.setImageResource(
                    android.R.drawable.ic_menu_view
                )

                passwordField.setSelection(
                    passwordField.text.length
                )
            }
        }

        signupButton.setOnClickListener {

            val username =
                usernameField.text
                    .toString()
                    .trim()

            val email =
                emailField.text
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

            if (email.isEmpty()) {
                emailField.error =
                    "Enter email"
                emailField.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordField.error =
                    "Enter password"
                passwordField.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                passwordField.error =
                    "Password must be at least 6 characters"
                passwordField.requestFocus()
                return@setOnClickListener
            }

            setLoading(true)

            val json =
                JSONObject().apply {
                    put("username", username)
                    put("email", email)
                    put("password", password)
                }

            val body =
                json.toString()
                    .toRequestBody(
                        "application/json".toMediaType()
                    )

            val request =
                Request.Builder()
                    .url("$BASE_URL/register")
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
                                    this@RegisterActivity,
                                    "Server is waking up. Please try again.",
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

                                    Toast.makeText(
                                        this@RegisterActivity,
                                        "Account created successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    val intent =
                                        Intent(
                                            this@RegisterActivity,
                                            LoginActivity::class.java
                                        )

                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP

                                    startActivity(intent)

                                    finish()

                                } else {

                                    Toast.makeText(
                                        this@RegisterActivity,
                                        if (responseText.isNotEmpty())
                                            responseText
                                        else
                                            "Registration failed. Please try again.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                )
        }

        loginRedirect.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    LoginActivity::class.java
                )
            )
        }
    }

    private fun setLoading(
        loading: Boolean
    ) {

        signupButton.isEnabled =
            !loading

        signupButton.text =
            if (loading)
                "Creating account..."
            else
                "Create Account"

        progressBar.visibility =
            if (loading)
                View.VISIBLE
            else
                View.GONE
    }
}