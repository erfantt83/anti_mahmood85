package com.example.theftprevention.ui.theme

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.theftprevention.R

class SmsSettingsActivity : AppCompatActivity() {
    private lateinit var edtPhoneNumber: EditText
    private lateinit var btnSaveNumber: Button
    private val TAG = "SmsSettings"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_settings)

        edtPhoneNumber = findViewById(R.id.edtPhoneNumber)
        btnSaveNumber = findViewById(R.id.btnSaveNumber)

        val prefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val savedNumber = prefs.getString("sos_phone_number", "")

        edtPhoneNumber.setText(savedNumber)
        Log.d(TAG, "Loaded saved number: $savedNumber")

        btnSaveNumber.setOnClickListener {
            val phone = edtPhoneNumber.text.toString().trim()

            if (isValidIranianNumber(phone)) {
                prefs.edit().putString("sos_phone_number", phone).apply()
                Toast.makeText(this, "شماره ذخیره شد", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Number saved: $phone")
                finish()
            } else {
                Toast.makeText(this, "شماره وارد شده معتبر نیست", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "Invalid number entered: $phone")
            }
        }
    }

    private fun isValidIranianNumber(number: String): Boolean {
        val regex = Regex("^09\\d{9}$")
        return regex.matches(number)
    }
}