package com.firstapp.esehat

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

enum class UserRole { DOCTOR, PATIENT, ADMIN, ASHA_WORKER }

class RoleSelection : AppCompatActivity() {

    private var selectedRole: UserRole? = null

    private lateinit var cards: Map<UserRole, CardView>
    private lateinit var radios: Map<UserRole, RadioButton>
    private lateinit var continueBtnCard: CardView
    private lateinit var btnContinue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_selection)

        cards = mapOf(
            UserRole.DOCTOR to findViewById(R.id.card_doctor),
            UserRole.PATIENT to findViewById(R.id.card_patient),
            UserRole.ADMIN to findViewById(R.id.card_admin),
            UserRole.ASHA_WORKER to findViewById(R.id.card_asha)
        )
        radios = mapOf(
            UserRole.DOCTOR to findViewById(R.id.radio_doctor),
            UserRole.PATIENT to findViewById(R.id.radio_patient),
            UserRole.ADMIN to findViewById(R.id.radio_admin),
            UserRole.ASHA_WORKER to findViewById(R.id.radio_asha)
        )
        continueBtnCard = findViewById(R.id.continueBtnCard)
        btnContinue = findViewById(R.id.btnContinue)

        cards.forEach { (role, card) ->
            card.setOnClickListener { selectRole(role) }
        }

        btnContinue.setOnClickListener {
            selectedRole?.let { role -> goToDashboard(role) }
        }

        updateContinueButtonState()
    }

    private fun selectRole(role: UserRole) {
        selectedRole = role
        radios.forEach { (r, radioButton) -> radioButton.isChecked = (r == role) }
        updateContinueButtonState()
    }

    private fun updateContinueButtonState() {
        val enabled = selectedRole != null
        continueBtnCard.setCardBackgroundColor(
            Color.parseColor(if (enabled) "#2E7D32" else "#CCCCCC")
        )
        btnContinue.isEnabled = enabled
    }

    private fun goToDashboard(role: UserRole) {
        getSharedPreferences("UserSession", MODE_PRIVATE)
            .edit()
            .putString("userRole", role.name)
            .apply()

        val target = when (role) {
            UserRole.DOCTOR -> DoctorDashboard::class.java
            UserRole.PATIENT -> MainActivity::class.java
            UserRole.ADMIN -> AdminDashboard::class.java
            UserRole.ASHA_WORKER -> AshaDashboardActivity::class.java
        }

        val intent = Intent(this, target)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}