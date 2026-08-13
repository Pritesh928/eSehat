package com.firstapp.esehat

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class FollowUpActivity : AppCompatActivity() {
    private lateinit var list: LinearLayout
    private val followUps = mutableListOf("Ravi Kumar — 18 Aug 2026", "Sita Devi — 25 Aug 2026")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_follow_up)
        list = findViewById(R.id.ll_followups)
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_add_followup).setOnClickListener { showAddDialog() }
        render()
    }

    private fun render() {
        list.removeAllViews()
        followUps.forEach { item ->
            val row = TextView(this).apply {
                text = "📅  $item"
                textSize = 16f
                setTextColor(android.graphics.Color.BLACK)
                setPadding(18, 20, 18, 20)
                setBackgroundResource(R.drawable.edittext)
                layoutParams = LinearLayout.LayoutParams(-1, -2).also { it.bottomMargin = 10 }
            }
            row.setOnLongClickListener {
                followUps.remove(item)
                render()
                true
            }
            list.addView(row)
        }
    }

    private fun showAddDialog() {
        val input = EditText(this).apply { hint = "Patient — Date" }
        AlertDialog.Builder(this)
            .setTitle("Add Follow-up")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val value = input.text.toString().trim()
                if (value.isNotEmpty()) {
                    followUps.add(value)
                    render()
                    Toast.makeText(this, "Follow-up added", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
