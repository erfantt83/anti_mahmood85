package com.example.theftprevention

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SoundSelectionActivity : AppCompatActivity() {

    private lateinit var radioGroup: RadioGroup
    private lateinit var btnSave: Button
    private lateinit var prefs: SharedPreferences

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sound_selection)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        radioGroup = findViewById(R.id.radioGroup)
        btnSave = findViewById(R.id.btnSaveSound)

        val currentSound = prefs.getString("selected_alarm_sound", "alarm1")

        when (currentSound) {
            "alarm2" -> findViewById<RadioButton>(R.id.radio_alarm2).isChecked = true
            "alarm3" -> findViewById<RadioButton>(R.id.radio_alarm3).isChecked = true
            else -> findViewById<RadioButton>(R.id.radio_alarm1).isChecked = true
        }

        btnSave.setOnClickListener {
            val selectedId = radioGroup.checkedRadioButtonId
            val selectedKey = when (selectedId) {
                R.id.radio_alarm2 -> "alarm2"
                R.id.radio_alarm3 -> "alarm3"
                else -> "alarm1"
            }

            prefs.edit().putString("selected_alarm_sound", selectedKey).apply()
            Toast.makeText(this, "آژیر انتخاب شد", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}