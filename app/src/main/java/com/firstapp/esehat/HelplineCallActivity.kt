package com.firstapp.esehat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.util.Locale

class HelplineCallActivity : AppCompatActivity() {

    private val TARGET_PHONE_NUMBER = "+918828787229"

    private lateinit var tts: TextToSpeech
    private var ttsReady = false

    private lateinit var contactListView: View
    private lateinit var callScreenView: View
    private lateinit var keypadDrawer: View
    private lateinit var tvStatus: TextView
    private lateinit var answerBtn: View
    private lateinit var endCallBtn: View

    private val scope = MainScope()
    private var callTimerJob: Job? = null
    private var seconds = 0
    private var isAnswered = false

    companion object {
        private const val REQUEST_CALL_PHONE = 3001
        private const val UTTERANCE_ID = "helpline_tts"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_helpline_call)

        contactListView = findViewById(R.id.contactListView)
        callScreenView = findViewById(R.id.callScreenView)
        keypadDrawer = findViewById(R.id.keypadDrawer)
        tvStatus = findViewById(R.id.tvCallStatus)
        answerBtn = findViewById(R.id.answerBtn)
        endCallBtn = findViewById(R.id.endCallBtn)

        setupTts()

        findViewById<View>(R.id.helplineContactRow).setOnClickListener { openCallScreen() }
        answerBtn.setOnClickListener { answerCall() }
        endCallBtn.setOnClickListener { endCall() }
        findViewById<View>(R.id.keypadToggle).setOnClickListener { toggleKeypad() }
        findViewById<View>(R.id.hideKeypadLabel).setOnClickListener { toggleKeypad() }

        val keyMap = mapOf(
            R.id.key1 to "1", R.id.key2 to "2", R.id.key3 to "3",
            R.id.key4 to "4", R.id.key5 to "5", R.id.key6 to "6",
            R.id.key7 to "7", R.id.key8 to "8", R.id.key9 to "9",
            R.id.keyStar to "*", R.id.key0 to "0", R.id.keyHash to "#"
        )
        for ((id, digit) in keyMap) {
            findViewById<View>(id).setOnClickListener { pressKey(digit) }
        }
    }

    private fun setupTts() {
        tts = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts.language = Locale("hi", "IN")
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {}
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {}
                })
            }
        }
    }

    /** Speaks text, and if onDone is given, calls it once speech genuinely finishes (not immediately). */
    private fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!ttsReady) {
            onDone?.invoke()
            return
        }
        if (onDone != null) {
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == UTTERANCE_ID) runOnUiThread { onDone() }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (utteranceId == UTTERANCE_ID) runOnUiThread { onDone() }
                }
            })
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    private fun openCallScreen() {
        contactListView.visibility = View.GONE
        callScreenView.visibility = View.VISIBLE
        tvStatus.text = "Incoming Call…"
        isAnswered = false
        answerBtn.visibility = View.VISIBLE
    }

    private fun answerCall() {
        if (isAnswered) return
        isAnswered = true
        answerBtn.visibility = View.GONE

        speak(
            "eSehat mein aapka swagat hai. Aapki sehat, hamari zimmedari. " +
                    "ASHA worker se baat karne ke liye 1 dabayein. Emergency Ambulance ke liye 2 dabayein."
        )

        callTimerJob = scope.launch {
            while (isActive) {
                delay(1000)
                seconds++
                val mins = seconds / 60
                val secs = seconds % 60
                tvStatus.text = String.format(Locale.US, "%02d:%02d", mins, secs)
            }
        }
    }

    private fun toggleKeypad() {
        keypadDrawer.visibility = if (keypadDrawer.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun pressKey(digit: String) {
        toggleKeypad()
        when (digit) {
            "1" -> {
                tvStatus.text = "Transferring Call…"
                speak("ASHA worker se connect kiya ja raha hai. Kripya pratiksha karein.") {
                    dialNumber(TARGET_PHONE_NUMBER)
                }
            }
            "2" -> {
                speak("Emergency S O S Ambulance ko alert bhej diya gaya hai.")
            }
            else -> {
                speak("Aapne galat vikalp chuna hai.")
            }
        }
    }

    /** Uses ACTION_CALL — dials immediately with no confirmation screen. Requires CALL_PHONE permission. */
    private fun dialNumber(number: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), REQUEST_CALL_PHONE)
            return
        }
        val intent = Intent(Intent.ACTION_CALL).apply { data = Uri.parse("tel:$number") }
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CALL_PHONE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dialNumber(TARGET_PHONE_NUMBER)
            } else {
                Toast.makeText(this, "Call permission is needed to connect you", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun endCall() {
        tts.stop()
        callTimerJob?.cancel()
        tvStatus.text = "Call Ended"
        scope.launch {
            delay(1000)
            resetToContactList()
        }
    }

    private fun resetToContactList() {
        seconds = 0
        isAnswered = false
        keypadDrawer.visibility = View.GONE
        callScreenView.visibility = View.GONE
        contactListView.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        callTimerJob?.cancel()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        scope.cancel()
    }
}