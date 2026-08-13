package com.firstapp.esehat

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import java.util.Calendar

class AddMedicationDialog(
    private val onAdd: (
        String,
        String,
        String,
        Boolean,
        Int,
        Int
    ) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(
        savedInstanceState: Bundle?
    ): Dialog {

        val view =
            LayoutInflater.from(requireContext())
                .inflate(
                    R.layout.dialog_add_medication,
                    null
                )

        val name =
            view.findViewById<EditText>(
                R.id.et_med_name
            )

        val description =
            view.findViewById<EditText>(
                R.id.et_med_description
            )

        val time =
            view.findViewById<TextView>(
                R.id.tv_selected_time
            )

        val reminder =
            view.findViewById<RadioButton>(
                R.id.rb_reminder
            )

        val selected =
            Calendar.getInstance()

        selected.set(
            Calendar.HOUR_OF_DAY,
            21
        )

        selected.set(
            Calendar.MINUTE,
            0
        )

        time.text =
            "9:00 PM"

        time.setOnClickListener {

            val picker =
                android.app.TimePickerDialog(
                    requireContext(),
                    { _, hour, minute ->

                        selected.set(
                            Calendar.HOUR_OF_DAY,
                            hour
                        )

                        selected.set(
                            Calendar.MINUTE,
                            minute
                        )

                        time.text =
                            formatTime(
                                hour,
                                minute
                            )
                    },
                    21,
                    0,
                    false
                )

            picker.show()
        }

        return AlertDialog.Builder(
            requireContext()
        )
            .setTitle("Add Medication")
            .setView(view)
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Add"
            ) { _, _ ->

                val medName =
                    name.text.toString().trim()

                val desc =
                    description.text.toString().trim()

                if (medName.isEmpty()) {

                    Toast.makeText(
                        requireContext(),
                        "Enter medicine name",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                onAdd(
                    medName,
                    formatTime(
                        selected.get(
                            Calendar.HOUR_OF_DAY
                        ),
                        selected.get(
                            Calendar.MINUTE
                        )
                    ),
                    if (desc.isEmpty())
                        "Medication"
                    else
                        desc,
                    reminder.isChecked,
                    selected.get(
                        Calendar.HOUR_OF_DAY
                    ),
                    selected.get(
                        Calendar.MINUTE
                    )
                )
            }
            .create()
    }

    private fun formatTime(
        hour: Int,
        minute: Int
    ): String {

        val suffix =
            if (hour >= 12) "PM" else "AM"

        val h =
            when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }

        return String.format(
            "%d:%02d %s",
            h,
            minute,
            suffix
        )
    }
}