package com.firstapp.esehat

import android.app.NotificationManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvMedicationsLeft: TextView
    private lateinit var tvProgressCount: TextView
    private lateinit var llMedicationList: LinearLayout
    private lateinit var tvEmptyState: TextView
    private lateinit var btnAddMedication: LinearLayout

    private val dotViews = mutableListOf<View>()

    private val TOTAL_DOTS = 8

    private lateinit var medications: MutableList<Medication>

    private var nextId = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        medications = MedicationStorage.load(this)

        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.TIRAMISU
        ) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS
                    ),
                    500
                )
            }
        }

        if (medications.isNotEmpty()) {
            nextId = medications.maxOf { it.id } + 1
        }

        bindViews()
        setupDotRefs()
        setupListeners()

        render()
    }

    override fun onResume() {
        super.onResume()

        if (::medications.isInitialized) {
            medications.clear()
            medications.addAll(MedicationStorage.load(this))

            render()
        }
    }

    private fun bindViews() {

        tvGreeting = findViewById(R.id.tv_greeting)

        tvMedicationsLeft =
            findViewById(R.id.tv_medications_left)

        tvProgressCount =
            findViewById(R.id.tv_progress_count)

        llMedicationList =
            findViewById(R.id.ll_medication_list)

        tvEmptyState =
            findViewById(R.id.tv_empty_state)

        btnAddMedication =
            findViewById(R.id.btn_add_medication)
    }

    private fun setupDotRefs() {

        val ids = listOf(
            R.id.circle1,
            R.id.circle2,
            R.id.circle3,
            R.id.circle4,
            R.id.circle5,
            R.id.circle6,
            R.id.circle7,
            R.id.circle8
        )

        ids.forEach {
            dotViews.add(findViewById(it))
        }
    }

    private fun setupListeners() {

        btnAddMedication.setOnClickListener {
            showAddDialog()
        }

        findViewById<ImageButton>(
            R.id.ib_add_circle
        ).setOnClickListener {
            showAddDialog()
        }

        findViewById<FrameLayout>(
            R.id.btn_consult
        ).setOnClickListener {

            startActivity(
                Intent(this, VideoConsult::class.java)
            )
        }

        findViewById<FrameLayout>(
            R.id.btn_ask_ai
        ).setOnClickListener {

            startActivity(
                Intent(this, BaymaxAI::class.java)
            )
        }

        findViewById<ImageButton>(
            R.id.homebtn
        ).setOnClickListener {}

        findViewById<ImageButton>(
            R.id.healthtrackbtn
        ).setOnClickListener {
            startActivity(
                Intent(this, HealthTracker::class.java)
            )
        }

        findViewById<ImageButton>(
            R.id.videoconsult
        ).setOnClickListener {
            startActivity(
                Intent(this, VideoConsult::class.java)
            )
        }

        findViewById<ImageButton>(
            R.id.baymaxAI
        ).setOnClickListener {
            startActivity(
                Intent(this, BaymaxAI::class.java)
            )
        }

        findViewById<ImageButton>(
            R.id.profileBtn
        ).setOnClickListener {
            startActivity(
                Intent(this, Profilepage::class.java)
            )
        }
    }

    private fun showAddDialog() {

        AddMedicationDialog { name, timing, desc, reminder, hour, minute ->

            val medication = Medication(
                id = nextId++,
                name = name,
                timing = timing,
                description = desc,
                reminderEnabled = reminder,
                reminderHour = hour,
                reminderMinute = minute
            )

            medications.add(medication)

            MedicationStorage.save(
                this,
                medications
            )

            if (reminder) {

                MedicationReminderScheduler.schedule(
                    this,
                    medication
                )
            }

            render()

            Toast.makeText(
                this,
                "$name added",
                Toast.LENGTH_SHORT
            ).show()

        }.show(
            supportFragmentManager,
            "AddMedicationDialog"
        )
    }

    private fun render() {

        updateGreeting()
        updateHeader()
        updateDots()
        buildMedCards()
    }

    private fun updateGreeting() {

        val prefs =
            getSharedPreferences(
                "UserSession",
                MODE_PRIVATE
            )

        val username =
            prefs.getString(
                "username",
                "User"
            )

        val hour =
            Calendar.getInstance()
                .get(Calendar.HOUR_OF_DAY)

        val salutation =
            when {

                hour < 12 ->
                    "Good morning"

                hour < 17 ->
                    "Good afternoon"

                else ->
                    "Good evening"
            }

        tvGreeting.text =
            "$salutation, $username!"
    }

    private fun updateHeader() {

        val taken =
            medications.count {
                it.status == MedStatus.TAKEN
            }

        val pending =
            medications.count {
                it.status == MedStatus.PENDING
            }

        val total =
            medications.size

        tvProgressCount.text =
            "$taken/$total"

        tvMedicationsLeft.text =
            if (pending == 0) {

                "All done for today! Great job 🎉"

            } else {

                "You have $pending medication" +
                        if (pending > 1) "s" else "" +
                                " left for today."
            }
    }

    private fun updateDots() {

        val taken =
            medications.count {
                it.status == MedStatus.TAKEN
            }

        dotViews.forEachIndexed { index, dot ->

            dot.setBackgroundResource(

                if (index < taken) {

                    R.drawable.circle_completed

                } else {

                    R.drawable.circle_incomplete
                }
            )
        }
    }

    private fun buildMedCards() {

        llMedicationList.removeAllViews()

        if (medications.isEmpty()) {

            tvEmptyState.visibility =
                View.VISIBLE

            return
        }

        tvEmptyState.visibility =
            View.GONE

        medications.forEach {

            llMedicationList.addView(
                createMedCard(it)
            )
        }
    }

    private fun createMedCard(
        med: Medication
    ): View {

        val card =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also {
                        it.bottomMargin =
                            dpToPx(12)
                    }

                setBackgroundResource(
                    R.drawable.edittext
                )

                setPadding(
                    dpToPx(16),
                    dpToPx(16),
                    dpToPx(16),
                    dpToPx(16)
                )

                alpha =
                    if (
                        med.status ==
                        MedStatus.SKIPPED
                    ) 0.55f else 1f
            }

        val infoLayout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
            }

        val tvName =
            TextView(this).apply {

                text = med.name

                textSize = 20f

                setTextColor(Color.BLACK)

                if (
                    med.status ==
                    MedStatus.SKIPPED
                ) {

                    paintFlags =
                        paintFlags or
                                android.graphics.Paint
                                    .STRIKE_THRU_TEXT_FLAG
                }
            }

        val tvTime =
            TextView(this).apply {

                text = med.timing

                textSize = 14f

                setTextColor(
                    Color.parseColor("#444444")
                )
            }

        val tvDesc =
            TextView(this).apply {

                text = med.description

                textSize = 12f

                setTextColor(
                    Color.parseColor("#757575")
                )
            }

        val tvReminder =
            TextView(this).apply {

                if (med.reminderEnabled) {

                    text =
                        "🔔 Reminder ${String.format(
                            "%02d:%02d",
                            med.reminderHour,
                            med.reminderMinute
                        )}"

                    setTextColor(
                        Color.parseColor("#2E7D32")
                    )

                    visibility =
                        View.VISIBLE

                } else {

                    visibility =
                        View.GONE
                }

                textSize = 12f

                setPadding(
                    dpToPx(4),
                    dpToPx(5),
                    dpToPx(4),
                    dpToPx(2)
                )
            }

        val tvBadge =
            TextView(this).apply {

                visibility =
                    if (
                        med.status ==
                        MedStatus.PENDING
                    ) View.GONE
                    else View.VISIBLE

                when (med.status) {

                    MedStatus.TAKEN -> {

                        text = "✓ Taken"

                        setBackgroundColor(
                            Color.parseColor("#D1F5E8")
                        )

                        setTextColor(
                            Color.parseColor("#0F6E56")
                        )
                    }

                    MedStatus.SKIPPED -> {

                        text = "Skipped"

                        setBackgroundColor(
                            Color.parseColor("#F1F1F1")
                        )

                        setTextColor(
                            Color.parseColor("#666666")
                        )
                    }

                    else -> {}
                }

                textSize = 11f

                setPadding(
                    dpToPx(8),
                    dpToPx(3),
                    dpToPx(8),
                    dpToPx(3)
                )
            }

        infoLayout.addView(tvName)
        infoLayout.addView(tvTime)
        infoLayout.addView(tvDesc)
        infoLayout.addView(tvReminder)
        infoLayout.addView(tvBadge)

        val actionsLayout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity = Gravity.END

                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also {
                        it.marginStart =
                            dpToPx(12)
                    }
            }

        when (med.status) {

            MedStatus.PENDING -> {

                val btnSkip =
                    makeActionButton(
                        "Skip",
                        "#0C1320"
                    ) {

                        med.status =
                            MedStatus.SKIPPED

                        MedicationStorage.save(
                            this,
                            medications
                        )

                        if (med.reminderEnabled) {
                            MedicationReminderScheduler.cancel(
                                this,
                                med.id
                            )
                        }

                        render()
                    }

                val btnTake =
                    makeActionButton(
                        "Take",
                        "#48B88C"
                    ) {

                        med.status =
                            MedStatus.TAKEN

                        MedicationStorage.save(
                            this,
                            medications
                        )

                        if (med.reminderEnabled) {
                            MedicationReminderScheduler.cancel(
                                this,
                                med.id
                            )
                        }

                        render()
                    }

                actionsLayout.addView(btnSkip)

                actionsLayout.addView(
                    Space(this).apply {
                        minimumHeight =
                            dpToPx(8)
                    }
                )

                actionsLayout.addView(btnTake)
            }

            else -> {

                val btnUndo =
                    makeActionButton(
                        "Undo",
                        "#888888"
                    ) {

                        med.status =
                            MedStatus.PENDING

                        MedicationStorage.save(
                            this,
                            medications
                        )

                        if (med.reminderEnabled) {

                            MedicationReminderScheduler.schedule(
                                this,
                                med
                            )
                        }

                        render()
                    }

                actionsLayout.addView(
                    btnUndo
                )
            }
        }

        card.addView(infoLayout)
        card.addView(actionsLayout)

        return card
    }

    private fun makeActionButton(
        label: String,
        colorHex: String,
        onClick: () -> Unit
    ): Button {

        return Button(this).apply {

            text = label

            textSize = 13f

            isAllCaps = false

            setTextColor(Color.WHITE)

            background =
                android.graphics.drawable
                    .GradientDrawable()
                    .apply {

                        setColor(
                            Color.parseColor(
                                colorHex
                            )
                        )

                        cornerRadius =
                            dpToPx(24).toFloat()
                    }

            layoutParams =
                LinearLayout.LayoutParams(
                    dpToPx(100),
                    dpToPx(38)
                )

            setOnClickListener {
                onClick()
            }
        }
    }

    private fun dpToPx(dp: Int): Int =
        (
                dp *
                        resources.displayMetrics.density
                ).toInt()
}