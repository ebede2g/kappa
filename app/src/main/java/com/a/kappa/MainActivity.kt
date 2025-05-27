package com.a.kappa

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.a.kappa.PermissionHelper.checkAndRequestCalendarPermission
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.TimeZone


class MainActivity : AppCompatActivity() {
    private val PERMISSION_REQUEST_CODE = 1001

    private lateinit var statusText: TextView

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (!grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }){
                Toast.makeText(this, "Надайте дозвіл на викорсиатння календаря", Toast.LENGTH_LONG).show()
            }
            else{
                Toast.makeText(this, "Дозвіл надано!", Toast.LENGTH_LONG).show()
                statusText.setText( UserPrefs.getStatus())
            }
        }
    }

    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestCalendarPermission(this, PERMISSION_REQUEST_CODE)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        statusText = findViewById(R.id.status)
        statusText.setText(if (hasPermission(this, Manifest.permission.WRITE_CALENDAR)) UserPrefs.getStatus() else UserPrefs.getStatus()+"\n> Надайте дозвіл на викорсиатння календаря")

        findViewById<Button>(R.id.goToSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }


    }


}