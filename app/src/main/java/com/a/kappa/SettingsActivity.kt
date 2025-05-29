package com.a.kappa

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody

import java.io.IOException
import kotlin.concurrent.thread

class SettingsActivity : AppCompatActivity() {
    private val CALENDAR_PERMISSION_REQUEST_CODE = 101

    fun fetchAndSendFcmToken() {
        var token = ""
        Log.d("TASK", "запитую токен")
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("TASK", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            token = task.result
            Log.d("TASK", "FCM Token: $token")
            Log.d("TASK", "ЗМІНЕНО!")
            UserPrefs.setToken(token)

            val client = OkHttpClient()
            val json = """
            {
              "fcm_token": "$token"
            }
        """.trimIndent()

            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("http://"+ UserPrefs.getIp()+":5000/register_token") // твій серверний ендпоінт
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("TASK", "Failed to send token to server", e)
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.d("TASK", "Token sent to server successfully")
                }
            })
        }

    }

    fun normalizeUrl(rawUrl: String): List<String> {
        var url = rawUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }

        if (!url.endsWith("/")) {
            url += "/"
        }

        val parts = url.split("/")
        val hostPort = if (parts.size > 2) parts[2] else ""
        val ip = hostPort.split(":")[0]
        val userSegment = if (parts.size > 3) parts[3] else ""
        val uidSegment = if (parts.size > 4) parts[4] else ""

        return listOf(ip, userSegment, uidSegment, url)
    }

    private fun checkAndRequestCalendarPermissions() {
        val readCalendarPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
        val writeCalendarPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR)

        if (readCalendarPermission != PackageManager.PERMISSION_GRANTED || writeCalendarPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
                CALENDAR_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CALENDAR_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Дозволи на доступ до календаря надано.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Доступи до календаря не надано. Функціонал може бути обмежений.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getOrCreateCalendarId(context: Context, calName: String): Long? {
        if (calName.isBlank()) {
            runOnUiThread {
                Toast.makeText(context, "Помилка: Ім'я для календаря не може бути порожнім.", Toast.LENGTH_LONG).show()
            }
            return null
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            runOnUiThread {
                Toast.makeText(context, "Потрібні дозволи для доступу до календаря.", Toast.LENGTH_LONG).show()
            }
            checkAndRequestCalendarPermissions()
            return null
        }

        val projection = arrayOf(CalendarContract.Calendars._ID)
        val selection = "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = ?"
        val selectionArgs = arrayOf(calName, CalendarContract.ACCOUNT_TYPE_LOCAL)

        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
                }
            }
        } catch (e: Exception) {
            return null
        }

        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, context.packageName)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, calName)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, calName)
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0x0077FF)
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, context.packageName)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
        }

        val insertUri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, context.packageName)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()

        return try {
            val newCalendarUri = context.contentResolver.insert(insertUri, values)
            newCalendarUri?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun initializeCalendar() {
        val url = UserPrefs.getUrl()
        val userName = UserPrefs.getUserName()
        val pwd = UserPrefs.getPwd()

        if (url.isBlank() || userName.isBlank() || pwd.isBlank()) {
            UserPrefs.setID(-1L)
            UserPrefs.setUID("")
            runOnUiThread {
                UserPrefs.setStatus("Севрер не знайдено\nПоля порожні")
                Toast.makeText(this, "URL, ім'я користувача або пароль не заповнені. Налаштування календаря скинуто.", Toast.LENGTH_LONG).show()
                UserPrefs.setToken("-")
                findViewById<TextView>(R.id.pp3).text = UserPrefs.getUID()
                findViewById<TextView>(R.id.pp4).text = UserPrefs.getID().toString()
            }
            return
        }

        if (!ChekUtil.isCalDAVCredentialsValid(url, userName, pwd)) {
            UserPrefs.setID(-1L)
            UserPrefs.setUID("")
            runOnUiThread {
                UserPrefs.setStatus("Севрер не знайдено\n--------\nCalDAV: \nхибний URL і/або пароль")
                Toast.makeText(this, "CalDAV: невірна URL, ім'я користувача або пароль. Налаштування календаря скинуто.", Toast.LENGTH_LONG).show()
                UserPrefs.setToken("-")
                findViewById<TextView>(R.id.pp3).text = UserPrefs.getUID()
                findViewById<TextView>(R.id.pp4).text = UserPrefs.getID().toString()
            }
            return
        }

        runOnUiThread {
            UserPrefs.setStatus("Вітаю\n<Онлайн режим>\nВсе готово до роботи!")
            Toast.makeText(this, "CalDAV: URL та облікові дані правильні.", Toast.LENGTH_SHORT).show()
            fetchAndSendFcmToken()
            //Запит токену FCM , draw it onto UserPrefs.setToken()
        }

        val calendarNameToCreate = UserPrefs.getUID()

        if (calendarNameToCreate.isNullOrBlank()) {
            UserPrefs.setID(-1L)
            runOnUiThread {
                Toast.makeText(this, "Не вказано ім'я для створення локального календаря (UID). ID календаря скинуто.", Toast.LENGTH_LONG).show()
                findViewById<TextView>(R.id.pp4).text = UserPrefs.getID().toString()
                UserPrefs.setToken("-")
            }
            return
        }

        val typeLongID = getOrCreateCalendarId(this, calendarNameToCreate)

        if (typeLongID != null) {
            UserPrefs.setID(typeLongID)
            runOnUiThread {
                findViewById<TextView>(R.id.pp4).text = typeLongID.toString()
                Toast.makeText(this, "ID календаря ($typeLongID) для '$calendarNameToCreate' збережено.", Toast.LENGTH_SHORT).show()
            }
        } else {
            UserPrefs.setID(-1L)
            runOnUiThread {
                findViewById<TextView>(R.id.pp4).text = UserPrefs.getID().toString()
                Toast.makeText(this, "Помилка: не вдалося створити/знайти календар '$calendarNameToCreate'. ID скинуто.", Toast.LENGTH_LONG).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        checkAndRequestCalendarPermissions()

        val switchOffline = findViewById<Switch>(R.id.switchUseOffline)
        switchOffline.isChecked = UserPrefs.getIs_offline()

        val urlField = findViewById<EditText>(R.id.CalendarFiled)
        val pwdField = findViewById<EditText>(R.id.PasswordfFiled)

        urlField.setText(UserPrefs.getUrl())
        pwdField.setText(UserPrefs.getPwd())
        urlField.isEnabled = !switchOffline.isChecked
        pwdField.isEnabled = !switchOffline.isChecked


        findViewById<TextView>(R.id.status).text = UserPrefs.getStatus()
        findViewById<TextView>(R.id.pp1).text = UserPrefs.getIp()
        findViewById<TextView>(R.id.pp2).text = UserPrefs.getUserName()
        findViewById<TextView>(R.id.pp3).text = UserPrefs.getUID()
        findViewById<TextView>(R.id.pp4).text = UserPrefs.getID().toString()
        findViewById<TextView>(R.id.pp5).text = UserPrefs.getToken()


        switchOffline.setOnCheckedChangeListener { _, isChecked ->
            urlField.isEnabled = !isChecked
            pwdField.isEnabled = !isChecked
            UserPrefs.setIs_offline(isChecked)
            UserPrefs.setToken("")
        }


        val goTomainScreen = Intent(this, MainActivity::class.java)
        val btnGoToMain = findViewById<Button>(R.id.btnGoToMain)
        btnGoToMain.setOnClickListener {

            if (switchOffline.isChecked){
                //оффлайн режим включено
                UserPrefs.setStatus("Вітаю\n<Офлайн режим>\nВсе готово до роботи!")
                val offlineCalName=applicationContext.applicationInfo.loadLabel(packageManager).toString()+"DAV_Offline"
                val offlineCalId = getOrCreateCalendarId(this, offlineCalName)
                UserPrefs.setIp("")
                UserPrefs.setUserName("")
                if (offlineCalId != null) {
                    UserPrefs.setID(offlineCalId)
                }
                UserPrefs.setToken("")
                UserPrefs.setUID(offlineCalName)
                startActivity(goTomainScreen)
                finish()
            }
            else{
                val rawUrlInput = urlField.text.toString()
                val passwordInput = pwdField.text.toString()
                val (ip, nameFromUrl, uidFromUrl, normalizedUrl) = normalizeUrl(rawUrlInput)

                UserPrefs.setPwd(passwordInput)
                UserPrefs.setUrl(normalizedUrl)
                UserPrefs.setIp(ip)
                UserPrefs.setUserName(nameFromUrl)
                UserPrefs.setUID(uidFromUrl)

                thread {
                    initializeCalendar()
                    runOnUiThread {
                        startActivity(goTomainScreen)
                        finish()
                    }
                }
            }


        }






    }
}