package com.firstapp.esehat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Base64
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.ArrayDeque
import java.util.concurrent.TimeUnit

class LiveCallActivity : AppCompatActivity() {

    private val WS_URL =
        "wss://callserver-9iai.onrender.com/call"

    private val languages = listOf(
        "English",
        "Hindi",
        "Marathi",
        "Punjabi",
        "Tamil",
        "Telugu",
        "Bengali"
    )

    private lateinit var tvStatus: TextView
    private lateinit var tvTranscript: TextView
    private lateinit var tvCallHint: TextView
    private lateinit var tvMicState: TextView
    private lateinit var tvYouName: TextView
    private lateinit var tvOtherName: TextView
    private lateinit var tvYouInitial: TextView
    private lateinit var tvOtherInitial: TextView
    private lateinit var tvYourLanguage: TextView
    private lateinit var tvOtherLanguage: TextView

    private lateinit var etRoomCode: EditText
    private lateinit var spinnerLanguage: Spinner

    private lateinit var btnStartCall: CardView
    private lateinit var btnEndCall: CardView
    private lateinit var btnMic: CardView

    private val httpClient =
        OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

    private var webSocket: WebSocket? = null

    private val userId =
        "user_${System.currentTimeMillis()}"

    private var userName = "Participant"
    private var otherName = "Waiting..."

    private var listeningActive = false
    private var callConnected = false
    private var callStarted = false

    private var speechRecognizer: SpeechRecognizer? = null

    private val playbackQueue = ArrayDeque<File>()

    private var mediaPlayer: MediaPlayer? = null
    private var isPlayingAudio = false

    private lateinit var audioManager: AudioManager

    companion object {
        private const val REQUEST_RECORD_AUDIO = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_live_call)

        audioManager =
            getSystemService(AUDIO_SERVICE) as AudioManager

        tvStatus = findViewById(R.id.tv_status)
        tvTranscript = findViewById(R.id.tv_transcript)
        tvCallHint = findViewById(R.id.tv_call_hint)
        tvMicState = findViewById(R.id.tv_mic_state)

        tvYouName = findViewById(R.id.tv_you_name)
        tvOtherName = findViewById(R.id.tv_other_name)

        tvYouInitial = findViewById(R.id.tv_you_initial)
        tvOtherInitial = findViewById(R.id.tv_other_initial)

        tvYourLanguage = findViewById(R.id.tv_your_language)
        tvOtherLanguage = findViewById(R.id.tv_other_language)

        etRoomCode = findViewById(R.id.et_room_code)
        spinnerLanguage = findViewById(R.id.spinner_language)

        btnStartCall = findViewById(R.id.btn_start_call)
        btnEndCall = findViewById(R.id.btn_end_call)
        btnMic = findViewById(R.id.btn_mic)

        userName =
            intent.getStringExtra("user_name")
                ?: "Participant"

        tvYouName.text = userName
        tvYouInitial.text =
            userName.firstOrNull()?.uppercase()
                ?: "Y"

        spinnerLanguage.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                languages
            )

        spinnerLanguage.setSelection(
            languages.indexOf("English")
        )

        updateLanguageUI()

        spinnerLanguage.setOnItemSelectedListener(
            object : android.widget.AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    updateLanguageUI()
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) {
                }
            }
        )

        btnStartCall.setOnClickListener {
            startCall()
        }

        btnMic.setOnClickListener {
            if (!callStarted) {
                return@setOnClickListener
            }

            if (listeningActive) {
                stopListening()
            } else {
                startListening()
            }
        }

        btnEndCall.setOnClickListener {
            endCall()
        }

        findViewById<CardView>(R.id.btn_back).setOnClickListener {
            endCall()
        }
    }

    private fun updateLanguageUI() {
        val language =
            spinnerLanguage.selectedItem?.toString()
                ?: "English"

        tvYourLanguage.text = language
    }

    private fun startCall() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO
            )
            return
        }

        if (callStarted) {
            return
        }

        callStarted = true

        btnStartCall.visibility = View.GONE
        btnEndCall.visibility = View.VISIBLE
        btnMic.visibility = View.VISIBLE

        tvStatus.text = "Connecting..."
        tvMicState.text = "Connecting..."
        tvCallHint.text =
            "Waiting for the other participant..."

        connectWebSocket()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == REQUEST_RECORD_AUDIO &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startCall()
        } else {
            Toast.makeText(
                this,
                "Microphone permission is required.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun connectWebSocket() {

        val request =
            Request.Builder()
                .url(WS_URL)
                .build()

        webSocket =
            httpClient.newWebSocket(
                request,
                object : WebSocketListener() {

                    override fun onOpen(
                        webSocket: WebSocket,
                        response: Response
                    ) {
                        runOnUiThread {
                            joinRoom(webSocket)
                        }
                    }

                    override fun onMessage(
                        webSocket: WebSocket,
                        text: String
                    ) {
                        runOnUiThread {
                            handleServerMessage(text)
                        }
                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        t: Throwable,
                        response: Response?
                    ) {
                        runOnUiThread {

                            callConnected = false
                            listeningActive = false

                            tvStatus.text = "Connection failed"
                            tvMicState.text = "Stopped"

                            appendTranscript(
                                "Connection failed. Please try again."
                            )

                            btnStartCall.visibility =
                                View.VISIBLE

                            btnEndCall.visibility =
                                View.GONE
                        }
                    }

                    override fun onClosed(
                        webSocket: WebSocket,
                        code: Int,
                        reason: String
                    ) {
                        runOnUiThread {

                            callConnected = false
                            listeningActive = false

                            tvStatus.text = "Call ended"
                            tvMicState.text = "Stopped"
                        }
                    }
                }
            )
    }

    private fun joinRoom(
        socket: WebSocket
    ) {

        val roomId =
            etRoomCode.text
                .toString()
                .trim()
                .ifEmpty {
                    "demo-room"
                }

        val language =
            spinnerLanguage.selectedItem
                ?.toString()
                ?: "English"

        val join =
            JSONObject().apply {

                put("type", "join")
                put("roomId", roomId)
                put("userId", userId)
                put("name", userName)

                put("sourceLang", language)
                put("targetLang", language)
                put("language", language)
            }

        socket.send(
            join.toString()
        )
    }

    private fun handleServerMessage(
        raw: String
    ) {

        val json =
            try {
                JSONObject(raw)
            } catch (_: Exception) {
                return
            }

        when (json.optString("type")) {

            "presence" -> {

                val count =
                    json.optInt(
                        "count",
                        json.optJSONArray(
                            "participants"
                        )?.length() ?: 0
                    )

                if (count >= 2) {

                    callConnected = true

                    tvStatus.text =
                        "Connected"

                    tvCallHint.text =
                        "Both participants are connected."

                    tvMicState.text =
                        "Ready"

                    val participants =
                        json.optJSONArray(
                            "participants"
                        )

                    if (participants != null) {

                        for (i in 0 until participants.length()) {

                            val participant =
                                participants.optJSONObject(i)
                                    ?: continue

                            val id =
                                participant.optString("userId")

                            val name =
                                participant.optString(
                                    "name",
                                    "Participant"
                                )

                            val language =
                                participant.optString(
                                    "language",
                                    participant.optString(
                                        "sourceLang",
                                        ""
                                    )
                                )

                            if (id == userId) {

                                tvYouName.text =
                                    name

                                tvYouInitial.text =
                                    name.firstOrNull()
                                        ?.uppercase()
                                        ?: "Y"

                            } else {

                                otherName = name

                                tvOtherName.text =
                                    name

                                tvOtherInitial.text =
                                    name.firstOrNull()
                                        ?.uppercase()
                                        ?: "P"

                                tvOtherLanguage.text =
                                    language
                            }
                        }
                    }

                    appendTranscript(
                        "✓ Both participants connected."
                    )

                } else {

                    callConnected = false

                    listeningActive = false

                    speechRecognizer?.cancel()

                    tvStatus.text =
                        "Waiting for participant"

                    tvCallHint.text =
                        "Share the room code with the other participant."

                    tvMicState.text =
                        "Waiting..."
                }
            }

            "translated" -> {

                val text =
                    json.optString("text")

                val original =
                    json.optString(
                        "originalText"
                    )

                val fromName =
                    json.optString(
                        "fromName",
                        otherName
                    )

                if (text.isNotBlank()) {

                    appendTranscript(
                        "$fromName\n$original\n\n$text"
                    )
                }
            }

            "audio" -> {

                val audioBase64 =
                    json.optString(
                        "audioBase64"
                    )

                if (audioBase64.isNotBlank()) {

                    try {

                        val bytes =
                            Base64.decode(
                                audioBase64,
                                Base64.DEFAULT
                            )

                        enqueueAudio(bytes)

                    } catch (_: Exception) {

                        appendTranscript(
                            "Voice playback failed."
                        )
                    }
                }
            }

            "translation_error" -> {

                val message =
                    json.optString(
                        "message",
                        "Translation failed."
                    )

                appendTranscript(
                    "⚠ $message"
                )
            }

            "audio_error" -> {

                val message =
                    json.optString(
                        "message",
                        "Voice generation failed."
                    )

                appendTranscript(
                    "⚠ $message"
                )
            }

            "error" -> {

                val code =
                    json.optInt("code")

                val message =
                    json.optString(
                        "message",
                        "Call error."
                    )

                if (code == 403) {

                    callConnected = false
                    listeningActive = false

                    speechRecognizer?.cancel()

                    tvStatus.text =
                        "Room Full"

                    tvMicState.text =
                        "Unavailable"

                    tvCallHint.text =
                        "Only two participants are allowed."

                    Toast.makeText(
                        this,
                        "This room already has two participants.",
                        Toast.LENGTH_LONG
                    ).show()

                } else {

                    appendTranscript(
                        "⚠ $message"
                    )
                }
            }
        }
    }

    private fun startListening() {

        if (!callConnected) {
            Toast.makeText(
                this,
                "Waiting for the other participant.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {

            Toast.makeText(
                this,
                "Speech recognition is unavailable.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        if (listeningActive) {
            return
        }

        listeningActive = true

        tvMicState.text =
            "Listening..."

        beginListening()
    }

    private fun beginListening() {

        if (!listeningActive ||
            !callConnected ||
            isPlayingAudio
        ) {
            return
        }

        if (speechRecognizer == null) {

            speechRecognizer =
                SpeechRecognizer
                    .createSpeechRecognizer(this)
                    .apply {

                        setRecognitionListener(
                            object :
                                RecognitionListener {

                                override fun onReadyForSpeech(
                                    params: Bundle?
                                ) {
                                    tvMicState.text =
                                        "Listening..."
                                }

                                override fun onBeginningOfSpeech() {
                                    tvMicState.text =
                                        "Listening..."
                                }

                                override fun onEndOfSpeech() {
                                    tvMicState.text =
                                        "Processing..."
                                }

                                override fun onResults(
                                    results: Bundle?
                                ) {

                                    val matches =
                                        results?.getStringArrayList(
                                            SpeechRecognizer.RESULTS_RECOGNITION
                                        )

                                    val text =
                                        matches
                                            ?.firstOrNull()
                                            ?.trim()

                                    if (
                                        !text.isNullOrEmpty() &&
                                        callConnected &&
                                        listeningActive
                                    ) {

                                        appendTranscript(
                                            "You:\n$text"
                                        )

                                        sendSpeech(text)
                                    }

                                    restartListening()
                                }

                                override fun onError(
                                    error: Int
                                ) {
                                    restartListening()
                                }

                                override fun onRmsChanged(
                                    rmsdB: Float
                                ) {
                                }

                                override fun onBufferReceived(
                                    buffer: ByteArray?
                                ) {
                                }

                                override fun onPartialResults(
                                    partialResults: Bundle?
                                ) {
                                }

                                override fun onEvent(
                                    eventType: Int,
                                    params: Bundle?
                                ) {
                                }
                            }
                        )
                    }
        }

        val language =
            spinnerLanguage.selectedItem
                ?.toString()
                ?: "English"

        val locale =
            when (language) {
                "English" -> "en-IN"
                "Hindi" -> "hi-IN"
                "Marathi" -> "mr-IN"
                "Punjabi" -> "pa-IN"
                "Tamil" -> "ta-IN"
                "Telugu" -> "te-IN"
                "Bengali" -> "bn-IN"
                else -> "en-IN"
            }

        val intent =
            Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    locale
                )

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                    locale
                )

                putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS,
                    1
                )

                putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    false
                )
            }

        try {
            speechRecognizer?.startListening(intent)
        } catch (_: Exception) {
            restartListening()
        }
    }

    private fun restartListening() {

        if (!listeningActive ||
            !callConnected
        ) {
            return
        }

        window.decorView.postDelayed({

            if (
                listeningActive &&
                callConnected &&
                !isPlayingAudio
            ) {
                beginListening()
            }

        }, 350)
    }

    private fun stopListening() {

        listeningActive = false

        speechRecognizer?.cancel()

        tvMicState.text =
            "Mic off"
    }

    private fun sendSpeech(
        text: String
    ) {

        val socket =
            webSocket ?: return

        val roomId =
            etRoomCode.text
                .toString()
                .trim()
                .ifEmpty {
                    "demo-room"
                }

        val language =
            spinnerLanguage.selectedItem
                ?.toString()
                ?: "English"

        val message =
            JSONObject().apply {

                put("type", "speech")
                put("roomId", roomId)
                put("userId", userId)
                put("name", userName)
                put("sourceLang", language)
                put("text", text)
            }

        socket.send(
            message.toString()
        )
    }

    private fun enqueueAudio(
        bytes: ByteArray
    ) {

        try {

            val file =
                File(
                    cacheDir,
                    "call_${System.currentTimeMillis()}.wav"
                )

            FileOutputStream(file).use {
                it.write(bytes)
            }

            playbackQueue.add(file)

            playNextAudio()

        } catch (_: Exception) {

            appendTranscript(
                "Voice playback failed."
            )
        }
    }

    private fun playNextAudio() {

        if (isPlayingAudio) {
            return
        }

        val file =
            playbackQueue.poll()
                ?: return

        isPlayingAudio = true

        speechRecognizer?.cancel()

        tvMicState.text =
            "Other participant speaking..."

        mediaPlayer?.release()

        mediaPlayer =
            MediaPlayer().apply {

                try {

                    setDataSource(
                        file.absolutePath
                    )

                    setOnPreparedListener {
                        it.start()
                    }

                    setOnCompletionListener {

                        it.release()

                        mediaPlayer = null

                        file.delete()

                        isPlayingAudio = false

                        if (listeningActive) {

                            tvMicState.text =
                                "Listening..."

                            restartListening()
                        }

                        playNextAudio()
                    }

                    setOnErrorListener {
                            mp,
                            _,
                            _ ->

                        mp.release()

                        mediaPlayer = null

                        file.delete()

                        isPlayingAudio = false

                        if (listeningActive) {
                            restartListening()
                        }

                        playNextAudio()

                        true
                    }

                    prepareAsync()

                } catch (_: Exception) {

                    release()

                    mediaPlayer = null

                    file.delete()

                    isPlayingAudio = false

                    playNextAudio()
                }
            }
    }

    private fun appendTranscript(
        text: String
    ) {

        tvTranscript.append(
            "$text\n\n"
        )

        findViewById<android.widget.ScrollView>(
            R.id.scroll_transcript
        ).post {

            findViewById<android.widget.ScrollView>(
                R.id.scroll_transcript
            ).fullScroll(
                View.FOCUS_DOWN
            )
        }
    }

    private fun endCall() {

        callStarted = false
        callConnected = false
        listeningActive = false

        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null

        playbackQueue.clear()

        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        webSocket?.close(
            1000,
            "Call ended"
        )

        webSocket = null

        tvStatus.text =
            "Call ended"

        tvMicState.text =
            "Stopped"

        btnStartCall.visibility =
            View.VISIBLE

        btnEndCall.visibility =
            View.GONE

        btnMic.visibility =
            View.GONE
    }

    override fun onBackPressed() {

        endCall()

        super.onBackPressed()
    }

    override fun onDestroy() {

        callStarted = false
        callConnected = false
        listeningActive = false

        speechRecognizer?.cancel()
        speechRecognizer?.destroy()

        speechRecognizer = null

        mediaPlayer?.release()
        mediaPlayer = null

        webSocket?.close(
            1000,
            "Activity destroyed"
        )

        webSocket = null

        super.onDestroy()
    }
}