package com.firstapp.esehat

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object MedicationStorage {

    private const val PREFS = "MedicationPrefs"
    private const val KEY_MEDICATIONS = "medications"

    fun save(context: Context, medications: List<Medication>) {
        val array = JSONArray()

        medications.forEach {
            val obj = JSONObject()

            obj.put("id", it.id)
            obj.put("name", it.name)
            obj.put("timing", it.timing)
            obj.put("description", it.description)
            obj.put("status", it.status.name)
            obj.put("reminderEnabled", it.reminderEnabled)
            obj.put("reminderHour", it.reminderHour)
            obj.put("reminderMinute", it.reminderMinute)

            array.put(obj)
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MEDICATIONS, array.toString())
            .apply()
    }

    fun load(context: Context): MutableList<Medication> {

        val result = mutableListOf<Medication>()

        val json = context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MEDICATIONS, null)
            ?: return result

        try {
            val array = JSONArray(json)

            for (i in 0 until array.length()) {

                val obj = array.getJSONObject(i)

                result.add(
                    Medication(
                        id = obj.getInt("id"),
                        name = obj.getString("name"),
                        timing = obj.getString("timing"),
                        description = obj.getString("description"),
                        status = MedStatus.valueOf(
                            obj.optString("status", "PENDING")
                        ),
                        reminderEnabled = obj.optBoolean(
                            "reminderEnabled",
                            false
                        ),
                        reminderHour = obj.optInt(
                            "reminderHour",
                            21
                        ),
                        reminderMinute = obj.optInt(
                            "reminderMinute",
                            0
                        )
                    )
                )
            }

        } catch (_: Exception) {
        }

        return result
    }

    fun updateStatus(
        context: Context,
        id: Int,
        status: MedStatus
    ) {

        val medications = load(context)

        medications.find {
            it.id == id
        }?.status = status

        save(context, medications)
    }
}