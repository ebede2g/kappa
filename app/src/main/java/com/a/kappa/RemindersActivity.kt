package com.a.kappa

//ReminderActivity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.app.AlertDialog

class RemindersActivity : AppCompatActivity() {


    private fun refreshEvents() {
        val events = getEventsFromCalendar(this, calendarId)
        val listView = findViewById<ListView>(R.id.reminderList)
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_1, events)
        listView.adapter = adapter
    }

    fun getEventsFromCalendar(context: Context, calendarId: Long): List<String> {
        val events = mutableListOf<String>()

        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART
        )

        val selection = "${CalendarContract.Events.CALENDAR_ID} = ?"
        val selectionArgs = arrayOf(calendarId.toString())

        val cursor = context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Events.DTSTART} ASC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val title = it.getString(0) ?: "Без назви"
                val startMillis = it.getLong(1)
                val date = android.text.format.DateFormat.format("[dd.MM.yyyy] HH:mm", startMillis)
                events.add("$date : $title")
            }
        }

        return events
    }


    private var calendarId = UserPrefs.getID()




    fun getLocalTaskNames(context: Context, calendarId: Long): List<String> {
        val events = mutableListOf<String>()

        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART
        )

        val selection = "${CalendarContract.Events.CALENDAR_ID} = ?"
        val selectionArgs = arrayOf(calendarId.toString())

        val cursor = context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            val titleIndex = it.getColumnIndex(CalendarContract.Events.TITLE)
            val dtstartIndex = it.getColumnIndex(CalendarContract.Events.DTSTART)

            while (it.moveToNext()) {
                val title = it.getString(titleIndex) ?: "No title"
                val dtstartMillis = it.getLong(dtstartIndex)
                val dtstartStr = TaskUtil.convertMillisToLocalDateTimeString(dtstartMillis)
                events.add(title+dtstartStr)
            }
        }

        return events
    }




    fun syncClinetToServer(context: Context, onDone: () -> Unit) {
        Log.d("TASK", "початок обробки списків...")

        val localEvents = getLocalTaskNames(context, UserPrefs.getID()).toSet()

        TaskUtil.fetchIcsEventSummaries { remoteEventsList ->
            if (remoteEventsList != null) {
                val remoteEvents = remoteEventsList.toSet()

                Log.d("TASK", "==== Local Events ====")
                localEvents.forEach { Log.d("TASK", it) }

                Log.d("TASK", "==== Remote Events ====")
                remoteEvents.forEach { Log.d("TASK", it) }

                val missingEvents = remoteEvents.filter { it !in localEvents }

                Log.d("TASK", "==== Отже, клієнту бракує... ====")
                if (missingEvents.isEmpty()) {
                    Log.d("TASK", "Бракує подій немає")
                    onDone()
                } else {
                    missingEvents.forEach { Log.d("TASK", it) }
                    TaskUtil.AddLocalTaskFromCalDavList(context, missingEvents) {
                        // після додавання задач
                        onDone()
                    }
                }
            } else {
                Log.d("TASK", "Не вдалося отримати дані з CalDAV")
                onDone()
            }
        }
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminders)

        Toast.makeText(this, "id = $calendarId", Toast.LENGTH_SHORT).show()

        // Відображення подій
        refreshEvents()

        val mainScreen = Intent(this, MainActivity::class.java)
        val btnGoToMain = findViewById<Button>(R.id.goToMain)
        btnGoToMain.setOnClickListener {
            startActivity(mainScreen)
            finish()
        }

        val btnDeleteAll = findViewById<Button>(R.id.btnDeleteAll)
        btnDeleteAll.setOnClickListener {
            val rowsDeleted = contentResolver.delete(
                CalendarContract.Events.CONTENT_URI,
                "${CalendarContract.Events.CALENDAR_ID} = ?",
                arrayOf(calendarId.toString())
            )
            if (rowsDeleted > 0) {
                Toast.makeText(this, "Видалено $rowsDeleted подій", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Події не знайдено або не видалено", Toast.LENGTH_SHORT).show()
            }
            refreshEvents()
        }


        val btnSync = findViewById<Button>(R.id.btnToSync)
        if (UserPrefs.getIs_offline() || UserPrefs.getUID()==""){
            btnSync.isEnabled = false
        }

        btnSync.setOnClickListener {
            btnSync.isEnabled = false
            Log.d("TASK", "Спроба синхронізації")

            ChekUtil.isOnline(this) { online ->
                if (online) {
                    Log.d("TASK", "все онлайн !")

                    syncClinetToServer(this) {
                        runOnUiThread {
                            refreshEvents()
                            Log.d("TASK", "синхронізовано! ок.")
                            btnSync.isEnabled = true
                        }
                    }

                } else {
                    Log.d("TASK", "Не вдалося Sync з CalDAV")
                    Toast.makeText(this, "Неможливо синхронізуватись. Не підключено сервер або ви сервер поза досяжністю", Toast.LENGTH_LONG).show()
                    btnSync.isEnabled = true
                }
            }
        }



    }

}
