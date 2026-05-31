package com.firstapp.esehat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.button.MaterialButton

class HealthTracker : AppCompatActivity() {


    private val medicalData = mapOf(
        "421503" to listOf(
            Triple(
                "Baymax Medical",
                "Shop no. 44, Near Maruti Temple, Gandhi Chowk, Badlapur E",
                "https://maps.google.com/?q=Baymax+Medical+Badlapur"
            ),
            Triple(
                "Interstellar Medical Store",
                "Shop No. 12, Opp. Dr. Gandhi Clinic, Dattawaadi, Badlapur E",
                "https://maps.google.com/?q=Interstellar+Medical+Badlapur"
            )
        ),
        "400001" to listOf(
            Triple(
                "Mumbai Central Pharmacy",
                "Near CST Station, Fort, Mumbai",
                "https://maps.google.com/?q=Mumbai+Central+Pharmacy+Fort"
            )
        )

    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_tracker)

        val pincodeInput   = findViewById<EditText>(R.id.pincode_input)
        val searchButton   = findViewById<MaterialButton>(R.id.btn_search_pincode)
        val pincodeLabel   = findViewById<TextView>(R.id.pincode_label)
        val container      = findViewById<LinearLayout>(R.id.medicals_container)
        val emptyState     = findViewById<LinearLayout>(R.id.empty_state)


        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val savedPin = prefs.getString("pincode", "")
        if (!savedPin.isNullOrEmpty()) {
            pincodeInput.setText(savedPin)
            pincodeLabel.text = "PIN: $savedPin"
            loadMedicals(savedPin, container, emptyState)
        }

        searchButton.setOnClickListener {
            val pin = pincodeInput.text.toString().trim()
            if (pin.length != 6) {
                Toast.makeText(this, "Enter a valid 6-digit PIN code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit().putString("pincode", pin).apply()
            pincodeLabel.text = "PIN: $pin"
            loadMedicals(pin, container, emptyState)
        }

        // Bottom nav
        findViewById<ImageButton>(R.id.homebtn).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<ImageButton>(R.id.healthtrackbtn).setOnClickListener {
            startActivity(Intent(this, HealthTracker::class.java))
        }
        findViewById<ImageButton>(R.id.videoconsult).setOnClickListener {
            startActivity(Intent(this, VideoConsult::class.java))
        }
        findViewById<ImageButton>(R.id.baymaxAI).setOnClickListener {
            startActivity(Intent(this, BaymaxAI::class.java))
        }
        findViewById<ImageButton>(R.id.profileBtn).setOnClickListener {
            startActivity(Intent(this, Profilepage::class.java))
        }
    }

    private fun loadMedicals(
        pincode: String,
        container: LinearLayout,
        emptyState: LinearLayout
    ) {

        container.removeAllViews()

        val medicals = medicalData[pincode]

        if (medicals.isNullOrEmpty()) {

            val notFound = TextView(this).apply {
                text = "No medical stores found for PIN $pincode.\nTry a different PIN code."
                textSize = 14f
                setTextColor(0xFF888888.toInt())
                gravity = android.view.Gravity.CENTER
                setPadding(0, 80, 0, 0)
            }
            container.addView(notFound)
            return
        }

        for ((name, address, mapsUrl) in medicals) {
            val card = layoutInflater.inflate(R.layout.item_medical_card, container, false)

            card.findViewById<TextView>(R.id.medical_name).text = name
            card.findViewById<TextView>(R.id.medical_address).text = "📍 $address"
            card.findViewById<MaterialButton>(R.id.btn_check_now).setOnClickListener {
                openGoogleMaps(mapsUrl)
            }

            container.addView(card)
        }
    }

    private fun openGoogleMaps(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.setPackage("com.google.android.apps.maps")
        try {
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
}