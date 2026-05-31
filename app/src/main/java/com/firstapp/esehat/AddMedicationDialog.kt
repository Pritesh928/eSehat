package com.firstapp.esehat

import android.app.Dialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class AddMedicationDialog(
    private val onAdd: (name: String, timing: String, desc: String) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
        }

        val etName = EditText(context).apply {
            hint = "Medication name"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }

        val etDesc = EditText(context).apply {
            hint = "Condition / description"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }

        val timings = arrayOf(
            "Before Breakfast", "After Breakfast",
            "Before Lunch", "After Lunch",
            "Before Dinner", "After Dinner", "Bedtime"
        )
        val spinner = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, timings)
            setSelection(5)
        }

        layout.addView(etName)
        layout.addView(Space(context).apply { minimumHeight = 24 })
        layout.addView(etDesc)
        layout.addView(Space(context).apply { minimumHeight = 24 })
        layout.addView(TextView(context).apply { text = "When to take:" })
        layout.addView(spinner)

        return AlertDialog.Builder(context)
            .setTitle("Add Medication")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                val desc = etDesc.text.toString().trim()
                val timing = spinner.selectedItem.toString()
                if (name.isNotEmpty()) {
                    onAdd(name, timing, desc.ifEmpty { "No description" })
                } else {
                    Toast.makeText(context, "Please enter a medication name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
}