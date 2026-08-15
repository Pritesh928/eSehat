package com.firstapp.esehat

import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.firstapp.esehat.databinding.ActivityBaymaxAiBinding
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class BaymaxAI : AppCompatActivity() {

    private lateinit var binding: ActivityBaymaxAiBinding

    private val scope = MainScope()

    private var started = false
    private var facts = "{}"

    private val BASE_URL = "https://baymaxai.onrender.com"

    private var networkAvailable = false

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    private val client: OkHttpClient by lazy {

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }

        OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(7, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    private lateinit var dbHelper: ChatHistoryDbHelper
    private lateinit var sessionPrefs: SharedPreferences

    private var currentConversationId: Long = -1

    private var speechOutputEnabled = false
    private var conversationModeActive = false
    private var mediaPlayer: MediaPlayer? = null

    private lateinit var input: EditText
    private lateinit var messagesContainer: LinearLayout
    private lateinit var chatScrollView: ScrollView
    private lateinit var micButton: ImageButton
    private lateinit var speakerToggle: ImageButton
    private lateinit var historyButton: ImageButton

    private var typingJob: Job? = null

    companion object {
        private const val REQUEST_RECORD_AUDIO = 1001
    }

    private val speechLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == RESULT_OK) {

                val matches =
                    result.data?.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS
                    )

                val text = matches?.firstOrNull()?.trim()

                if (!text.isNullOrEmpty()) {

                    setListeningVisual(false)

                    sendMessage(
                        text,
                        spokenReply = true
                    )

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

        binding =
            ActivityBaymaxAiBinding.inflate(layoutInflater)

        setContentView(binding.root)

        dbHelper = ChatHistoryDbHelper(this)

        sessionPrefs =
            getSharedPreferences(
                "BaymaxSession",
                MODE_PRIVATE
            )

        input = findViewById(R.id.chatInput)
        messagesContainer = findViewById(R.id.messagesContainer)
        chatScrollView = findViewById(R.id.chatScrollView)
        micButton = findViewById(R.id.micButton)
        speakerToggle = findViewById(R.id.speakerToggle)
        historyButton = findViewById(R.id.historyButton)

        val sendButton =
            findViewById<ImageButton>(R.id.sendButton)

        currentConversationId =
            resolveConversationId()

        loadConversationIntoUI(
            currentConversationId
        )

        setupNetworkMonitoring()

        sendButton.setOnClickListener {

            val message =
                input.text.toString().trim()

            if (message.isEmpty()) return@setOnClickListener

            input.text.clear()

            sendMessage(message)
        }

        micButton.setOnClickListener {
            onMicTapped()
        }

        historyButton.setOnClickListener {
            showRecentChatsDialog()
        }

        speakerToggle.setOnClickListener {

            speechOutputEnabled =
                !speechOutputEnabled

            speakerToggle.setImageResource(
                if (speechOutputEnabled)
                    android.R.drawable.ic_lock_silent_mode
                else
                    android.R.drawable.ic_lock_silent_mode_off
            )

            if (!speechOutputEnabled) {
                mediaPlayer?.let {
                    if (it.isPlaying) it.stop()
                }
            }

            Toast.makeText(
                this,
                if (speechOutputEnabled)
                    "Baymax will speak replies aloud"
                else
                    "Spoken replies off",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<ImageButton>(R.id.homebtn)
            .setOnClickListener {
                startActivity(
                    Intent(this, MainActivity::class.java)
                )
            }

        findViewById<ImageButton>(R.id.healthtrackbtn)
            .setOnClickListener {
                startActivity(
                    Intent(this, HealthTracker::class.java)
                )
            }

        findViewById<ImageButton>(R.id.videoconsult)
            .setOnClickListener {
                startActivity(
                    Intent(this, VideoConsult::class.java)
                )
            }

        findViewById<ImageButton>(R.id.baymaxAI)
            .setOnClickListener {
                startActivity(
                    Intent(this, BaymaxAI::class.java)
                )
            }

        findViewById<ImageButton>(R.id.profileBtn)
            .setOnClickListener {
                startActivity(
                    Intent(this, Profilepage::class.java)
                )
            }
    }

    // ---------------- Chat bubbles ----------------

    private fun addUserBubble(text: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dpToPx(10) }
        }

        val bubble = TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 14f
            maxWidth = dpToPx(260)
            setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E1E1E"))
                cornerRadius = dpToPx(16).toFloat()
            }
        }

        row.addView(bubble)
        messagesContainer.addView(row)
        scrollToBottom()
    }

    private fun addBotBubble(text: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dpToPx(10) }
        }

        val bubble = TextView(this).apply {
            this.text = text
            setTextColor(Color.BLACK)
            textSize = 14f
            maxWidth = dpToPx(260)
            setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F0F0F0"))
                cornerRadius = dpToPx(16).toFloat()
            }
        }

        row.addView(bubble)
        messagesContainer.addView(row)
        scrollToBottom()
    }

    /** Adds an animated "Baymax is typing…" bubble and returns its row so it can be removed later. */
    private fun addTypingBubble(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dpToPx(10) }
        }

        val bubble = TextView(this).apply {
            text = "Baymax is typing"
            setTextColor(Color.parseColor("#777777"))
            textSize = 14f
            setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F0F0F0"))
                cornerRadius = dpToPx(16).toFloat()
            }
        }

        row.addView(bubble)
        messagesContainer.addView(row)
        scrollToBottom()

        typingJob = scope.launch {
            var dots = 0
            while (isActive) {
                bubble.text = "Baymax is typing" + ".".repeat(dots % 4)
                dots++
                delay(400)
            }
        }

        return row
    }

    private fun removeTypingBubble(row: View) {
        typingJob?.cancel()
        typingJob = null
        messagesContainer.removeView(row)
    }

    private fun scrollToBottom() {
        chatScrollView.post {
            chatScrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    // ---------------- Network monitoring (unchanged) ----------------

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupNetworkMonitoring() {

        connectivityManager =
            getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager

        networkAvailable =
            isInternetAvailable()

        networkCallback =
            object : ConnectivityManager.NetworkCallback() {

                override fun onAvailable(
                    network: Network
                ) {
                    runOnUiThread {
                        networkAvailable = true
                        showOnlineState()
                    }
                }

                override fun onLost(
                    network: Network
                ) {
                    runOnUiThread {

                        networkAvailable =
                            isInternetAvailable()

                        if (!networkAvailable) {
                            showOfflineState()
                        }
                    }
                }
            }

        connectivityManager.registerDefaultNetworkCallback(
            networkCallback
        )
    }

    private fun isInternetAvailable(): Boolean {

        return try {

            val network =
                connectivityManager.activeNetwork
                    ?: return false

            val capabilities =
                connectivityManager
                    .getNetworkCapabilities(network)
                    ?: return false

            capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            ) &&
                    capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED
                    )

        } catch (_: Exception) {
            false
        }
    }

    private fun showOfflineState() {

        Toast.makeText(
            this,
            "Offline mode — basic Baymax is available",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showOnlineState() {

        Toast.makeText(
            this,
            "Internet restored — Baymax AI is online",
            Toast.LENGTH_SHORT
        ).show()
    }

    // ---------------- Local storage: conversation load/switch/create ----------------

    private fun resolveConversationId(): Long {

        val requestedId =
            intent.getLongExtra(
                "conversation_id",
                -1L
            )

        if (requestedId != -1L) {

            sessionPrefs.edit()
                .putLong(
                    "last_conversation_id",
                    requestedId
                )
                .apply()

            return requestedId
        }

        val lastId =
            sessionPrefs.getLong(
                "last_conversation_id",
                -1L
            )

        if (lastId != -1L)
            return lastId

        val newId =
            dbHelper.createConversation()

        sessionPrefs.edit()
            .putLong(
                "last_conversation_id",
                newId
            )
            .apply()

        return newId
    }

    private fun loadConversationIntoUI(
        conversationId: Long
    ) {

        messagesContainer.removeAllViews()

        val state =
            dbHelper.getConversationState(
                conversationId
            )

        facts = state.facts
        started = state.started

        for (m in dbHelper.getMessages(conversationId)) {
            if (m.sender == "user") addUserBubble(m.text) else addBotBubble(m.text)
        }
    }

    private fun startNewConversation() {

        val id =
            dbHelper.createConversation()

        currentConversationId = id

        sessionPrefs.edit()
            .putLong(
                "last_conversation_id",
                id
            )
            .apply()

        facts = "{}"
        started = false

        messagesContainer.removeAllViews()
    }

    private fun openConversation(id: Long) {

        if (id == currentConversationId)
            return

        currentConversationId = id

        sessionPrefs.edit()
            .putLong(
                "last_conversation_id",
                id
            )
            .apply()

        loadConversationIntoUI(id)
    }

    /** Styled "Recent Chats" dialog — a scrollable list of card rows instead of a plain text list. */
    private fun showRecentChatsDialog() {

        val recents = dbHelper.getRecentConversations()
        val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(4))
        }

        lateinit var dialog: AlertDialog

        fun addRow(icon: String, title: String, subtitle: String?, accentColor: String, onClick: () -> Unit) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#F7F7F7"))
                    cornerRadius = dpToPx(14).toFloat()
                }
                setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dpToPx(8) }
            }

            val iconBadge = TextView(this).apply {
                text = icon
                textSize = 18f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40)).also {
                    it.marginEnd = dpToPx(12)
                }
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(accentColor))
                    cornerRadius = dpToPx(12).toFloat()
                }
            }

            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            textCol.addView(TextView(this).apply {
                text = title
                textSize = 14f
                setTextColor(Color.parseColor("#111111"))
                setTypeface(typeface, Typeface.BOLD)
                maxLines = 1
            })

            if (subtitle != null) {
                textCol.addView(TextView(this).apply {
                    text = subtitle
                    textSize = 11f
                    setTextColor(Color.parseColor("#888888"))
                })
            }

            row.addView(iconBadge)
            row.addView(textCol)
            row.setOnClickListener {
                onClick()
                dialog.dismiss()
            }
            container.addView(row)
        }

        addRow("➕", "New chat", null, "#DFF5E1") { startNewConversation() }

        for (c in recents) {
            addRow("💬", c.title, fmt.format(Date(c.updatedAt)), "#E3F2FD") { openConversation(c.id) }
        }

        val scrollWrapper = ScrollView(this).apply { addView(container) }

        dialog = AlertDialog.Builder(this)
            .setTitle("Recent Chats")
            .setView(scrollWrapper)
            .setNegativeButton("Close", null)
            .create()

        dialog.show()
    }

    private fun maybeSetTitleFromFirstMessage(
        conversationId: Long,
        message: String
    ) {

        if (
            dbHelper.getMessageCount(
                conversationId
            ) == 0
        ) {

            val title =
                if (message.length > 40)
                    message.take(40) + "…"
                else
                    message

            dbHelper.renameConversation(
                conversationId,
                title
            )
        }
    }

    // ---------------- Sending messages ----------------

    private fun sendMessage(
        userMessage: String,
        spokenReply: Boolean = false
    ) {

        addUserBubble(userMessage)

        maybeSetTitleFromFirstMessage(
            currentConversationId,
            userMessage
        )

        dbHelper.appendMessage(
            currentConversationId,
            "user",
            userMessage
        )

        val typingRow = addTypingBubble()

        scope.launch {

            val reply =
                if (networkAvailable) {

                    val serverReply =
                        fetchReply(
                            userMessage
                        )

                    if (serverReply != null) {
                        serverReply
                    } else {
                        offlineSymptomReply(
                            userMessage
                        )
                    }

                } else {

                    offlineSymptomReply(
                        userMessage
                    )
                }

            removeTypingBubble(typingRow)
            addBotBubble(reply)

            dbHelper.appendMessage(
                currentConversationId,
                "baymax",
                reply
            )

            dbHelper.updateState(
                currentConversationId,
                facts,
                started
            )

            if (
                spokenReply ||
                speechOutputEnabled
            ) {

                speak(
                    reply,
                    continueConversation =
                        spokenReply &&
                                conversationModeActive
                )

            } else if (
                conversationModeActive
            ) {

                startListeningRound()
            }
        }
    }

    private suspend fun fetchReply(
        message: String
    ): String? {

        return withContext(
            Dispatchers.IO
        ) {

            try {

                val encoded =
                    URLEncoder.encode(
                        message,
                        "UTF-8"
                    )

                val url =
                    if (!started) {

                        "$BASE_URL/chats/create?message=$encoded"

                    } else {

                        "$BASE_URL/chats/follow?message=$encoded&facts=$facts"
                    }

                val request =
                    Request.Builder()
                        .addHeader(
                            "Authorization",
                            "Bearer 12345"
                        )
                        .url(url)
                        .build()

                client.newCall(
                    request
                ).execute().use { response ->

                    if (!response.isSuccessful) {
                        return@use null
                    }

                    val body =
                        response.body?.string()
                            ?: return@use null

                    try {

                        val json =
                            JSONObject(body)

                        val extracted =
                            json.optString(
                                "t",
                                ""
                            )

                        val decision =
                            json.optString(
                                "a",
                                ""
                            )

                        if (
                            json.has("u")
                        ) {
                            facts =
                                json.get("u")
                                    .toString()
                        }

                        if (
                            extracted.isNotEmpty()
                        ) {

                            started = true

                            if (
                                decision == "assess"
                            ) {
                                started = false
                            }

                            extracted

                        } else {

                            body
                        }

                    } catch (_: Exception) {

                        body
                    }
                }

            } catch (_: Exception) {

                null
            }
        }
    }

    private fun offlineSymptomReply(
        message: String
    ): String {

        val language =
            Locale.getDefault()
                .language
                .lowercase()

        val text =
            message.lowercase(
                Locale.getDefault()
            )

        val hasFever =
            text.contains("fever") ||
                    text.contains("temperature") ||
                    text.contains("bukhar") ||
                    text.contains("ताप")

        val hasCold =
            text.contains("cold") ||
                    text.contains("cough") ||
                    text.contains("flu") ||
                    text.contains("sardi") ||
                    text.contains("खांसी")

        val hasPain =
            text.contains("pain") ||
                    text.contains("ache") ||
                    text.contains("दर्द") ||
                    text.contains("vedana")

        return when (language) {

            "hi" -> {

                when {
                    hasFever || hasCold -> """
मैं समझता हूँ कि आपको बुखार या सर्दी-जुकाम जैसे लक्षण हैं।

अभी आराम करें, पर्याप्त पानी पिएं और अपने तापमान पर नजर रखें।

क्या आप अपने लक्षणों के बारे में थोड़ा और विस्तार से बता सकते हैं — जैसे बुखार कितने समय से है, खांसी है या नहीं और कोई दर्द या सांस लेने में परेशानी है?

यह एक सामान्य प्रारंभिक सलाह है, डॉक्टर का निदान नहीं।

अगर लक्षण ज्यादा गंभीर हों या लगातार बने रहें तो डॉक्टर से सलाह लें।

आप eSehat में डॉक्टर से Consult कर सकते हैं या इंटरनेट उपलब्ध होने पर हमारे Live AI Health Assistant Baymax से बात कर सकते हैं।
                """.trimIndent()

                    hasPain -> """
मुझे समझ आ रहा है कि आपको दर्द की समस्या है।

कृपया बताएं दर्द कहां है, कब से है और कितना तेज है।

अभी आराम करें और पर्याप्त पानी पिएं।

अगर दर्द बहुत तेज है, अचानक शुरू हुआ है या लगातार बढ़ रहा है तो डॉक्टर से सलाह लें।

आप eSehat में डॉक्टर से Consult कर सकते हैं या इंटरनेट उपलब्ध होने पर Baymax Live AI Health Assistant से बात कर सकते हैं।
                """.trimIndent()

                    else -> """
मैं आपकी मदद करने की कोशिश कर सकता हूँ।

कृपया अपने लक्षणों के बारे में थोड़ा विस्तार से बताएं — समस्या कब शुरू हुई, आपको क्या महसूस हो रहा है और लक्षण कितने गंभीर हैं।

यह केवल सामान्य स्वास्थ्य जानकारी है और डॉक्टर का निदान नहीं है।

लक्षण गंभीर या लगातार बने रहें तो डॉक्टर से सलाह लें।

आप eSehat में डॉक्टर से Consult कर सकते हैं या इंटरनेट उपलब्ध होने पर Baymax Live AI Health Assistant से बात कर सकते हैं।
                """.trimIndent()
                }
            }

            "mr" -> {

                when {
                    hasFever || hasCold -> """
तुम्हाला ताप किंवा सर्दी-खोकल्यासारखी लक्षणे आहेत असे मला समजते.

सध्या विश्रांती घ्या, पुरेसे पाणी प्या आणि तापमानावर लक्ष ठेवा.

तुमची लक्षणे थोडी सविस्तर सांगू शकता का? ताप किती दिवसांपासून आहे, खोकला आहे का आणि श्वास घेण्यास त्रास आहे का?

ही फक्त प्राथमिक सामान्य आरोग्य माहिती आहे, डॉक्टरांचे निदान नाही.

लक्षणे गंभीर असतील किंवा जास्त काळ राहिल्यास डॉक्टरांचा सल्ला घ्या.

तुम्ही eSehat मधून डॉक्टरांशी Consult करू शकता किंवा इंटरनेट उपलब्ध झाल्यावर Baymax Live AI Health Assistant शी बोलू शकता.
                """.trimIndent()

                    else -> """
मी तुम्हाला प्राथमिक मदत करण्याचा प्रयत्न करू शकतो.

तुमची लक्षणे सविस्तर सांगा — समस्या कधी सुरू झाली, काय त्रास होत आहे आणि तो किती गंभीर आहे.

लक्षणे गंभीर किंवा सतत राहिल्यास डॉक्टरांचा सल्ला घ्या.

तुम्ही eSehat मधून डॉक्टरांशी Consult करू शकता किंवा इंटरनेट उपलब्ध झाल्यावर Baymax Live AI Health Assistant शी बोलू शकता.
                """.trimIndent()
                }
            }

            "pa" -> {

                when {
                    hasFever || hasCold -> """
ਮੈਨੂੰ ਸਮਝ ਆ ਰਿਹਾ ਹੈ ਕਿ ਤੁਹਾਨੂੰ ਬੁਖਾਰ ਜਾਂ ਜ਼ੁਕਾਮ ਵਰਗੇ ਲੱਛਣ ਹਨ।

ਫਿਲਹਾਲ ਆਰਾਮ ਕਰੋ, ਕਾਫ਼ੀ ਪਾਣੀ ਪੀਓ ਅਤੇ ਆਪਣੇ ਤਾਪਮਾਨ 'ਤੇ ਨਜ਼ਰ ਰੱਖੋ।

ਕੀ ਤੁਸੀਂ ਆਪਣੇ ਲੱਛਣਾਂ ਬਾਰੇ ਥੋੜ੍ਹਾ ਹੋਰ ਵਿਸਥਾਰ ਨਾਲ ਦੱਸ ਸਕਦੇ ਹੋ — ਬੁਖਾਰ ਕਿੰਨੇ ਸਮੇਂ ਤੋਂ ਹੈ, ਖੰਘ ਹੈ ਜਾਂ ਸਾਹ ਲੈਣ ਵਿੱਚ ਕੋਈ ਤਕਲੀਫ਼ ਹੈ?

ਇਹ ਸਿਰਫ਼ ਆਮ ਸਿਹਤ ਜਾਣਕਾਰੀ ਹੈ, ਡਾਕਟਰੀ ਨਿਦਾਨ ਨਹੀਂ।

ਜੇ ਲੱਛਣ ਗੰਭੀਰ ਹਨ ਜਾਂ ਲਗਾਤਾਰ ਰਹਿੰਦੇ ਹਨ ਤਾਂ ਡਾਕਟਰ ਨਾਲ ਸਲਾਹ ਕਰੋ।

ਤੁਸੀਂ eSehat ਵਿੱਚ ਡਾਕਟਰ ਨਾਲ Consult ਕਰ ਸਕਦੇ ਹੋ ਜਾਂ ਇੰਟਰਨੈੱਟ ਉਪਲਬਧ ਹੋਣ 'ਤੇ Baymax Live AI Health Assistant ਨਾਲ ਗੱਲ ਕਰ ਸਕਦੇ ਹੋ।
                """.trimIndent()

                    else -> """
ਮੈਂ ਤੁਹਾਡੀ ਮੁੱਢਲੀ ਮਦਦ ਕਰਨ ਦੀ ਕੋਸ਼ਿਸ਼ ਕਰ ਸਕਦਾ ਹਾਂ।

ਕਿਰਪਾ ਕਰਕੇ ਆਪਣੇ ਲੱਛਣਾਂ ਬਾਰੇ ਵਿਸਥਾਰ ਨਾਲ ਦੱਸੋ — ਸਮੱਸਿਆ ਕਦੋਂ ਸ਼ੁਰੂ ਹੋਈ ਅਤੇ ਤੁਹਾਨੂੰ ਕੀ ਮਹਿਸੂਸ ਹੋ ਰਿਹਾ ਹੈ।

ਜੇ ਲੱਛਣ ਗੰਭੀਰ ਜਾਂ ਲਗਾਤਾਰ ਰਹਿੰਦੇ ਹਨ ਤਾਂ ਡਾਕਟਰ ਨਾਲ ਸਲਾਹ ਕਰੋ।

ਤੁਸੀਂ eSehat ਵਿੱਚ ਡਾਕਟਰ ਨਾਲ Consult ਕਰ ਸਕਦੇ ਹੋ ਜਾਂ ਇੰਟਰਨੈੱਟ ਉਪਲਬਧ ਹੋਣ 'ਤੇ Baymax Live AI Health Assistant ਨਾਲ ਗੱਲ ਕਰ ਸਕਦੇ ਹੋ।
                """.trimIndent()
                }
            }

            else -> {

                when {
                    hasFever || hasCold -> """
I understand that you may have symptoms such as fever, cold or cough.

For now, get adequate rest, drink enough fluids and keep an eye on your temperature.

Can you explain your symptoms in more detail? For example, how long you have had the fever or cold, whether you have a cough, pain, breathing difficulty, or any other symptoms?

This is only general preliminary health information and is not a medical diagnosis.

If your symptoms are severe, worsening, or persistent, please consult a doctor.

You can consult a doctor through eSehat or talk to our Live AI Health Assistant Baymax when internet access is available.
                """.trimIndent()

                    hasPain -> """
I understand that you are experiencing some pain.

Can you explain where the pain is, how long you have had it, and how severe it is?

Please rest and stay adequately hydrated.

If the pain is severe, sudden, worsening, or persistent, please consult a doctor.

You can consult a doctor through eSehat or talk to our Live AI Health Assistant Baymax when internet access is available.
                """.trimIndent()

                    else -> """
I can provide some basic guidance while you are offline.

Please explain your symptoms in more detail — when the problem started, what you are feeling, and how severe it is.

This is general health information and not a medical diagnosis.

If your symptoms are severe, worsening, or persistent, please consult a doctor.

You can consult a doctor through eSehat or talk to our Live AI Health Assistant Baymax when internet access is available.
                """.trimIndent()
                }
            }
        }
    }

    private fun onMicTapped() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.RECORD_AUDIO
                ),
                REQUEST_RECORD_AUDIO
            )

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

        if (
            !SpeechRecognizer.isRecognitionAvailable(
                this
            )
        ) {

            Toast.makeText(
                this,
                "Voice input isn't available on this device.",
                Toast.LENGTH_LONG
            ).show()

            stopConversationMode()

            return
        }

        setListeningVisual(true)

        val intent =
            Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )

                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Describe your symptom…"
                )
            }

        speechLauncher.launch(intent)
    }

    private fun onRecognitionFailed() {

        setListeningVisual(false)

        val wasInConversation =
            conversationModeActive

        stopConversationMode()

        Toast.makeText(
            this,
            if (wasInConversation)
                "Didn't catch that — tap the mic to talk again"
            else
                "Didn't catch that — try again",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun stopConversationMode() {

        conversationModeActive = false

        setListeningVisual(false)

        mediaPlayer?.let {
            if (it.isPlaying)
                it.stop()
        }
    }

    private fun setListeningVisual(
        listening: Boolean
    ) {

        micButton.setImageResource(
            if (listening)
                android.R.drawable.ic_media_pause
            else
                android.R.drawable.ic_btn_speak_now
        )
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

        if (
            requestCode ==
            REQUEST_RECORD_AUDIO
        ) {

            if (
                grantResults.isNotEmpty() &&
                grantResults[0] ==
                PackageManager.PERMISSION_GRANTED
            ) {

                onMicTapped()

            } else {

                Toast.makeText(
                    this,
                    "Microphone permission is needed for voice input",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun speak(
        text: String,
        continueConversation: Boolean = false
    ) {

        scope.launch {

            try {

                val audioFile =
                    withContext(
                        Dispatchers.IO
                    ) {
                        fetchSpeechAudio(text)
                    }

                playAudio(
                    audioFile,
                    continueConversation
                )

            } catch (_: Exception) {

                Toast.makeText(
                    this@BaymaxAI,
                    "Voice reply unavailable",
                    Toast.LENGTH_SHORT
                ).show()

                if (continueConversation)
                    startListeningRound()
            }
        }
    }

    private suspend fun fetchSpeechAudio(
        text: String
    ): File =
        withContext(
            Dispatchers.IO
        ) {

            val encoded =
                URLEncoder.encode(
                    text,
                    "UTF-8"
                )

            val request =
                Request.Builder()
                    .addHeader(
                        "Authorization",
                        "Bearer 12345"
                    )
                    .url(
                        "$BASE_URL/tts?message=$encoded"
                    )
                    .build()

            client.newCall(
                request
            ).execute().use { response ->

                if (!response.isSuccessful)
                    throw Exception(
                        "TTS failed"
                    )

                val bytes =
                    response.body?.bytes()
                        ?: throw Exception(
                            "Empty TTS response"
                        )

                val file =
                    File(
                        cacheDir,
                        "baymax_reply_${System.currentTimeMillis()}.wav"
                    )

                FileOutputStream(file)
                    .use {
                        it.write(bytes)
                    }

                file
            }
        }

    private fun playAudio(
        file: File,
        continueConversation: Boolean
    ) {

        mediaPlayer?.release()

        mediaPlayer =
            MediaPlayer().apply {

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

                    if (
                        continueConversation
                    ) {
                        startListeningRound()
                    }
                }

                setOnErrorListener { mp, _, _ ->

                    mp.release()

                    mediaPlayer = null

                    file.delete()

                    if (
                        continueConversation
                    ) {
                        startListeningRound()
                    }

                    true
                }

                prepareAsync()
            }
    }

    override fun onPause() {

        super.onPause()

        mediaPlayer?.let {
            if (it.isPlaying)
                it.stop()
        }
    }

    override fun onDestroy() {

        super.onDestroy()

        try {
            connectivityManager
                .unregisterNetworkCallback(
                    networkCallback
                )
        } catch (_: Exception) {
        }

        conversationModeActive = false

        mediaPlayer?.release()

        mediaPlayer = null

        typingJob?.cancel()

        scope.cancel()
    }
}