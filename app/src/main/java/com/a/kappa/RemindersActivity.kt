package com.a.kappa

//ReminderActivity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RemindersActivity : AppCompatActivity() {


    private var calendarId = UserPrefs.getID()

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
    }

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
}
