package com.firstapp.esehat

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Base64
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.ArrayDeque
import java.util.concurrent.TimeUnit

class LiveCallActivity : AppCompatActivity() {

    private val WS_URL = "wss://callserver-9iai.onrender.com/call"

    private val languages = listOf("English", "Hindi", "Marathi", "Punjabi", "Tamil", "Telugu", "Bengali")

    private lateinit var tvStatus: TextView
    private lateinit var tvTranscript: TextView
    private lateinit var scrollTranscript: ScrollView
    private lateinit var etRoomCode: EditText
    private lateinit var spinnerLanguage: Spinner
    private lateinit var micIndicator: CardView
    private lateinit var tvMicState: TextView

    private val httpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // WebSocket connections stay open indefinitely
        .build()
    private var webSocket: WebSocket? = null

    private var speechRecognizer: SpeechRecognizer? = null
    private var listeningActive = false
    private val userId = "user_${System.currentTimeMillis()}"

    // Translated audio arrives fully-formed over the WebSocket now (no separate
    // fetch needed) — this queue just makes sure back-to-back replies play in
    // order instead of overlapping.
    private val playbackQueue = ArrayDeque<File>()
    private var isPlayingAudio = false
    private var mediaPlayer: MediaPlayer? = null

    companion object {
        private const val REQUEST_RECORD_AUDIO = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_call)

        tvStatus = findViewById(R.id.tv_status)
        tvTranscript = findViewById(R.id.tv_transcript)
        scrollTranscript = findViewById(R.id.scroll_transcript)
        etRoomCode = findViewById(R.id.et_room_code)
        spinnerLanguage = findViewById(R.id.spinner_language)
        micIndicator = findViewById(R.id.mic_indicator)
        tvMicState = findViewById(R.id.tv_mic_state)

        spinnerLanguage.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)

        findViewById<CardView>(R.id.btn_end_call).setOnClickListener { finish() }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
        } else {
            connectAndJoin()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                connectAndJoin()
            } else {
                Toast.makeText(this, "Microphone permission is required for calls", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    // ---------------- WebSocket connection ----------------

    private fun connectAndJoin() {
        // Guard against launching a second connection if this somehow gets called twice
        // (e.g. re-entering the screen without a clean exit) — a duplicate connection in
        // the same room is exactly what causes hearing your own voice echoed back.
        if (webSocket != null) return

        appendTranscript("Connecting to call server…")
        val request = Request.Builder().url(WS_URL).build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                runOnUiThread {
                    tvStatus.text = "Connected · waiting for the other side…"
                    val roomId = etRoomCode.text.toString().trim().ifEmpty { "demo-room" }
                    val lang = spinnerLanguage.selectedItem as String
                    val join = JSONObject().apply {
                        put("type", "join")
                        put("roomId", roomId)
                        put("userId", userId)
                        put("lang", lang)
                    }
                    webSocket.send(join.toString())
                    startContinuousListening()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runOnUiThread { handleServerMessage(text) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                runOnUiThread {
                    tvStatus.text = "Connection lost"
                    appendTranscript("⚠ Connection error: ${t.localizedMessage}")
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                runOnUiThread { tvStatus.text = "Call ended" }
            }
        })
    }

    private fun handleServerMessage(raw: String) {
        val json = try { JSONObject(raw) } catch (e: Exception) { return }

        when (json.optString("type")) {
            "presence" -> {
                val participants = json.optJSONArray("participants")
                val count = participants?.length() ?: 0
                tvStatus.text = if (count > 1) "Connected · $count on the call" else "Waiting for the other side…"
            }
            "room_full" -> {
                listeningActive = false
                speechRecognizer?.destroy()
                tvStatus.text = "Room is full"
                tvMicState.text = "This room already has 2 people"
                appendTranscript("⚠ This room already has 2 people on the call. Try a different room code.")
                Toast.makeText(this, "Room is full (max 2 people per call)", Toast.LENGTH_LONG).show()
            }
            "translated" -> {
                val text = json.optString("text")
                if (text.isNotBlank()) appendTranscript("Them: $text")

                val audioBase64 = json.optString("audioBase64")
                if (audioBase64.isNotBlank()) {
                    try {
                        val bytes = Base64.decode(audioBase64, Base64.DEFAULT)
                        enqueueAudio(bytes)
                    } catch (e: Exception) {
                        appendTranscript("⚠ Couldn't play voice reply")
                    }
                }
            }
        }
    }

    // ---------------- Continuous speech input ----------------

    private fun startContinuousListening() {
        listeningActive = true
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            tvMicState.text = "Voice input unavailable on this device"
            appendTranscript("⚠ This device has no speech recognition service available.")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim()
                    if (!text.isNullOrEmpty()) {
                        appendTranscript("You: $text")
                        sendSpeech(text)
                    }
                    restartListeningIfActive()
                }

                override fun onError(error: Int) {
                    restartListeningIfActive()
                }

                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        beginListeningPass()
    }

    private fun beginListeningPass() {
        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun restartListeningIfActive() {
        if (!listeningActive) return
        tvTranscript.postDelayed({ if (listeningActive) beginListeningPass() }, 300)
    }

    private fun sendSpeech(text: String) {
        val roomId = etRoomCode.text.toString().trim().ifEmpty { "demo-room" }
        val msg = JSONObject().apply {
            put("type", "speech")
            put("roomId", roomId)
            put("userId", userId)
            put("text", text)
        }
        webSocket?.send(msg.toString())
    }

    // ---------------- Translated playback (queued, audio arrives inline now) ----------------

    private fun enqueueAudio(bytes: ByteArray) {
        val file = File(cacheDir, "call_reply_${System.currentTimeMillis()}.wav")
        FileOutputStream(file).use { it.write(bytes) }
        playbackQueue.add(file)
        playNextIfIdle()
    }

    private fun playNextIfIdle() {
        if (isPlayingAudio) return
        val next = playbackQueue.poll() ?: return
        isPlayingAudio = true
        playFile(next)
    }

    private fun playFile(file: File) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnPreparedListener { it.start() }
            setOnCompletionListener {
                it.release()
                mediaPlayer = null
                file.delete()
                isPlayingAudio = false
                playNextIfIdle()
            }
            setOnErrorListener { mp, _, _ ->
                mp.release()
                mediaPlayer = null
                file.delete()
                isPlayingAudio = false
                playNextIfIdle()
                true
            }
            prepareAsync()
        }
    }

    // ---------------- Misc ----------------

    private fun appendTranscript(line: String) {
        tvTranscript.append("$line\n\n")
        scrollTranscript.post { scrollTranscript.fullScroll(android.view.View.FOCUS_DOWN) }
    }

    override fun onDestroy() {
        super.onDestroy()
        listeningActive = false
        speechRecognizer?.destroy()
        webSocket?.close(1000, "Call ended")
        webSocket = null
        mediaPlayer?.release()
    }
}