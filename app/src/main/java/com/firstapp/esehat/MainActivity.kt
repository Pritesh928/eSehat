package com.firstapp.esehat

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Calendar

class MainActivity : AppCompatActivity() {


    private lateinit var tvGreeting: TextView
    private lateinit var tvMedicationsLeft: TextView
    private lateinit var tvProgressCount: TextView
    private lateinit var llMedicationList: LinearLayout
    private lateinit var tvEmptyState: TextView
    private lateinit var btnAddMedication: LinearLayout

    private val dotViews = mutableListOf<View>()
    private val TOTAL_DOTS = 8



    private val medications = mutableListOf(
        Medication(1, "Paracetamol", "After Dinner", "Treats illness and normal fever"),
    )
    private var nextId = 3


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        setupDotRefs()
        setupListeners()
        render()
    }


    private fun bindViews() {
        tvGreeting         = findViewById(R.id.tv_greeting)
        tvMedicationsLeft  = findViewById(R.id.tv_medications_left)
        tvProgressCount    = findViewById(R.id.tv_progress_count)
        llMedicationList   = findViewById(R.id.ll_medication_list)
        tvEmptyState       = findViewById(R.id.tv_empty_state)
        btnAddMedication   = findViewById(R.id.btn_add_medication)
    }

    private fun setupDotRefs() {
        val ids = listOf(
            R.id.circle1, R.id.circle2, R.id.circle3, R.id.circle4,
            R.id.circle5, R.id.circle6, R.id.circle7, R.id.circle8
        )
        ids.forEach { dotViews.add(findViewById(it)) }
    }


    private fun setupListeners() {

        btnAddMedication.setOnClickListener { showAddDialog() }
        findViewById<ImageButton>(R.id.ib_add_circle).setOnClickListener { showAddDialog() }


        findViewById<FrameLayout>(R.id.btn_consult).setOnClickListener {
            startActivity(Intent(this, VideoConsult::class.java))
            Toast.makeText(this, "Opening video consult…", Toast.LENGTH_SHORT).show()
        }
        findViewById<FrameLayout>(R.id.btn_ask_ai).setOnClickListener {
            startActivity(Intent(this, BaymaxAI::class.java))
            Toast.makeText(this, "Opening AI assistant…", Toast.LENGTH_SHORT).show()
        }


        findViewById<ImageButton>(R.id.homebtn).setOnClickListener {
        }
        findViewById<ImageButton>(R.id.healthtrackbtn).setOnClickListener {
            Toast.makeText(this, "Health Tracking", Toast.LENGTH_SHORT).show()
             startActivity(Intent(this, HealthTracker::class.java))
        }
        findViewById<ImageButton>(R.id.videoconsult).setOnClickListener {
            Toast.makeText(this, "Video Consult", Toast.LENGTH_SHORT).show()
             startActivity(Intent(this, VideoConsult::class.java))
        }
        findViewById<ImageButton>(R.id.baymaxAI).setOnClickListener {
            Toast.makeText(this, "AI Assistant", Toast.LENGTH_SHORT).show()
             startActivity(Intent(this, BaymaxAI::class.java))
        }
        findViewById<ImageButton>(R.id.profileBtn).setOnClickListener {
            Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show()
             startActivity(Intent(this, Profilepage::class.java))
        }
    }


    private fun showAddDialog() {
        AddMedicationDialog { name, timing, desc ->
            medications.add(Medication(nextId++, name, timing, desc))
            render()
            Toast.makeText(this, "$name added!", Toast.LENGTH_SHORT).show()
        }.show(supportFragmentManager, "AddMedicationDialog")
    }


    private fun render() {
        updateGreeting()
        updateHeader()
        updateDots()
        buildMedCards()
    }

    private fun updateGreeting() {
        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val username = prefs.getString("username", "User")

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val salutation = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else      -> "Good evening"
        }
        tvGreeting.text = "$salutation, $username!"
    }

    private fun updateHeader() {
        val taken   = medications.count { it.status == MedStatus.TAKEN }
        val pending = medications.count { it.status == MedStatus.PENDING }
        val total   = medications.size

        tvProgressCount.text = "$taken/$total"

        tvMedicationsLeft.text = if (pending == 0) {
            "All done for today! Great job 🎉"
        } else {
            "You have $pending medication${if (pending > 1) "s" else ""} left for today."
        }
    }

    private fun updateDots() {
        val taken = medications.count { it.status == MedStatus.TAKEN }
        dotViews.forEachIndexed { index, dot ->
            dot.setBackgroundResource(
                if (index < taken) R.drawable.circle_completed
                else               R.drawable.circle_incomplete
            )
        }
    }


    private fun buildMedCards() {
        llMedicationList.removeAllViews()

        if (medications.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            return
        }
        tvEmptyState.visibility = View.GONE

        medications.forEach { med -> llMedicationList.addView(createMedCard(med)) }
    }

    private fun createMedCard(med: Medication): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dpToPx(12) }
            setBackgroundResource(R.drawable.edittext)
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            alpha = if (med.status == MedStatus.SKIPPED) 0.55f else 1f
        }

        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, // width = 0, weight fills the rest
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val tvName = TextView(this).apply {
            text = med.name
            textSize = 20f
            setTextColor(Color.BLACK)
            if (med.status == MedStatus.SKIPPED) paintFlags =
                paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
        }
        val tvTime = TextView(this).apply {
            text = med.timing
            textSize = 14f
            setTextColor(Color.BLACK)
            (layoutParams as? LinearLayout.LayoutParams)?.topMargin = dpToPx(2)
        }
        val tvDesc = TextView(this).apply {
            text = med.description
            textSize = 12f
            setTextColor(Color.parseColor("#757575"))
            (layoutParams as? LinearLayout.LayoutParams)?.topMargin = dpToPx(2)
        }

        val tvBadge = TextView(this).apply {
            visibility = if (med.status == MedStatus.PENDING) View.GONE else View.VISIBLE
            when (med.status) {
                MedStatus.TAKEN -> {
                    text = "✓ Taken"
                    setBackgroundColor(Color.parseColor("#D1F5E8"))
                    setTextColor(Color.parseColor("#0F6E56"))
                }
                MedStatus.SKIPPED -> {
                    text = "Skipped"
                    setBackgroundColor(Color.parseColor("#F1F1F1"))
                    setTextColor(Color.parseColor("#666666"))
                }
                else -> {}
            }
            textSize = 11f
            setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = dpToPx(6) }
        }

        infoLayout.addView(tvName)
        infoLayout.addView(tvTime)
        infoLayout.addView(tvDesc)
        infoLayout.addView(tvBadge)

        val actionsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.marginStart = dpToPx(12) }
        }

        when (med.status) {
            MedStatus.PENDING -> {
                val btnSkip = makeActionButton("Skip", "#0C1320") {
                    med.status = MedStatus.SKIPPED
                    toast("${med.name} skipped")
                    render()
                }
                val btnTake = makeActionButton("Take", "#48B88C") {
                    med.status = MedStatus.TAKEN
                    toast("${med.name} marked as taken ✓")
                    render()
                }
                actionsLayout.addView(btnSkip)
                actionsLayout.addView(Space(this).apply { minimumHeight = dpToPx(8) })
                actionsLayout.addView(btnTake)
            }
            else -> {
                val btnUndo = makeActionButton("Undo", "#888888") {
                    med.status = MedStatus.PENDING
                    toast("${med.name} reset")
                    render()
                }
                actionsLayout.addView(btnUndo)
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
    ): Button = Button(this).apply {
        text = label
        textSize = 13f
        isAllCaps = false
        setTextColor(Color.WHITE)


        val drawable = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor(colorHex))
            cornerRadius = dpToPx(24).toFloat()
        }
        background = drawable

        layoutParams = LinearLayout.LayoutParams(dpToPx(100), dpToPx(38)).also {
            it.bottomMargin = dpToPx(4)
        }
        setOnClickListener { onClick() }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}