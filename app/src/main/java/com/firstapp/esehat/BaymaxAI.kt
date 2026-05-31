package com.firstapp.esehat
import android.util.Log
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.firstapp.esehat.databinding.ActivityBaymaxAiBinding
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class BaymaxAI : AppCompatActivity() {

    private lateinit var binding: ActivityBaymaxAiBinding
    private val scope = MainScope()
    private var started = false

    private var facts = "{}"

    private val BASE_URL = "http://193.24.210.2:6699"
    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBaymaxAiBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_baymax_ai)

        val input: EditText = findViewById<EditText>(R.id.chatInput)
        val sendButton: ImageButton = findViewById<ImageButton>(R.id.sendButton)
        val output: TextView = findViewById<TextView>(R.id.chatOutput)
        sendButton.setOnClickListener {
            Log.d("debug", "clicked")
            val userMessage = input.text.toString().trim()

            if (userMessage.isEmpty()) return@setOnClickListener

            output.append("\nYou:\n$userMessage\n")
            input.text.clear()

            scope.launch {
                val reply = fetchReply(userMessage, output)
                output.append("\nBaymax:\n$reply\n")
            }

        }


        val homeButton : ImageButton = findViewById<ImageButton>(R.id.homebtn)
        homeButton.setOnClickListener {
            val intent3 = Intent(this, MainActivity::class.java)
            startActivity(intent3)
        }
        val healthButton : ImageButton = findViewById<ImageButton>(R.id.healthtrackbtn)
        healthButton.setOnClickListener {
            val intent4 = Intent(this, HealthTracker::class.java)
            startActivity(intent4)
        }
        val videoconsultButton : ImageButton = findViewById<ImageButton>(R.id.videoconsult)
        videoconsultButton.setOnClickListener {
            val intent5 = Intent(this, VideoConsult::class.java)
            startActivity(intent5)
        }
        val AIButton : ImageButton = findViewById<ImageButton>(R.id.baymaxAI)
        AIButton.setOnClickListener {
            val intent6 = Intent(this, BaymaxAI::class.java)
            startActivity(intent6)
        }
        val profileButton : ImageButton = findViewById<ImageButton>(R.id.profileBtn)
        profileButton.setOnClickListener {
            val intent7 = Intent(this, Profilepage::class.java)
            startActivity(intent7)
        }

    }

    private suspend fun fetchReply(message: String, output: TextView): String {
        return withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(message, "UTF-8")
                val url = if (!started) "$BASE_URL/chats/create?message=$encoded" else "$BASE_URL/chats/follow?message=$encoded&facts=$facts"
                val request = Request.Builder().addHeader("Authorization", "Bearer 12345").url(url).build()

                client.newCall(request).execute().use {
                        response ->
                    if (!response.isSuccessful) {
                        return@use "There was an error, please try again"
                    }

                    val body = response.body?.string() ?: return@use "Empty response"

                    try {
                        val json = org.json.JSONObject(body)
                        val extracted = json.optString("t", "")
                        val decision = json.optString("a", "")

                        val factsObj = json.get("u")
                        facts = if (factsObj is org.json.JSONObject || factsObj is org.json.JSONArray) {
                            factsObj.toString()
                        } else {
                            factsObj.toString()
                        }
                        if (extracted.isNotEmpty()) {
                            started = true
                            if (decision == "assess") {
                                started = false
                            }
                            return@use extracted
                        } else body
                    } catch (e: Exception) {
                        return@use body
                    }
                } ?: return@withContext "No response"
            }
            catch (e: Exception) {
                return@withContext "There was an error: ${e.localizedMessage}"
            }
        }
    }
}
