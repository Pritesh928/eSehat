package com.firstapp.esehat

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PatientsRecordsActivity : AppCompatActivity() {
    private lateinit var list: LinearLayout

    private val patients = listOf(
        "Sandeep Dhabade",
        "Shailesh Rathod",
        "Anil Verma",
        "Priya Shah",
        "Rahul Patil",
        "Riya Joshi"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patients_records)

        list = findViewById(R.id.ll_patients)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<EditText>(R.id.et_search).addTextChangedListener(
            object : TextWatcher {

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
                    render(s?.toString().orEmpty())
                }

                override fun afterTextChanged(s: Editable?) {
                }
            }
        )

        render("")
    }

    private fun render(query: String) {

        list.removeAllViews()

        val filtered = patients.filter {
            it.contains(
                query.trim(),
                ignoreCase = true
            )
        }

        val noPatients = findViewById<TextView>(R.id.tv_no_patients)

        noPatients.visibility =
            if (filtered.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }

        filtered.forEachIndexed { index, name ->

            val row = LinearLayout(this).apply {

                orientation = LinearLayout.HORIZONTAL

                gravity = Gravity.CENTER_VERTICAL

                setPadding(
                    dpToPx(16),
                    dpToPx(16),
                    dpToPx(16),
                    dpToPx(16)
                )

                setBackgroundResource(R.drawable.edittext)

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also {
                    it.bottomMargin = dpToPx(10)
                }
            }

            // Patient information
            val patientText = TextView(this)

            patientText.setText(
                "$name\nPatient ID: P${String.format("%03d", index + 1)}"
            )

            patientText.textSize = 16f
            patientText.setTextColor(Color.BLACK)

            patientText.layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

            // View button
            val button = Button(this)

            button.setText("View")
            button.isAllCaps = false
            button.textSize = 13f

            button.setTextColor(Color.WHITE)

            button.setBackgroundColor(
                Color.parseColor("#0C1320")
            )

            button.layoutParams = LinearLayout.LayoutParams(
                dpToPx(90),
                dpToPx(45)
            )

            button.setOnClickListener {

                val intent = Intent(
                    this@PatientsRecordsActivity,
                    PatientDetailsActivity::class.java
                )

                intent.putExtra(
                    "patientName",
                    name
                )

                startActivity(intent)
            }

            row.addView(patientText)
            row.addView(button)

            list.addView(row)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

}
