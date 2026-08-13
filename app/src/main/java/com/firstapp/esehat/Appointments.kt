package com.firstapp.esehat

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class Appointments : AppCompatActivity() {
    private lateinit var list: LinearLayout
    private lateinit var count: TextView
    private val appointments = mutableListOf(
        Appointment(1, "Ravi Kumar", "10:30 AM", "Fever & body ache"),
        Appointment(2, "Sita Devi", "11:15 AM", "Follow-up: Diabetes"),
        Appointment(3, "Anil Verma", "12:00 PM", "Skin rash consultation")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointments)
        list = findViewById(R.id.ll_appointments)
        count = findViewById(R.id.tv_appointment_count)
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
        render()
    }

    private fun render() {
        list.removeAllViews()
        val pending = appointments.count { it.status == AppointmentStatus.UPCOMING }
        count.text = "$pending upcoming · ${appointments.size} total"
        findViewById<TextView>(R.id.tv_empty).visibility = if (appointments.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE

        appointments.forEach { a ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(18, 18, 18, 18)
                setBackgroundResource(R.drawable.edittext)
                layoutParams = LinearLayout.LayoutParams(-1, -2).also { it.bottomMargin = 12 }
            }
            val name = TextView(this).apply {
                text = a.patientName
                textSize = 18f
                setTextColor(Color.BLACK)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            val details = TextView(this).apply {
                text = "${a.time}  •  ${a.reason}"
                textSize = 14f
                setTextColor(Color.DKGRAY)
            }
            val buttons = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
            }
            val view = Button(this).apply {
                text = "View"
                isAllCaps = false
                setOnClickListener {
                    startActivity(Intent(this@Appointments, PatientDetailsActivity::class.java).putExtra("patientName", a.patientName))
                }
            }
            val status = Button(this).apply {
                text = if (a.status == AppointmentStatus.UPCOMING) "Complete" else "Completed"
                isAllCaps = false
                isEnabled = a.status == AppointmentStatus.UPCOMING
                setOnClickListener {
                    a.status = AppointmentStatus.COMPLETED
                    render()
                    Toast.makeText(this@Appointments, "Appointment completed", Toast.LENGTH_SHORT).show()
                }
            }
            buttons.addView(view)
            buttons.addView(status)
            card.addView(name)
            card.addView(details)
            card.addView(buttons)
            list.addView(card)
        }
    }
}
