package com.example.h8fulalarmclock;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import java.util.Calendar;

public class EditReminderActivity extends AppCompatActivity {

    private EditText reminderMessage;
    private Button setDateButton;
    private Button setTimeButton;
    private Button saveButton;
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_reminder);

        reminderMessage = findViewById(R.id.reminder_message);
        setDateButton = findViewById(R.id.set_date);
        setTimeButton = findViewById(R.id.set_time);
        saveButton = findViewById(R.id.save_reminder);

        calendar = Calendar.getInstance();

        // If the activity was started with a reminder ID, load the reminder data from the database
        int reminderId = getIntent().getIntExtra("REMINDER_ID", -1);
        if (reminderId != -1) {
            // Fetch the reminder from the database in a background thread
            new Thread(() -> {
                AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "reminders").build();
                ReminderEntity reminder = db.reminderDao().getById(reminderId);

                // Populate the UI with the reminder data in the main thread
                runOnUiThread(() -> {
                    String[] timeParts = reminder.time.split(":");
                    calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
                    calendar.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));

                    String[] dateParts = reminder.date.split("/");
                    calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateParts[0]));
                    calendar.set(Calendar.MONTH, Integer.parseInt(dateParts[1]));
                    calendar.set(Calendar.YEAR, Integer.parseInt(dateParts[2]));


                    reminderMessage.setText(reminder.getMessage());
                });
            }).start();
        }

        setDateButton.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(EditReminderActivity.this, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                }
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        setTimeButton.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(EditReminderActivity.this, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                }
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
            timePickerDialog.show();
        });

        saveButton.setOnClickListener(v -> {
            String message = reminderMessage.getText().toString();
            String date = calendar.get(Calendar.DAY_OF_MONTH) + "/" + calendar.get(Calendar.MONTH) + "/" + calendar.get(Calendar.YEAR);
            String time = calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE);

            // Create a reminder object with the data
            ReminderEntity reminder = new ReminderEntity(date, time, message, true);

            // Save the reminder to the database in a background thread
            new Thread(() -> {
                AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "reminders").build();

                if (reminderId != -1) {
                    reminder.id = reminderId;
                    Log.d("EditReminderActivity", "Updating reminder with ID: " + reminder.id);
                    db.reminderDao().update(reminder);

                }
                else{
                    //Get a new reminder id different from -1 and from the ones already in the database
                    int newId = 0;
                    while(db.reminderDao().getById(newId) != null || newId == -1){
                        newId++;
                    }
                    reminder.id = newId;
                    db.reminderDao().insert(reminder);
                    Log.d("EditReminderActivity", "Creating new reminder with ID: " + reminder.id);
                }
                reminder.scheduleReminder(EditReminderActivity.this);
            }).start();

            Intent intent = new Intent(EditReminderActivity.this, RemindersActivity.class);
            startActivity(intent);
        });
    }
}