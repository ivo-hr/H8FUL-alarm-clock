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

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
        // Get the next alarm from the database
        new Thread(() -> {
            AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                    AppDatabase.class, "alarms").build();
            List<AlarmEntity> activeAlarms = db.alarmDao().getActiveAlarms();
            AlarmEntity nextAlarm = null;
            LocalDateTime now = LocalDateTime.now();
            int today = now.getDayOfWeek().getValue() + 1;
            if (today == 8)
                today = 1;

            for (AlarmEntity alarm : activeAlarms) {
                // Check if the alarm is active today or tomorrow
                if (alarm.days.charAt(today-1) == '1' || alarm.days.charAt(today) == '1') {
                    // Parse the time from the alarm
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                    if (alarm.time.length() < 5) {
                        if (alarm.time.charAt(0) == ':')
                            alarm.time = "00" + alarm.time;
                        else if (alarm.time.charAt(1) == ':')
                            alarm.time = "0" + alarm.time;
                        while (alarm.time.length() < 5)
                            alarm.time = alarm.time.substring(0, 3) + "0" + alarm.time.substring(3);
                    }
                    LocalTime alTim = LocalTime.parse(alarm.time, formatter);
                    // Check if the alarm is later than the current time and (if set) the next alarm
                    if (alTim.isAfter(now.toLocalTime()) && (nextAlarm == null || LocalTime.parse(nextAlarm.time, formatter).isAfter(alTim))) {
                        nextAlarm = alarm;
                    }
                }
            }
            TextView alarmComment = (TextView) findViewById(R.id.alarm_text);
            if (nextAlarm != null) {
                // Update the alarmTime TextView on the main thread
                AlarmEntity finalNextAlarm = nextAlarm;
                runOnUiThread(() -> {
                    alarmTime.setText(finalNextAlarm.time);
                    alarmComment.setText("your next\nalarm is at");
                });
            }
            else {
                // Update the alarmTime TextView on the main thread
                runOnUiThread(() -> {
                    alarmTime.setText("");
                    alarmComment.setText("no alarms\nfor today!");
                });
            }
        }).start();

        //Set reminder text and datetime to textview
        TextView reminderText = (TextView) findViewById(R.id.next_reminder);
        TextView reminderTime = (TextView) findViewById(R.id.reminder_time);
        // Get the next reminder from the database
        new Thread(() -> {
            AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                    AppDatabase.class, "reminders").build();
            List<ReminderEntity> activeReminders = db.reminderDao().getActiveReminders();
            ReminderEntity nextRem = null;
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy HH:mm");
            for (ReminderEntity reminder : activeReminders) {
                // Parse the date and time from the reminder
                if (reminder.time.length() < 5) {
                    if (reminder.time.charAt(0) == ':')
                        reminder.time = "00" + reminder.time;
                    else if (reminder.time.charAt(1) == ':')
                        reminder.time = "0" + reminder.time;
                    while (reminder.time.length() < 5)
                        reminder.time = reminder.time.substring(0, 3) + "0" + reminder.time.substring(3);
                }
                LocalDateTime reminderDateTime = LocalDateTime.parse(reminder.date + " " + reminder.time, formatter);
                // Check if the reminder is later than the current time and (if set) the next reminder
                if (reminderDateTime.isAfter(now) && (nextRem == null || LocalDateTime.parse(nextRem.date + " " + nextRem.time, formatter).isAfter(reminderDateTime))) {
                    nextRem = reminder;
                }
            }
            TextView reminderComment = (TextView) findViewById(R.id.reminder_text);
            if (nextRem != null) {
                // Update the reminderText and reminderTime TextViews on the main thread
                ReminderEntity finalNextRem = nextRem;
                runOnUiThread(() -> {
                    reminderText.setText(finalNextRem.message);
                    reminderTime.setText(finalNextRem.date + " " + finalNextRem.time);
                    reminderComment.setText("remember to");
                });
            }
            else {
                // Update the reminderText and reminderTime TextViews on the main thread
                runOnUiThread(() -> {
                    reminderText.setText("");
                    reminderTime.setText("");

                    reminderComment.setText("no reminders coming up!");
                });
            }
        }).start();



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
