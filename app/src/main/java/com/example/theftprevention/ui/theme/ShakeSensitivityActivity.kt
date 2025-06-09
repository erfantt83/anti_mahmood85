package com.example.theftprevention

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ShakeSensitivityActivity : AppCompatActivity() {

    private lateinit var sensitivityInput: EditText
    private lateinit var saveButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shake_sensitivity)

        sensitivityInput = findViewById(R.id.editTextSensitivity)
        saveButton = findViewById(R.id.btnSaveSensitivity)
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        // مقدار فعلی را نمایش بده
        val currentSensitivity = sharedPreferences.getFloat("shake_threshold", 1.0f)
        sensitivityInput.setText(currentSensitivity.toString())

        saveButton.setOnClickListener {
            val inputText = sensitivityInput.text.toString()
            val sensitivity = inputText.toFloatOrNull()

            if (sensitivity != null && sensitivity > 0) {
                sharedPreferences.edit().putFloat("shake_threshold", sensitivity).apply()
                Toast.makeText(this, "حساسیت ذخیره شد", Toast.LENGTH_SHORT).show()
                finish() // بستن صفحه و بازگشت
            } else {
                Toast.makeText(this, "مقدار وارد شده معتبر نیست", Toast.LENGTH_SHORT).show()
            }
        }
    }
}