package com.firstapp.esehat

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class PrescriptionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prescription)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        val patient = intent.getStringExtra("patientName").orEmpty()
        if (patient.isNotEmpty()) findViewById<EditText>(R.id.et_patient).setText(patient)

        findViewById<Button>(R.id.btn_save_prescription).setOnClickListener {
            val p = findViewById<EditText>(R.id.et_patient).text.toString().trim()
            val med = findViewById<EditText>(R.id.et_medicine).text.toString().trim()
            val dose = findViewById<EditText>(R.id.et_dosage).text.toString().trim()

            if (p.isEmpty() || med.isEmpty() || dose.isEmpty()) {
                Toast.makeText(this, "Please fill patient, medicine and dosage", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            getSharedPreferences("DoctorData", MODE_PRIVATE).edit()
                .putString("lastPrescription", "$p|$med|$dose").apply()

            findViewById<TextView>(R.id.tv_saved).text =
                "Prescription saved for $p.\n$med — $dose"
            Toast.makeText(this, "Prescription saved", Toast.LENGTH_SHORT).show()
        }
    }
}
