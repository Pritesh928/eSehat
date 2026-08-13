package com.firstapp.esehat

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class PatientDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_details)

        val name = intent.getStringExtra("patientName") ?: "Patient"
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tv_patient_name).text = name
        findViewById<TextView>(R.id.tv_patient_id).text = "Patient ID: ${name.take(2).uppercase()}-${name.hashCode().toString().takeLast(4)}"

        findViewById<Button>(R.id.btn_prescribe).setOnClickListener {
            startActivity(Intent(this, PrescriptionActivity::class.java).putExtra("patientName", name))
        }
        findViewById<Button>(R.id.btn_call_patient).setOnClickListener {
            startActivity(Intent(this, VideoConsult::class.java))
        }
    }
}
