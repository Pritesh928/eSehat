package com.firstapp.esehat

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class AshaDashboardActivity : AppCompatActivity() {

    private lateinit var content: LinearLayout
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView

    private val patients = mutableListOf(
        "Ravi Kumar",
        "Sita Devi",
        "Anil Verma",
        "Priya Shah",
        "Rahul Patil",
        "Neha Joshi"
    )

    private val followUps = mutableListOf(
        "Ravi Kumar - Diabetes follow-up",
        "Sita Devi - Blood pressure check",
        "Priya Shah - Pregnancy follow-up"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asha_dashboard)

        content = findViewById(R.id.asha_content)
        tvTitle = findViewById(R.id.tv_asha_title)
        tvSubtitle = findViewById(R.id.tv_asha_subtitle)

        findViewById<ImageButton>(R.id.btn_asha_logout)
            .setOnClickListener {
                logout()
            }

        findViewById<LinearLayout>(R.id.nav_asha_home)
            .setOnClickListener {
                showDashboard()
            }

        findViewById<LinearLayout>(R.id.nav_asha_patients)
            .setOnClickListener {
                showPatients()
            }

        findViewById<LinearLayout>(R.id.nav_asha_followups)
            .setOnClickListener {
                showFollowUps()
            }

        findViewById<LinearLayout>(R.id.nav_asha_profile)
            .setOnClickListener {
                showProfile()
            }

        showDashboard()
    }

    private fun showDashboard() {

        tvTitle.text = "ASHA Dashboard"
        tvSubtitle.text = "Good morning, ASHA Worker"

        content.removeAllViews()

        content.addView(
            createSectionTitle("Today's Overview")
        )

        content.addView(
            createStatCard(
                "Assigned Patients",
                patients.size.toString(),
                "#E8F3FF"
            )
        )

        content.addView(
            createStatCard(
                "Pending Follow-ups",
                followUps.size.toString(),
                "#E8FFF4"
            )
        )

        content.addView(
            createStatCard(
                "Today's Visits",
                "4",
                "#FFF4E8"
            )
        )

        content.addView(
            createSectionTitle("Quick Actions")
        )

        content.addView(
            createActionButton(
                "My Patients",
                "View and search assigned patients"
            ) {
                showPatients()
            }
        )

        content.addView(
            createActionButton(
                "Register Patient",
                "Register a new patient"
            ) {
                showRegisterPatient()
            }
        )

        content.addView(
            createActionButton(
                "Health Checkup",
                "Record patient's health information"
            ) {
                showPatientSelectionForCheckup()
            }
        )

        content.addView(
            createActionButton(
                "Follow-ups",
                "View pending patient follow-ups"
            ) {
                showFollowUps()
            }
        )

        content.addView(
            createActionButton(
                "Emergency / Referral",
                "Refer a patient to a doctor"
            ) {
                showEmergency()
            }
        )
    }

    private fun showPatients() {

        tvTitle.text = "My Patients"
        tvSubtitle.text = "${patients.size} assigned patients"

        content.removeAllViews()

        val search = EditText(this)

        search.hint = "Search patient"
        search.setSingleLine(true)

        content.addView(search)

        val patientContainer = LinearLayout(this)

        patientContainer.orientation = LinearLayout.VERTICAL

        content.addView(patientContainer)

        fun render(query: String) {

            patientContainer.removeAllViews()

            patients
                .filter {
                    it.contains(
                        query.trim(),
                        ignoreCase = true
                    )
                }
                .forEachIndexed { index, name ->

                    patientContainer.addView(
                        createPatientCard(
                            name,
                            index
                        )
                    )
                }
        }

        search.addTextChangedListener(
            object : android.text.TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    render(s?.toString() ?: "")
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) {
                }
            }
        )

        render("")
    }

    private fun createPatientCard(
        name: String,
        index: Int
    ): View {

        val row = LinearLayout(this)

        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL

        row.setPadding(
            dp(16),
            dp(16),
            dp(16),
            dp(16)
        )

        row.setBackgroundResource(
            R.drawable.edittext
        )

        row.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).also {
            it.bottomMargin = dp(10)
        }

        val info = LinearLayout(this)

        info.orientation = LinearLayout.VERTICAL

        info.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        val nameText = TextView(this)

        nameText.setText(name)
        nameText.textSize = 16f
        nameText.setTextColor(Color.BLACK)
        nameText.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        val idText = TextView(this)

        idText.setText(
            "Patient ID: P${String.format("%03d", index + 1)}"
        )

        idText.textSize = 13f
        idText.setTextColor(Color.GRAY)

        info.addView(nameText)
        info.addView(idText)

        val button = Button(this)

        button.setText("Open")
        button.isAllCaps = false

        button.setOnClickListener {
            showPatientDetails(name)
        }

        row.addView(info)
        row.addView(button)

        return row
    }

    private fun showPatientDetails(name: String) {

        val options = arrayOf(
            "View Details",
            "Record Checkup",
            "Record Vitals",
            "Schedule Follow-up",
            "Refer to Doctor"
        )

        AlertDialog.Builder(this)
            .setTitle(name)
            .setItems(options) { _, which ->

                when (which) {

                    0 -> showPatientInfo(name)

                    1 -> showCheckupForm(name)

                    2 -> showVitalsForm(name)

                    3 -> showFollowUpDialog(name)

                    4 -> showReferralDialog(name)
                }
            }
            .show()
    }

    private fun showPatientInfo(name: String) {

        AlertDialog.Builder(this)
            .setTitle(name)
            .setMessage(
                "Patient ID: P001\n\n" +
                        "Age: 34\n" +
                        "Gender: Not specified\n\n" +
                        "Last Visit:\n" +
                        "General health checkup\n\n" +
                        "Current Status: Active"
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showRegisterPatient() {

        val layout = LinearLayout(this)

        layout.orientation = LinearLayout.VERTICAL

        layout.setPadding(
            dp(20),
            dp(10),
            dp(20),
            dp(10)
        )

        val name = EditText(this)
        name.hint = "Patient name"

        val age = EditText(this)
        age.hint = "Age"
        age.inputType =
            android.text.InputType.TYPE_CLASS_NUMBER

        val phone = EditText(this)
        phone.hint = "Phone number"
        phone.inputType =
            android.text.InputType.TYPE_CLASS_PHONE

        layout.addView(name)
        layout.addView(age)
        layout.addView(phone)

        AlertDialog.Builder(this)
            .setTitle("Register Patient")
            .setView(layout)
            .setPositiveButton("Register") { _, _ ->

                val patientName =
                    name.text.toString().trim()

                if (patientName.isNotEmpty()) {

                    patients.add(patientName)

                    Toast.makeText(
                        this,
                        "Patient registered successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    showPatients()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPatientSelectionForCheckup() {

        if (patients.isEmpty()) {
            Toast.makeText(
                this,
                "No patients available",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        AlertDialog.Builder(this)
            .setTitle("Select Patient")
            .setItems(
                patients.toTypedArray()
            ) { _, which ->

                showCheckupForm(
                    patients[which]
                )
            }
            .show()
    }

    private fun showCheckupForm(
        patientName: String
    ) {

        val layout = LinearLayout(this)

        layout.orientation = LinearLayout.VERTICAL

        layout.setPadding(
            dp(20),
            dp(10),
            dp(20),
            dp(10)
        )

        val symptoms = EditText(this)

        symptoms.hint = "Symptoms / observations"

        val diagnosis = EditText(this)

        diagnosis.hint = "Health condition"

        val notes = EditText(this)

        notes.hint = "Additional notes"

        notes.minLines = 3

        layout.addView(symptoms)
        layout.addView(diagnosis)
        layout.addView(notes)

        AlertDialog.Builder(this)
            .setTitle("Health Checkup")
            .setMessage(patientName)
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->

                Toast.makeText(
                    this,
                    "Checkup saved for $patientName",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showVitalsForm(
        patientName: String
    ) {

        val layout = LinearLayout(this)

        layout.orientation = LinearLayout.VERTICAL

        layout.setPadding(
            dp(20),
            dp(10),
            dp(20),
            dp(10)
        )

        val temperature = EditText(this)
        temperature.hint = "Temperature °C"

        val bp = EditText(this)
        bp.hint = "Blood Pressure"

        val pulse = EditText(this)
        pulse.hint = "Pulse / min"

        val weight = EditText(this)
        weight.hint = "Weight kg"

        layout.addView(temperature)
        layout.addView(bp)
        layout.addView(pulse)
        layout.addView(weight)

        AlertDialog.Builder(this)
            .setTitle("Record Vitals")
            .setMessage(patientName)
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->

                Toast.makeText(
                    this,
                    "Vitals saved",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFollowUps() {

        tvTitle.text = "Follow-ups"
        tvSubtitle.text = "Pending patient follow-ups"

        content.removeAllViews()

        content.addView(
            createAddButton("Add Follow-up") {
                showFollowUpDialog("")
            }
        )

        if (followUps.isEmpty()) {

            content.addView(
                createInfoCard(
                    "No Follow-ups",
                    "There are no pending follow-ups."
                )
            )

            return
        }

        followUps.forEachIndexed { index, followUp ->

            val card = createInfoCard(
                "Follow-up",
                followUp
            )

            card.setOnLongClickListener {

                AlertDialog.Builder(this)
                    .setTitle("Remove Follow-up?")
                    .setMessage(followUp)
                    .setPositiveButton("Remove") { _, _ ->

                        followUps.removeAt(index)
                        showFollowUps()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()

                true
            }

            content.addView(card)
        }
    }

    private fun showFollowUpDialog(
        existingPatient: String
    ) {

        val layout = LinearLayout(this)

        layout.orientation = LinearLayout.VERTICAL

        layout.setPadding(
            dp(20),
            dp(10),
            dp(20),
            dp(10)
        )

        val patient = EditText(this)

        patient.hint = "Patient name"

        if (existingPatient.isNotEmpty()) {
            patient.setText(existingPatient)
        }

        val reason = EditText(this)

        reason.hint = "Reason for follow-up"

        layout.addView(patient)
        layout.addView(reason)

        AlertDialog.Builder(this)
            .setTitle("Schedule Follow-up")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->

                val patientName =
                    patient.text.toString().trim()

                val followUpReason =
                    reason.text.toString().trim()

                if (
                    patientName.isNotEmpty() &&
                    followUpReason.isNotEmpty()
                ) {

                    followUps.add(
                        "$patientName - $followUpReason"
                    )

                    Toast.makeText(
                        this,
                        "Follow-up scheduled",
                        Toast.LENGTH_SHORT
                    ).show()

                    showFollowUps()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEmergency() {

        val options = arrayOf(
            "Refer to Doctor",
            "Emergency Case",
            "Call Medical Support"
        )

        AlertDialog.Builder(this)
            .setTitle("Emergency / Referral")
            .setItems(options) { _, which ->

                when (which) {

                    0 -> {

                        showReferralDialog("")
                    }

                    1 -> {

                        AlertDialog.Builder(this)
                            .setTitle("Emergency Case")
                            .setMessage(
                                "Emergency case recorded.\n\n" +
                                        "Please contact the nearest medical facility."
                            )
                            .setPositiveButton("OK", null)
                            .show()
                    }

                    2 -> {

                        Toast.makeText(
                            this,
                            "Medical support request created",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }

    private fun showReferralDialog(
        patientName: String
    ) {

        val input = EditText(this)

        input.hint = "Reason for referral"

        AlertDialog.Builder(this)
            .setTitle(
                if (patientName.isEmpty())
                    "Doctor Referral"
                else
                    "Refer $patientName"
            )
            .setView(input)
            .setPositiveButton("Submit") { _, _ ->

                Toast.makeText(
                    this,
                    "Referral submitted successfully",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showProfile() {

        tvTitle.text = "ASHA Profile"
        tvSubtitle.text = "Account information"

        content.removeAllViews()

        content.addView(
            createInfoCard(
                "ASHA Worker",
                "eSehat Community Health Worker\n\n" +
                        "Assigned Area: Local Community\n" +
                        "Status: Active"
            )
        )

        content.addView(
            createActionButton(
                "Logout",
                "Sign out of your account"
            ) {
                logout()
            }
        )
    }

    private fun createStatCard(
        title: String,
        value: String,
        backgroundColor: String
    ): View {

        val card = LinearLayout(this)

        card.orientation = LinearLayout.VERTICAL

        card.setPadding(
            dp(20),
            dp(18),
            dp(20),
            dp(18)
        )

        card.setBackgroundColor(
            Color.parseColor(backgroundColor)
        )

        card.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.bottomMargin = dp(10)
            }

        val number = TextView(this)

        number.setText(value)
        number.textSize = 28f
        number.setTextColor(Color.BLACK)
        number.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        val label = TextView(this)

        label.setText(title)
        label.textSize = 14f
        label.setTextColor(Color.DKGRAY)

        card.addView(number)
        card.addView(label)

        return card
    }

    private fun createActionButton(
        title: String,
        subtitle: String,
        action: () -> Unit
    ): View {

        val layout = LinearLayout(this)

        layout.orientation = LinearLayout.VERTICAL

        layout.setPadding(
            dp(18),
            dp(16),
            dp(18),
            dp(16)
        )

        layout.setBackgroundResource(
            R.drawable.edittext
        )

        layout.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.bottomMargin = dp(12)
            }

        val titleView = TextView(this)

        titleView.setText(title)
        titleView.textSize = 17f
        titleView.setTextColor(Color.BLACK)
        titleView.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        val subtitleView = TextView(this)

        subtitleView.setText(subtitle)
        subtitleView.textSize = 13f
        subtitleView.setTextColor(Color.GRAY)

        layout.addView(titleView)
        layout.addView(subtitleView)

        layout.setOnClickListener {
            action()
        }

        return layout
    }

    private fun createAddButton(
        title: String,
        action: () -> Unit
    ): Button {

        val button = Button(this)

        button.setText(title)
        button.isAllCaps = false
        button.setTextColor(Color.WHITE)
        button.setBackgroundColor(
            Color.parseColor("#48B88C")
        )

        button.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
            ).also {
                it.bottomMargin = dp(12)
            }

        button.setOnClickListener {
            action()
        }

        return button
    }

    private fun createInfoCard(
        title: String,
        message: String
    ): LinearLayout {

        val layout = LinearLayout(this)

        layout.orientation = LinearLayout.VERTICAL

        layout.setPadding(
            dp(18),
            dp(18),
            dp(18),
            dp(18)
        )

        layout.setBackgroundResource(
            R.drawable.edittext
        )

        layout.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.bottomMargin = dp(12)
            }

        val titleView = TextView(this)

        titleView.setText(title)
        titleView.textSize = 16f
        titleView.setTextColor(Color.BLACK)
        titleView.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        val messageView = TextView(this)

        messageView.setText(message)
        messageView.textSize = 14f
        messageView.setTextColor(Color.DKGRAY)

        layout.addView(titleView)
        layout.addView(messageView)

        return layout
    }

    private fun createSectionTitle(
        title: String
    ): TextView {

        val text = TextView(this)

        text.setText(title)
        text.textSize = 20f
        text.setTextColor(Color.BLACK)
        text.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        text.setPadding(
            0,
            dp(12),
            0,
            dp(12)
        )

        return text
    }

    private fun logout() {

        getSharedPreferences(
            "UserSession",
            MODE_PRIVATE
        )
            .edit()
            .clear()
            .apply()

        finish()
    }

    private fun dp(value: Int): Int {
        return (
                value *
                        resources.displayMetrics.density
                ).toInt()
    }
}