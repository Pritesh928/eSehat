package com.firstapp.esehat

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class VideoConsult : AppCompatActivity() {


    private val doctor1Phone = "917276872115"
    private val doctor2Phone = "919892240434"
    private val doctor3Phone = "918779560816"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_consult)


        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val username = prefs.getString("username", "User")
        findViewById<TextView>(R.id.username_text).text = "Hi, $username"


        findViewById<MaterialButton>(R.id.call_btn1).setOnClickListener {
            showCallDialog("Dr. Ayush Telawane", doctor1Phone)
        }
        findViewById<MaterialButton>(R.id.call_btn2).setOnClickListener {
            showCallDialog("Dr. Soham Suvarna", doctor2Phone)
        }
        findViewById<MaterialButton>(R.id.call_btn3).setOnClickListener {
            showCallDialog("Dr. Dishant Soyam", doctor3Phone)
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

    private fun showCallDialog(doctorName: String, phone: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_call_options)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.findViewById<TextView>(R.id.dialog_doctor_name).text = doctorName

        dialog.findViewById<MaterialButton>(R.id.btn_video_call).setOnClickListener {

            val uri = Uri.parse("https://wa.me/$phone")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.whatsapp")
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.findViewById<MaterialButton>(R.id.btn_audio_call).setOnClickListener {

            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:+$phone")
            startActivity(intent)
            dialog.dismiss()
        }

        dialog.findViewById<TextView>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}