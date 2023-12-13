package com.example.h8fulalarmclock;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import java.time.format.DateTimeFormatter;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Notification channel for reminders
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Reminder Channel";
            String description = "Channel for Reminder notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("reminderChannel", name, importance);
            channel.setDescription(description);
            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }


        //Set current time in 24h to textview
        TextView currentTime = (TextView) findViewById(R.id.current_time);
        currentTime.setText(java.time.LocalTime.now().toString());
        //Set current date to textview
        TextView currentDate = (TextView) findViewById(R.id.current_date);
        DateTimeFormatter wdmyFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy");
        String formattedDate = java.time.LocalDate.now().format(wdmyFormat);
        currentDate.setText(formattedDate);

        //Update date and time every minute
        final Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                currentTime.setText(java.time.LocalTime.now().toString());
                String formattedDate = java.time.LocalDate.now().format(wdmyFormat);
                currentDate.setText(formattedDate);
                handler.postDelayed(this, 60000); // 60 seconds delay
            }
        };

        handler.post(runnable); // start the updates


        //Set alarm time to textview
        TextView alarmTime = (TextView) findViewById(R.id.next_alarm);
        //TODO: Get alarm time from database
        alarmTime.setText("07:00");

        //Set reminder text to textview
        TextView reminderText = (TextView) findViewById(R.id.next_reminder);
        //TODO: Get reminder text from database
        reminderText.setText("take your medicine!");

        //Set reminder date/time to textview
        TextView reminderTime = (TextView) findViewById(R.id.reminder_time);
        //TODO: Get reminder time from database
        reminderTime.setText("12 dec 12:00");

        //BUTTONS
        Button alarmButton = (Button) findViewById(R.id.but_alarm);
        Button reminderButton = (Button) findViewById(R.id.but_reminder);
        Button testAlarmButton = (Button) findViewById(R.id.but_testAlarm);
        Button testReminderButton = (Button) findViewById(R.id.but_testReminder);

        //go to alarm activity
        alarmButton.setOnClickListener(v -> {
            //Go to alarm activity
            Intent intent = new Intent(this, AlarmsActivity.class);
            startActivity(intent);

        });

        //go to reminder activity
        reminderButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, RemindersActivity.class);
            startActivity(intent);
        });

        //Test the alarm
        testAlarmButton.setOnClickListener(v -> {
            //Create an alarm to test with
            AlarmEntity alarm = new AlarmEntity("12:00", "0000000", true, 1, "ringtone");
            //Add the alarm to the database
            new Thread(() -> {
                AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "alarms").build();
                //Get a new alarm id different from -1 and from the ones already in the database
                int newId = 0;
                while(db.alarmDao().getById(newId) != null || newId == -1){
                    newId++;
                }
                alarm.id = newId;
                db.alarmDao().insert(alarm);
            }).start();
            Intent intent = new Intent(this, AlarmScreenActivity.class);
            intent.putExtra("ALARM_ID", alarm.id);
            startActivity(intent);
        });

        //Test the reminder
        testReminderButton.setOnClickListener(v -> {
            //Create a reminder to test with
            ReminderEntity reminder = new ReminderEntity("12/12/2021", "12:00", "Test reminder", true);
            //Add the reminder to the database
            new Thread(() -> {
                AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "reminders").build();
                //Get a new reminder id different from -1 and from the ones already in the database
                int newId = 0;
                while(db.reminderDao().getById(newId) != null || newId == -1){
                    newId++;
                }
                reminder.id = newId;
                db.reminderDao().insert(reminder);
            }).start();

            //Send a broadcast to force a notification
            Intent intent = new Intent(MainActivity.this, ReminderReceiver.class);
            intent.putExtra("REMINDER_ID", reminder.id);
            sendBroadcast(intent);
        });

    }
}
