package com.firstapp.esehat

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

enum class AppointmentStatus { UPCOMING, COMPLETED }

data class Appointment(
    val id: Int,
    val patientName: String,
    val time: String,
    val reason: String,
    var status: AppointmentStatus = AppointmentStatus.UPCOMING
)

class DoctorDashboard : AppCompatActivity() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvAppointmentsLeft: TextView
    private lateinit var llAppointmentList: LinearLayout
    private lateinit var tvEmptyState: TextView

    // Dummy data — replace with real appointments once you have a backend/DB for this.
    private val appointments = mutableListOf(
        Appointment(1, "Ravi Kumar", "10:30 AM", "Fever & body ache"),
        Appointment(2, "Sita Devi", "11:15 AM", "Follow-up: Diabetes"),
        Appointment(3, "Anil Verma", "12:00 PM", "Skin rash consultation")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_dashboard)

        bindViews()
        setupQuickActions()
        setupBottomNav()
        render()
    }

    private fun bindViews() {
        tvGreeting = findViewById(R.id.tv_greeting)
        tvAppointmentsLeft = findViewById(R.id.tv_appointments_left)
        llAppointmentList = findViewById(R.id.ll_appointment_list)
        tvEmptyState = findViewById(R.id.tv_empty_state)
    }

    private fun setupQuickActions() {
        // Existing screen — reused as-is.
        findViewById<androidx.cardview.widget.CardView>(R.id.action_start_consult).setOnClickListener {
            startActivity(Intent(this, VideoConsult::class.java))
        }

        // NOTE: these three activities don't exist yet in your project — create them
        // (even as empty placeholder Activities) or these clicks will crash until you do.
        findViewById<androidx.cardview.widget.CardView>(R.id.action_write_prescription).setOnClickListener {
            startActivity(Intent(this, PrescriptionActivity::class.java))
        }
        findViewById<androidx.cardview.widget.CardView>(R.id.action_patient_records).setOnClickListener {
            startActivity(Intent(this, PatientRecordsActivity::class.java))
        }
        findViewById<androidx.cardview.widget.CardView>(R.id.action_follow_ups).setOnClickListener {
            startActivity(Intent(this, FollowUpActivity::class.java))
        }
    }

    private fun setupBottomNav() {
        findViewById<ImageButton>(R.id.nav_home).isEnabled = false // already here

        findViewById<ImageButton>(R.id.nav_appointments).setOnClickListener {
            startActivity(Intent(this, AppointmentsActivity::class.java))
        }
        findViewById<ImageButton>(R.id.nav_consult).setOnClickListener {
            startActivity(Intent(this, VideoConsult::class.java))
        }
        findViewById<ImageButton>(R.id.nav_patients).setOnClickListener {
            startActivity(Intent(this, PatientRecordsActivity::class.java))
        }
        findViewById<ImageButton>(R.id.nav_profile).setOnClickListener {
            // Reusing the existing patient Profilepage — split this into a dedicated
            // DoctorProfile screen later if doctors need different settings.
            startActivity(Intent(this, Profilepage::class.java))
        }
    }

    private fun render() {
        updateGreeting()
        updateHeaderStats()
        buildAppointmentCards()
    }

    private fun updateGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val salutation = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
        val doctorName = getSharedPreferences("UserSession", MODE_PRIVATE)
            .getString("username", "User")
        tvGreeting.text = "$salutation, Dr. $doctorName"
    }

    private fun updateHeaderStats() {
        val pending = appointments.count { it.status == AppointmentStatus.UPCOMING }
        tvAppointmentsLeft.text = if (pending == 0) {
            "No appointments left for today 🎉"
        } else {
            "You have $pending appointment${if (pending > 1) "s" else ""} left today"
        }
    }

    private fun buildAppointmentCards() {
        llAppointmentList.removeAllViews()

        if (appointments.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            return
        }
        tvEmptyState.visibility = View.GONE

        appointments.forEach { appt -> llAppointmentList.addView(createAppointmentCard(appt)) }
    }

    private fun createAppointmentCard(appt: Appointment): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dpToPx(12) }
            setBackgroundResource(R.drawable.edittext)
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            alpha = if (appt.status == AppointmentStatus.COMPLETED) 0.6f else 1f
        }

        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvName = TextView(this).apply {
            text = appt.patientName
            textSize = 17f
            setTextColor(Color.BLACK)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val tvTimeReason = TextView(this).apply {
            text = "${appt.time} · ${appt.reason}"
            textSize = 13f
            setTextColor(Color.parseColor("#757575"))
            (layoutParams as? LinearLayout.LayoutParams)?.topMargin = dpToPx(3)
        }

        infoLayout.addView(tvName)
        infoLayout.addView(tvTimeReason)

        if (appt.status == AppointmentStatus.COMPLETED) {
            val badge = TextView(this).apply {
                text = "✓ Completed"
                textSize = 11f
                setTextColor(Color.parseColor("#0F6E56"))
                setBackgroundColor(Color.parseColor("#D1F5E8"))
                setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = dpToPx(6) }
            }
            infoLayout.addView(badge)
        }

        val actionsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.marginStart = dpToPx(12) }
        }

        val btnView = makeActionButton("View", "#0C1320") {
            // NOTE: PatientDetailsActivity doesn't exist yet — create it, passing the
            // patient name (and ideally a real patient ID once you have one).
            val intent = Intent(this, PatientDetailsActivity::class.java)
            intent.putExtra("patientName", appt.patientName)
            startActivity(intent)
        }
        actionsLayout.addView(btnView)
        actionsLayout.addView(Space(this).apply { minimumHeight = dpToPx(8) })

        if (appt.status == AppointmentStatus.UPCOMING) {
            val btnCall = makeActionButton("Start Call", "#48B88C") {
                startActivity(Intent(this, VideoConsult::class.java))
            }
            actionsLayout.addView(btnCall)
        }

        card.addView(infoLayout)
        card.addView(actionsLayout)
        return card
    }

    private fun makeActionButton(label: String, colorHex: String, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 13f
            isAllCaps = false
            setTextColor(Color.WHITE)

            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor(colorHex))
                cornerRadius = dpToPx(24).toFloat()
            }

            layoutParams = LinearLayout.LayoutParams(dpToPx(100), dpToPx(38))
            setOnClickListener { onClick() }
        }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}