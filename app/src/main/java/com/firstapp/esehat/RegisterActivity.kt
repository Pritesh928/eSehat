package com.firstapp.esehat

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class RegisterActivity : AppCompatActivity() {

    private val BASE_URL = "https://esehat-auth.onrender.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val usernameField = findViewById<EditText>(R.id.signup_username)
        val emailField    = findViewById<EditText>(R.id.signup_email)
        val passwordField = findViewById<EditText>(R.id.signup_pass)
        val signupButton  = findViewById<Button>(R.id.signup_button)
        val loginRedirect = findViewById<TextView>(R.id.loginRedirectText)

        signupButton.setOnClickListener {
            val username = usernameField.text.toString().trim()
            val email    = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val json = JSONObject().apply {
                put("username", username)
                put("email", email)
                put("password", password)
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url("$BASE_URL/register").post(body).build()

            OkHttpClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread { Toast.makeText(this@RegisterActivity, "Network error", Toast.LENGTH_SHORT).show() }
                }
                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread {
                        if (response.isSuccessful) {
                            Toast.makeText(this@RegisterActivity, "Account created! Please login.", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                            finish()
                        } else {
                            val msg = response.body?.string()
                            Toast.makeText(this@RegisterActivity, "Registration failed: $msg", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }

        loginRedirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}