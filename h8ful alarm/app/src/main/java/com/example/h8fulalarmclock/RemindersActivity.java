package com.example.h8fulalarmclock;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;

import androidx.room.Room;

import java.util.List;

public class RemindersActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        ListView remindersList = findViewById(R.id.reminders_list);

        // Get a database instance
        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "reminders").build();

        // Get the reminders from the database in a background thread
        new Thread(() -> {
            List<ReminderEntity> reminders = db.reminderDao().getAll();

            // Update the ListView in the main thread
            runOnUiThread(() -> {
                ListReminderAdapter adapter = new ListReminderAdapter(this, reminders, db);
                remindersList.setAdapter(adapter);
            });
        }).start();

        remindersList.setOnItemClickListener((parent, view, position, id) -> {
            ReminderEntity clickedReminder = (ReminderEntity) parent.getItemAtPosition(position);
            Intent intent = new Intent(RemindersActivity.this, EditReminderActivity.class);
            intent.putExtra("REMINDER_ID", clickedReminder.getId());
            startActivity(intent);
        });

        Button addReminderButton = findViewById(R.id.add_reminder);
        addReminderButton.setOnClickListener(v -> {
            Intent intent = new Intent(RemindersActivity.this, EditReminderActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(RemindersActivity.this, MainActivity.class);
        startActivity(intent);
    }
}