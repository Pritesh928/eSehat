package com.firstapp.esehat

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import org.json.JSONArray
import org.json.JSONObject

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var content: LinearLayout

    private val prefs by lazy {
        getSharedPreferences(
            "AdminLocalData",
            Context.MODE_PRIVATE
        )
    }

    private val sessionPrefs by lazy {
        getSharedPreferences(
            "UserSession",
            Context.MODE_PRIVATE
        )
    }

    data class LocalUser(
        val id: String,
        var name: String,
        var email: String,
        var role: String
    )

    data class LocalAppointment(
        val id: String,
        var patient: String,
        var doctor: String,
        var date: String,
        var time: String,
        var status: String
    )

    private val users = mutableListOf<LocalUser>()
    private val appointments = mutableListOf<LocalAppointment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_admin_dashboard
        )

        content =
            findViewById(R.id.admin_content)

        loadLocalData()

        findViewById<View>(
            R.id.nav_admin_home
        ).setOnClickListener {
            showHome()
        }

        findViewById<View>(
            R.id.nav_admin_users
        ).setOnClickListener {
            showUsers()
        }

        findViewById<View>(
            R.id.nav_admin_appointments
        ).setOnClickListener {
            showAppointments()
        }

        findViewById<View>(
            R.id.nav_admin_profile
        ).setOnClickListener {
            showProfile()
        }

        findViewById<View>(
            R.id.btn_admin_logout
        ).setOnClickListener {
            logout()
        }

        showHome()
    }

    private fun loadLocalData() {

        users.clear()
        appointments.clear()

        val usersJson =
            prefs.getString(
                "users",
                null
            )

        if (usersJson == null) {

            users.add(
                LocalUser(
                    "U001",
                    "Dr. Rahul Sharma",
                    "rahul@esehat.com",
                    "Doctor"
                )
            )

            users.add(
                LocalUser(
                    "U002",
                    "Priya Patil",
                    "priya@esehat.com",
                    "Patient"
                )
            )

            users.add(
                LocalUser(
                    "U003",
                    "Sunita Yadav",
                    "sunita@esehat.com",
                    "ASHA Worker"
                )
            )

            saveUsers()

        } else {

            val array =
                JSONArray(usersJson)

            for (i in 0 until array.length()) {

                val obj =
                    array.getJSONObject(i)

                users.add(
                    LocalUser(
                        obj.getString("id"),
                        obj.getString("name"),
                        obj.getString("email"),
                        obj.getString("role")
                    )
                )
            }
        }

        val appointmentsJson =
            prefs.getString(
                "appointments",
                null
            )

        if (appointmentsJson == null) {

            appointments.add(
                LocalAppointment(
                    "A001",
                    "Priya Patil",
                    "Dr. Rahul Sharma",
                    "14 Aug 2026",
                    "10:30 AM",
                    "Upcoming"
                )
            )

            appointments.add(
                LocalAppointment(
                    "A002",
                    "Amit Shah",
                    "Dr. Neha Joshi",
                    "14 Aug 2026",
                    "12:00 PM",
                    "Completed"
                )
            )

            saveAppointments()

        } else {

            val array =
                JSONArray(appointmentsJson)

            for (i in 0 until array.length()) {

                val obj =
                    array.getJSONObject(i)

                appointments.add(
                    LocalAppointment(
                        obj.getString("id"),
                        obj.getString("patient"),
                        obj.getString("doctor"),
                        obj.getString("date"),
                        obj.getString("time"),
                        obj.getString("status")
                    )
                )
            }
        }
    }

    private fun saveUsers() {

        val array = JSONArray()

        users.forEach {

            val obj = JSONObject()

            obj.put("id", it.id)
            obj.put("name", it.name)
            obj.put("email", it.email)
            obj.put("role", it.role)

            array.put(obj)
        }

        prefs.edit()
            .putString(
                "users",
                array.toString()
            )
            .apply()
    }

    private fun saveAppointments() {

        val array = JSONArray()

        appointments.forEach {

            val obj = JSONObject()

            obj.put("id", it.id)
            obj.put("patient", it.patient)
            obj.put("doctor", it.doctor)
            obj.put("date", it.date)
            obj.put("time", it.time)
            obj.put("status", it.status)

            array.put(obj)
        }

        prefs.edit()
            .putString(
                "appointments",
                array.toString()
            )
            .apply()
    }

    private fun showHome() {

        clearContent()
        selectNavigation(R.id.nav_admin_home)

        addSectionTitle(
            "Overview",
            "System statistics and activity"
        )

        val row1 =
            LinearLayout(this)

        row1.orientation =
            LinearLayout.HORIZONTAL

        row1.weightSum = 2f

        addStatCard(
            row1,
            "Total Users",
            users.size.toString(),
            "👥"
        )

        addStatCard(
            row1,
            "Doctors",
            users.count {
                it.role == "Doctor"
            }.toString(),
            "🩺"
        )

        content.addView(row1)

        val row2 =
            LinearLayout(this)

        row2.orientation =
            LinearLayout.HORIZONTAL

        row2.weightSum = 2f

        addStatCard(
            row2,
            "Patients",
            users.count {
                it.role == "Patient"
            }.toString(),
            "❤️"
        )

        addStatCard(
            row2,
            "Appointments",
            appointments.size.toString(),
            "📅"
        )

        content.addView(row2)

        addSpace(12)

        addSectionTitle(
            "Quick Actions",
            "Manage the system"
        )

        addActionCard(
            "👥",
            "Manage Users",
            "Add, search or remove users"
        ) {
            showUsers()
        }

        addActionCard(
            "📅",
            "Appointments",
            "View and manage appointments"
        ) {
            showAppointments()
        }

        addActionCard(
            "⚙",
            "Admin Profile",
            "Manage your account"
        ) {
            showProfile()
        }

        addSpace(10)

        addSectionTitle(
            "Recent Activity",
            "Latest system activity"
        )

        val recent =
            appointments.takeLast(3).reversed()

        if (recent.isEmpty()) {

            addEmptyCard(
                "No recent activity"
            )

        } else {

            recent.forEach {
                addAppointmentCompact(it)
            }
        }
    }

    private fun showUsers() {

        clearContent()
        selectNavigation(R.id.nav_admin_users)

        addSectionTitle(
            "Users",
            "${users.size} registered users"
        )

        val search =
            EditText(this)

        search.hint =
            "Search users..."

        search.setSingleLine(true)

        search.setPadding(
            20,
            0,
            20,
            0
        )

        search.setBackgroundColor(
            Color.WHITE
        )

        val searchParams =
            LinearLayout.LayoutParams(
                -1,
                52
            )

        searchParams.setMargins(
            0,
            0,
            0,
            14
        )

        content.addView(
            search,
            searchParams
        )

        val addButton =
            TextView(this)

        addButton.text =
            "+  Add New User"

        addButton.textSize =
            15f

        addButton.setTextColor(
            Color.WHITE
        )

        addButton.gravity =
            Gravity.CENTER

        addButton.setBackgroundColor(
            Color.rgb(46, 125, 50)
        )

        addButton.setOnClickListener {
            showAddUserDialog()
        }

        val addParams =
            LinearLayout.LayoutParams(
                -1,
                52
            )

        addParams.setMargins(
            0,
            0,
            0,
            16
        )

        content.addView(
            addButton,
            addParams
        )

        val list =
            LinearLayout(this)

        list.orientation =
            LinearLayout.VERTICAL

        content.addView(list)

        fun renderUsers(
            query: String
        ) {

            list.removeAllViews()

            val filtered =
                users.filter {

                    it.name.contains(
                        query,
                        true
                    ) ||
                            it.email.contains(
                                query,
                                true
                            ) ||
                            it.role.contains(
                                query,
                                true
                            )
                }

            if (filtered.isEmpty()) {

                addEmptyCardTo(
                    list,
                    "No users found"
                )

                return
            }

            filtered.forEach {
                addUserCard(
                    list,
                    it
                )
            }
        }

        search.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    renderUsers(
                        s?.toString() ?: ""
                    )
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {}
            }
        )

        renderUsers("")
    }

    private fun showAddUserDialog() {

        val layout =
            LinearLayout(this)

        layout.orientation =
            LinearLayout.VERTICAL

        layout.setPadding(
            30,
            10,
            30,
            10
        )

        val name =
            EditText(this)

        name.hint =
            "Full name"

        val email =
            EditText(this)

        email.hint =
            "Email"

        email.inputType =
            android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        val role =
            Spinner(this)

        role.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                arrayOf(
                    "Doctor",
                    "Patient",
                    "ASHA Worker"
                )
            )

        layout.addView(name)
        layout.addView(email)
        layout.addView(role)

        AlertDialog.Builder(this)
            .setTitle("Add New User")
            .setView(layout)
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Add"
            ) { _, _ ->

                if (
                    name.text
                        .toString()
                        .trim()
                        .isEmpty()
                ) {
                    return@setPositiveButton
                }

                val newId =
                    "U" +
                            String.format(
                                "%03d",
                                users.size + 1
                            )

                users.add(
                    LocalUser(
                        newId,
                        name.text.toString(),
                        email.text.toString(),
                        role.selectedItem.toString()
                    )
                )

                saveUsers()
                showUsers()
            }
            .show()
    }

    private fun addUserCard(
        parent: LinearLayout,
        user: LocalUser
    ) {

        val card =
            CardView(this)

        card.radius = 22f

        card.cardElevation = 1f

        card.setCardBackgroundColor(
            Color.WHITE
        )

        val layout =
            LinearLayout(this)

        layout.orientation =
            LinearLayout.HORIZONTAL

        layout.gravity =
            Gravity.CENTER_VERTICAL

        layout.setPadding(
            18,
            18,
            14,
            18
        )

        val avatar =
            TextView(this)

        avatar.text =
            user.name
                .firstOrNull()
                ?.uppercase()
                ?: "U"

        avatar.textSize =
            18f

        avatar.gravity =
            Gravity.CENTER

        avatar.setTextColor(
            Color.rgb(46, 125, 50)
        )

        avatar.setBackgroundColor(
            Color.rgb(232, 245, 233)
        )

        val avatarParams =
            LinearLayout.LayoutParams(
                52,
                52
            )

        layout.addView(
            avatar,
            avatarParams
        )

        val info =
            LinearLayout(this)

        info.orientation =
            LinearLayout.VERTICAL

        val infoParams =
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )

        infoParams.setMargins(
            14,
            0,
            10,
            0
        )

        val name =
            TextView(this)

        name.text =
            user.name

        name.textSize =
            16f

        name.setTextColor(
            Color.rgb(16, 24, 40)
        )

        name.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        val email =
            TextView(this)

        email.text =
            user.email

        email.textSize =
            12f

        email.setTextColor(
            Color.rgb(102, 112, 133)
        )

        val role =
            TextView(this)

        role.text =
            user.role

        role.textSize =
            11f

        role.setTextColor(
            Color.rgb(46, 125, 50)
        )

        info.addView(name)
        info.addView(email)
        info.addView(role)

        layout.addView(
            info,
            infoParams
        )

        val delete =
            ImageButton(this)

        delete.setImageResource(
            android.R.drawable.ic_menu_delete
        )

        delete.setBackgroundColor(
            Color.TRANSPARENT
        )

        delete.setOnClickListener {

            AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage(
                    "Remove ${user.name} from the system?"
                )
                .setNegativeButton(
                    "Cancel",
                    null
                )
                .setPositiveButton(
                    "Delete"
                ) { _, _ ->

                    users.remove(user)

                    saveUsers()

                    showUsers()
                }
                .show()
        }

        layout.addView(
            delete,
            LinearLayout.LayoutParams(
                45,
                45
            )
        )

        card.addView(layout)

        val params =
            LinearLayout.LayoutParams(
                -1,
                -2
            )

        params.setMargins(
            0,
            0,
            0,
            12
        )

        parent.addView(
            card,
            params
        )
    }

    private fun showAppointments() {

        clearContent()
        selectNavigation(
            R.id.nav_admin_appointments
        )

        addSectionTitle(
            "Appointments",
            "${appointments.size} total appointments"
        )

        if (appointments.isEmpty()) {

            addEmptyCard(
                "No appointments available"
            )

            return
        }

        appointments.forEach {
            addAppointmentCard(it)
        }
    }

    private fun addAppointmentCard(
        appointment: LocalAppointment
    ) {

        val card =
            CardView(this)

        card.radius = 22f

        card.cardElevation = 1f

        card.setCardBackgroundColor(
            Color.WHITE
        )

        val layout =
            LinearLayout(this)

        layout.orientation =
            LinearLayout.VERTICAL

        layout.setPadding(
            20,
            20,
            20,
            20
        )

        val title =
            TextView(this)

        title.text =
            appointment.patient

        title.textSize =
            17f

        title.setTextColor(
            Color.rgb(16, 24, 40)
        )

        title.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        val doctor =
            TextView(this)

        doctor.text =
            appointment.doctor

        doctor.textSize =
            13f

        doctor.setTextColor(
            Color.rgb(102, 112, 133)
        )

        doctor.setPadding(
            0,
            5,
            0,
            0
        )

        val date =
            TextView(this)

        date.text =
            "${appointment.date}  •  ${appointment.time}"

        date.textSize =
            13f

        date.setTextColor(
            Color.rgb(52, 64, 84)
        )

        date.setPadding(
            0,
            12,
            0,
            10
        )

        val status =
            TextView(this)

        status.text =
            appointment.status

        status.textSize =
            12f

        status.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        status.setTextColor(
            if (
                appointment.status ==
                "Completed"
            )
                Color.rgb(46, 125, 50)
            else
                Color.rgb(245, 124, 0)
        )

        val change =
            TextView(this)

        change.text =
            if (
                appointment.status ==
                "Completed"
            )
                "Mark as Upcoming"
            else
                "Mark as Completed"

        change.textSize =
            13f

        change.setTextColor(
            Color.rgb(46, 125, 50)
        )

        change.setPadding(
            0,
            14,
            0,
            0
        )

        change.setOnClickListener {

            appointment.status =
                if (
                    appointment.status ==
                    "Completed"
                )
                    "Upcoming"
                else
                    "Completed"

            saveAppointments()

            showAppointments()
        }

        layout.addView(title)
        layout.addView(doctor)
        layout.addView(date)
        layout.addView(status)
        layout.addView(change)

        card.addView(layout)

        val params =
            LinearLayout.LayoutParams(
                -1,
                -2
            )

        params.setMargins(
            0,
            0,
            0,
            12
        )

        content.addView(
            card,
            params
        )
    }

    private fun addAppointmentCompact(
        appointment: LocalAppointment
    ) {

        val card =
            CardView(this)

        card.radius = 20f

        card.cardElevation = 1f

        card.setCardBackgroundColor(
            Color.WHITE
        )

        val text =
            TextView(this)

        text.text =
            "${appointment.patient}\n${appointment.doctor}  •  ${appointment.date}"

        text.textSize =
            14f

        text.setTextColor(
            Color.rgb(52, 64, 84)
        )

        text.setPadding(
            18,
            18,
            18,
            18
        )

        card.addView(text)

        val params =
            LinearLayout.LayoutParams(
                -1,
                -2
            )

        params.setMargins(
            0,
            0,
            0,
            10
        )

        content.addView(
            card,
            params
        )
    }

    private fun showProfile() {

        clearContent()
        selectNavigation(
            R.id.nav_admin_profile
        )

        addSectionTitle(
            "Admin Profile",
            "Manage your local account"
        )

        val name =
            prefs.getString(
                "adminName",
                "System Administrator"
            ) ?: "System Administrator"

        val email =
            prefs.getString(
                "adminEmail",
                "admin@esehat.com"
            ) ?: "admin@esehat.com"

        val card =
            CardView(this)

        card.radius = 24f

        card.cardElevation = 1f

        card.setCardBackgroundColor(
            Color.WHITE
        )

        val layout =
            LinearLayout(this)

        layout.orientation =
            LinearLayout.VERTICAL

        layout.setPadding(
            22,
            22,
            22,
            22
        )

        val avatar =
            TextView(this)

        avatar.text = "A"

        avatar.textSize = 28f

        avatar.gravity =
            Gravity.CENTER

        avatar.setTextColor(
            Color.rgb(46, 125, 50)
        )

        avatar.setBackgroundColor(
            Color.rgb(232, 245, 233)
        )

        layout.addView(
            avatar,
            LinearLayout.LayoutParams(
                72,
                72
            )
        )

        addSpaceTo(
            layout,
            14
        )

        val nameText =
            TextView(this)

        nameText.text =
            name

        nameText.textSize =
            20f

        nameText.setTextColor(
            Color.rgb(16, 24, 40)
        )

        nameText.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        layout.addView(nameText)

        val emailText =
            TextView(this)

        emailText.text =
            email

        emailText.textSize =
            13f

        emailText.setTextColor(
            Color.rgb(102, 112, 133)
        )

        emailText.setPadding(
            0,
            5,
            0,
            18
        )

        layout.addView(emailText)

        card.addView(layout)

        content.addView(
            card,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

        addSpace(14)

        val edit =
            TextView(this)

        edit.text =
            "Edit Profile"

        edit.gravity =
            Gravity.CENTER

        edit.textSize =
            15f

        edit.setBackgroundColor(
            Color.rgb(46, 125, 50)
        )

        edit.setOnClickListener {
            showEditProfileDialog()
        }

        content.addView(
            edit,
            LinearLayout.LayoutParams(
                -1,
                54
            )
        )

        addSpace(12)

        val reset =
            TextView(this)

        reset.text =
            "Reset Local Admin Data"

        reset.gravity =
            Gravity.CENTER

        reset.textSize =
            14f

        reset.setTextColor(
            Color.rgb(217, 45, 32)
        )

        reset.setOnClickListener {

            AlertDialog.Builder(this)
                .setTitle(
                    "Reset local data?"
                )
                .setMessage(
                    "This will remove locally stored users and appointments."
                )
                .setNegativeButton(
                    "Cancel",
                    null
                )
                .setPositiveButton(
                    "Reset"
                ) { _, _ ->

                    prefs.edit()
                        .clear()
                        .apply()

                    loadLocalData()

                    showHome()
                }
                .show()
        }

        content.addView(
            reset,
            LinearLayout.LayoutParams(
                -1,
                50
            )
        )
    }

    private fun showEditProfileDialog() {

        val layout =
            LinearLayout(this)

        layout.orientation =
            LinearLayout.VERTICAL

        layout.setPadding(
            30,
            10,
            30,
            10
        )

        val name =
            EditText(this)

        name.hint =
            "Admin name"

        name.setText(
            prefs.getString(
                "adminName",
                "System Administrator"
            )
        )

        val email =
            EditText(this)

        email.hint =
            "Email"

        email.setText(
            prefs.getString(
                "adminEmail",
                "admin@esehat.com"
            )
        )

        layout.addView(name)
        layout.addView(email)

        AlertDialog.Builder(this)
            .setTitle("Edit Profile")
            .setView(layout)
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Save"
            ) { _, _ ->

                prefs.edit()
                    .putString(
                        "adminName",
                        name.text.toString()
                    )
                    .putString(
                        "adminEmail",
                        email.text.toString()
                    )
                    .apply()

                showProfile()
            }
            .show()
    }

    private fun addStatCard(
        parent: LinearLayout,
        title: String,
        value: String,
        icon: String
    ) {

        val card =
            CardView(this)

        card.radius = 22f

        card.cardElevation = 1f

        card.setCardBackgroundColor(
            Color.WHITE
        )

        val layout =
            LinearLayout(this)

        layout.orientation =
            LinearLayout.VERTICAL

        layout.setPadding(
            18,
            18,
            18,
            18
        )

        val iconText =
            TextView(this)

        iconText.text =
            icon

        iconText.textSize =
            22f

        val number =
            TextView(this)

        number.text =
            value

        number.textSize =
            25f

        number.setTextColor(
            Color.rgb(16, 24, 40)
        )

        number.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        val label =
            TextView(this)

        label.text =
            title

        label.textSize =
            12f

        label.setTextColor(
            Color.rgb(102, 112, 133)
        )

        layout.addView(iconText)
        layout.addView(number)
        layout.addView(label)

        card.addView(layout)

        val params =
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )

        params.setMargins(
            0,
            0,
            6,
            12
        )

        parent.addView(
            card,
            params
        )
    }

    private fun addActionCard(
        icon: String,
        title: String,
        subtitle: String,
        action: () -> Unit
    ) {

        val card =
            CardView(this)

        card.radius = 22f

        card.cardElevation = 1f

        card.setCardBackgroundColor(
            Color.WHITE
        )

        card.setOnClickListener {
            action()
        }

        val layout =
            LinearLayout(this)

        layout.orientation =
            LinearLayout.HORIZONTAL

        layout.gravity =
            Gravity.CENTER_VERTICAL

        layout.setPadding(
            20,
            18,
            20,
            18
        )

        val iconText =
            TextView(this)

        iconText.text =
            icon

        iconText.textSize =
            24f

        layout.addView(
            iconText,
            LinearLayout.LayoutParams(
                45,
                45
            )
        )

        val info =
            LinearLayout(this)

        info.orientation =
            LinearLayout.VERTICAL

        val titleText =
            TextView(this)

        titleText.text =
            title

        titleText.textSize =
            16f

        titleText.setTextColor(
            Color.rgb(16, 24, 40)
        )

        titleText.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        val subText =
            TextView(this)

        subText.text =
            subtitle

        subText.textSize =
            12f

        subText.setTextColor(
            Color.rgb(102, 112, 133)
        )

        info.addView(titleText)
        info.addView(subText)

        val infoParams =
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )

        infoParams.setMargins(
            14,
            0,
            0,
            0
        )

        layout.addView(
            info,
            infoParams
        )

        val arrow =
            TextView(this)

        arrow.text =
            "›"

        arrow.textSize =
            28f

        arrow.setTextColor(
            Color.rgb(152, 162, 179)
        )

        layout.addView(
            arrow
        )

        card.addView(layout)

        val params =
            LinearLayout.LayoutParams(
                -1,
                -2
            )

        params.setMargins(
            0,
            0,
            0,
            12
        )

        content.addView(
            card,
            params
        )
    }

    private fun addSectionTitle(
        title: String,
        subtitle: String
    ) {

        val layout =
            LinearLayout(this)

        layout.orientation =
            LinearLayout.VERTICAL

        layout.setPadding(
            2,
            4,
            2,
            14
        )

        val titleText =
            TextView(this)

        titleText.text =
            title

        titleText.textSize =
            20f

        titleText.setTextColor(
            Color.rgb(16, 24, 40)
        )

        titleText.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        val subtitleText =
            TextView(this)

        subtitleText.text =
            subtitle

        subtitleText.textSize =
            12f

        subtitleText.setTextColor(
            Color.rgb(102, 112, 133)
        )

        subtitleText.setPadding(
            0,
            4,
            0,
            0
        )

        layout.addView(titleText)
        layout.addView(subtitleText)

        content.addView(layout)
    }

    private fun addEmptyCard(
        message: String
    ) {

        addEmptyCardTo(
            content,
            message
        )
    }

    private fun addEmptyCardTo(
        parent: LinearLayout,
        message: String
    ) {

        val card =
            CardView(this)

        card.radius = 22f

        card.cardElevation = 1f

        card.setCardBackgroundColor(
            Color.WHITE
        )

        val text =
            TextView(this)

        text.text =
            message

        text.textSize =
            14f

        text.gravity =
            Gravity.CENTER

        text.setTextColor(
            Color.rgb(102, 112, 133)
        )

        text.setPadding(
            20,
            35,
            20,
            35
        )

        card.addView(text)

        parent.addView(
            card,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )
    }

    private fun addSpace(
        height: Int
    ) {

        addSpaceTo(
            content,
            height
        )
    }

    private fun addSpaceTo(
        parent: LinearLayout,
        height: Int
    ) {

        val space =
            Space(this)

        parent.addView(
            space,
            LinearLayout.LayoutParams(
                1,
                height
            )
        )
    }

    private fun clearContent() {

        content.removeAllViews()
    }

    private fun selectNavigation(
        selectedId: Int
    ) {

        val ids =
            listOf(
                R.id.nav_admin_home,
                R.id.nav_admin_users,
                R.id.nav_admin_appointments,
                R.id.nav_admin_profile
            )

        ids.forEach { id ->

            val view =
                findViewById<View>(id)

            view.alpha =
                if (id == selectedId)
                    1f
                else
                    0.65f
        }
    }

    private fun logout() {

        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage(
                "Are you sure you want to logout?"
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Logout"
            ) { _, _ ->

                sessionPrefs.edit()
                    .putBoolean(
                        "isLoggedIn",
                        false
                    )
                    .remove("userRole")
                    .apply()

                val intent =
                    Intent(
                        this,
                        RegisterActivity::class.java
                    )

                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK

                startActivity(intent)

                finish()
            }
            .show()
    }
}