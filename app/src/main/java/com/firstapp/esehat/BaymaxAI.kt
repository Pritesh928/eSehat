package com.firstapp.esehat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.firstapp.esehat.databinding.ActivityBaymaxAiBinding
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class BaymaxAI : AppCompatActivity() {

    private lateinit var binding: ActivityBaymaxAiBinding
    private val scope = MainScope()
    private var started = false
    private var facts = "{}"

    private val BASE_URL = "https://baymaxai.onrender.com"

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    // ---- Voice ----
    private var speechOutputEnabled = false
    private var conversationModeActive = false
    private var mediaPlayer: MediaPlayer? = null

    private lateinit var input: EditText
    private lateinit var output: TextView
    private lateinit var micButton: ImageButton
    private lateinit var speakerToggle: ImageButton

    companion object {
        private const val REQUEST_RECORD_AUDIO = 1001
    }

    private val speechLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val text = matches?.firstOrNull()?.trim()
                if (!text.isNullOrEmpty()) {
                    setListeningVisual(false)
                    sendMessage(text, spokenReply = true)
                } else {
                    onRecognitionFailed()
                }
            } else {
                onRecognitionFailed()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBaymaxAiBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_baymax_ai)

        input = findViewById(R.id.chatInput)
        output = findViewById(R.id.chatOutput)
        micButton = findViewById(R.id.micButton)
        speakerToggle = findViewById(R.id.speakerToggle)
        val sendButton: ImageButton = findViewById(R.id.sendButton)

        sendButton.setOnClickListener {
            val userMessage = input.text.toString().trim()
            if (userMessage.isEmpty()) return@setOnClickListener
            input.text.clear()
            sendMessage(userMessage)
        }

        micButton.setOnClickListener { onMicTapped() }

        speakerToggle.setOnClickListener {
            speechOutputEnabled = !speechOutputEnabled
            speakerToggle.setImageResource(
                if (speechOutputEnabled) android.R.drawable.ic_lock_silent_mode
                else android.R.drawable.ic_lock_silent_mode_off
            )
            if (!speechOutputEnabled) mediaPlayer?.let { if (it.isPlaying) it.stop() }
            Toast.makeText(
                this,
                if (speechOutputEnabled) "Baymax will speak replies aloud" else "Spoken replies off",
                Toast.LENGTH_SHORT
            ).show()
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

    private fun sendMessage(userMessage: String, spokenReply: Boolean = false) {
        output.append("\nYou:\n$userMessage\n")

        scope.launch {
            val reply = fetchReply(userMessage, output)
            output.append("\nBaymax:\n$reply\n")

            if (spokenReply || speechOutputEnabled) {
                speak(reply, continueConversation = spokenReply && conversationModeActive)
            } else if (conversationModeActive) {
                startListeningRound()
            }
        }
    }

    // ---------------- Voice input (Speech-to-Text) ----------------

    private fun onMicTapped() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
            return
        }
        if (conversationModeActive) {
            stopConversationMode()
        } else {
            conversationModeActive = true
            startListeningRound()
        }
    }

    private fun startListeningRound() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(
                this,
                "Voice input isn't available — this device has no speech recognition service enabled.",
                Toast.LENGTH_LONG
            ).show()
            stopConversationMode()
            return
        }

        setListeningVisual(true)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe your symptom…")
        }
        speechLauncher.launch(intent)
    }

    private fun onRecognitionFailed() {
        setListeningVisual(false)
        val wasInConversation = conversationModeActive
        stopConversationMode()
        Toast.makeText(
            this,
            if (wasInConversation) "Didn't catch that — tap the mic to talk again"
            else "Didn't catch that — try again",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun stopConversationMode() {
        conversationModeActive = false
        setListeningVisual(false)
        mediaPlayer?.let { if (it.isPlaying) it.stop() }
    }

    private fun setListeningVisual(listening: Boolean) {
        micButton.setImageResource(
            if (listening) android.R.drawable.ic_media_pause else android.R.drawable.ic_btn_speak_now
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onMicTapped()
            } else {
                Toast.makeText(this, "Microphone permission is needed for voice input", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---------------- Voice output — Gemini-generated speech, played back ----------------

    private fun speak(text: String, continueConversation: Boolean = false) {
        scope.launch {
            try {
                val audioFile = withContext(Dispatchers.IO) { fetchSpeechAudio(text) }
                playAudio(audioFile, continueConversation)
            } catch (e: Exception) {
                // Network hiccup or TTS failure — don't break the conversation loop over it.
                Toast.makeText(this@BaymaxAI, "Couldn't play voice reply", Toast.LENGTH_SHORT).show()
                if (continueConversation) startListeningRound()
            }
        }
    }

    /** Calls the backend's /tts route and saves the returned WAV bytes to a cache file. */
    private suspend fun fetchSpeechAudio(text: String): File = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(text, "UTF-8")
        val request = Request.Builder()
            .addHeader("Authorization", "Bearer 12345")
            .url("$BASE_URL/tts?message=$encoded")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("TTS request failed: ${response.code}")
            val bytes = response.body?.bytes() ?: throw Exception("Empty TTS response")
            val file = File(cacheDir, "baymax_reply_${System.currentTimeMillis()}.wav")
            FileOutputStream(file).use { it.write(bytes) }
            file
        }
    }

    private fun playAudio(file: File, continueConversation: Boolean) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnPreparedListener { it.start() }
            setOnCompletionListener {
                it.release()
                mediaPlayer = null
                file.delete()
                if (continueConversation) startListeningRound()
            }
            setOnErrorListener { mp, _, _ ->
                mp.release()
                mediaPlayer = null
                file.delete()
                if (continueConversation) startListeningRound()
                true
            }
            prepareAsync()
        }
    }

    override fun onPause() {
        super.onPause()
        // Don't stop conversation mode here — launching the speech dialog also triggers onPause,
        // and we don't want that to cancel the loop. Just silence any audio mid-playback.
        mediaPlayer?.let { if (it.isPlaying) it.stop() }
    }

    override fun onDestroy() {
        super.onDestroy()
        conversationModeActive = false
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // ---------------- Backend call (unchanged) ----------------

    private suspend fun fetchReply(message: String, output: TextView): String {
        return withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(message, "UTF-8")
                val url = if (!started) "$BASE_URL/chats/create?message=$encoded" else "$BASE_URL/chats/follow?message=$encoded&facts=$facts"
                val request = Request.Builder().addHeader("Authorization", "Bearer 12345").url(url).build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@use "There was an error, please try again"
                    }
                    val body = response.body?.string() ?: return@use "Empty response"
                    try {
                        val json = org.json.JSONObject(body)
                        val extracted = json.optString("t", "")
                        val decision = json.optString("a", "")
                        facts = json.get("u").toString()
                        if (extracted.isNotEmpty()) {
                            started = true
                            if (decision == "assess") started = false
                            return@use extracted
                        } else body
                    } catch (e: Exception) {
                        return@use body
                    }
                } ?: return@withContext "No response"
            } catch (e: Exception) {
                return@withContext "There was an error: ${e.localizedMessage}"
            }
        }
    }
}