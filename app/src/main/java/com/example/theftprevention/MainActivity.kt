package com.example.theftprevention

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.hardware.*
import android.location.Location
import android.location.LocationManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.example.theftprevention.ui.theme.ChangePasswordActivity
import com.example.theftprevention.ui.theme.SmsSettingsActivity
import com.example.theftprevention.ui.theme.SoundManager
import kotlin.math.abs
import kotlin.math.sqrt
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource


class MainActivity : AppCompatActivity() {
    private lateinit var batterySwitch: Switch
    private lateinit var shakeSwitch: Switch
    private lateinit var btnChangePassword: Button
    private lateinit var btnSmsSettings: Button
    private lateinit var batteryReceiver: BroadcastReceiver
    private lateinit var sensorManager: SensorManager
    private var shakeListener: SensorEventListener? = null
    private lateinit var sharedPreferences: SharedPreferences
    private var lastSmsTime:Long=0
    private var firstSmsSent:Boolean=false

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkAndRequestPermissions()
        batterySwitch = findViewById(R.id.switch_battery_mode)
        shakeSwitch = findViewById(R.id.switch_shake_mode)
        btnChangePassword = findViewById(R.id.btnChangePassword)
        btnSmsSettings = findViewById(R.id.btnSmsSettings) // دکمه تنظیم شماره

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        batterySwitch.isChecked = sharedPreferences.getBoolean("battery_mode", false)
        shakeSwitch.isChecked = sharedPreferences.getBoolean("shake_mode", false)

        batterySwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("battery_mode", isChecked).apply()
        }

        shakeSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("shake_mode", isChecked).apply()
            if (isChecked) startShakeDetection() else stopShakeDetection()
        }

        btnChangePassword.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        btnSmsSettings.setOnClickListener {
            startActivity(Intent(this, SmsSettingsActivity::class.java))
        }

        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val status: Int = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                val isCharging =
                    status == BatteryManager.BATTERY_PLUGGED_USB || status == BatteryManager.BATTERY_PLUGGED_AC
                if (!isCharging && batterySwitch.isChecked) {
                    SoundManager.playAlarm(context)
                    sendLocationSms()
                    startActivity(Intent(context, passwordActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                    // TODO: ارسال SMS انجام خواهد شد اینجا
                }
            }
        }

        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        if (shakeSwitch.isChecked) startShakeDetection()
    }

    private fun startShakeDetection() {
        val SHAKE_THRESHOLD = 1.0f
        var lastAcceleration = SensorManager.GRAVITY_EARTH
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        shakeListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val x = it.values[0]
                    val y = it.values[1]
                    val z = it.values[2]

                    val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                    val delta = abs(acceleration - lastAcceleration)
                    if (delta > SHAKE_THRESHOLD && shakeSwitch.isChecked) {
                        setMaxVolume()
                        SoundManager.playAlarm(this@MainActivity)
                        sendLocationSms()
                        startActivity(Intent(this@MainActivity, passwordActivity::class.java))
                        // TODO: ارسال SMS انجام خواهد شد اینجا
                    }

                    lastAcceleration = acceleration
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            shakeListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    private fun stopShakeDetection() {
        shakeListener?.let {
            sensorManager.unregisterListener(it)
        }
    }

    private fun setMaxVolume() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
        stopShakeDetection()
    }



    @SuppressLint("MissingPermission")
    private fun sendLocationSms() {
        val phoneNumber = sharedPreferences.getString("sos_phone_number", "")

        if (phoneNumber.isNullOrEmpty()) {
            Toast.makeText(this, "شماره‌ای ذخیره نشده", Toast.LENGTH_SHORT).show()
            Log.d("LocationSMS", "No phone number saved")
            return
        }

        val currentTime = System.currentTimeMillis()

        // فقط در اولین بار، SMS رو فوری بفرست
        if (firstSmsSent && currentTime - lastSmsTime < 60_000) {
            Log.d("LocationSMS", "SMS recently sent, skipping")
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    Log.d("LocationSMS", "Last location used: ${location.latitude}, ${location.longitude}")
                    sendSmsWithLocation(phoneNumber, location)
                    lastSmsTime = currentTime
                    firstSmsSent = true
                } else {
                    val cancellationTokenSource = CancellationTokenSource()
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.token
                    ).addOnSuccessListener { currentLocation ->
                        if (currentLocation != null) {
                            Log.d("LocationSMS", "Current location used: ${currentLocation.latitude}, ${currentLocation.longitude}")
                            sendSmsWithLocation(phoneNumber, currentLocation)
                            lastSmsTime = currentTime
                            firstSmsSent = true
                        } else {
                            Toast.makeText(this, "مکان یافت نشد", Toast.LENGTH_SHORT).show()
                            Log.d("LocationSMS", "getCurrentLocation() also returned null")
                        }
                    }.addOnFailureListener {
                        Log.e("LocationSMS", "Failed to get current location", it)
                    }
                }
            }.addOnFailureListener {
                Log.e("LocationSMS", "Failed to get last location", it)
            }
    }

    private fun sendSmsWithLocation(phoneNumber: String, location: Location) {
        val message = "مکان فعلی:\nLatitude: ${location.latitude}, Longitude: ${location.longitude}"

        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(this, "مکان ارسال شد", Toast.LENGTH_SHORT).show()
            Log.d("LocationSMS", "SMS sent to $phoneNumber with location")
        } catch (e: Exception) {
            Toast.makeText(this, "خطا در ارسال پیامک", Toast.LENGTH_SHORT).show()
            Log.e("LocationSMS", "SMS send failed", e)
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.SEND_SMS)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1)
        }
    }
}


