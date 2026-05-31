package com.firstapp.esehat

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class OTPActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otpactivity)

        val verifyButton : Button = findViewById<Button>(R.id.verification)

        verifyButton.setOnClickListener {
            //intent init 
            val intent = Intent(this, MainActivity::class.java)
            //start intent
            startActivity(intent)
        }

    }
}