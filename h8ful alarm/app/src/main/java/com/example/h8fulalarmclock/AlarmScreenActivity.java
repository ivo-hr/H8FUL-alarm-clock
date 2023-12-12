package com.example.h8fulalarmclock;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

public class AlarmScreenActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_screen);

        Button stopAlarmButton = findViewById(R.id.stop_alarm);

        // Get the alarm ID from the Intent
        int alarmId = getIntent().getIntExtra("ALARM_ID", -1);

        // Fetch the alarm from the database in a background thread
        new Thread(() -> {
            AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                    AppDatabase.class, "database-name").build();
            AlarmEntity alarm = db.alarmDao().getById(alarmId);
            if (alarm == null) Log.d("AlarmScreenActivity", "Alarm is null ");
            // Animate the button in the main thread
            runOnUiThread(() -> {
                // Calculate the animation duration based on the speed
                long duration = 750;
                if (alarm != null) duration = 1000 / alarm.getSpeed();
                // Get the size of the screen
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int screenWidth = displayMetrics.widthPixels;
                int screenHeight = displayMetrics.heightPixels;

                // Get the size of the button
                int buttonWidth = stopAlarmButton.getWidth();
                int buttonHeight = stopAlarmButton.getHeight();

                // Calculate the range of possible X and Y coordinates for the button
                int minX = 0;
                int maxX = screenWidth - buttonWidth;
                int minY = 0;
                int maxY = screenHeight - buttonHeight;
                //Randomize the min and max values without going out of screen
                minX = (int) (Math.random() * (maxX - minX - 400) + minX);
                minY = (int) (Math.random() * (maxY - minY - 400) + minY);
                maxX = (int) (Math.random() * (maxX - minX + 400) + minX);
                maxY = (int) (Math.random() * (maxY - minY + 400) + minY);
                //The shorter the range the faster the button moves
                int rangeX = maxX - minX;
                int rangeY = maxY - minY;
                long alarmSpeed = duration/(rangeX * rangeY);

                // Create an ObjectAnimator for the X coordinate
                ObjectAnimator xAnimator = ObjectAnimator.ofFloat(stopAlarmButton, "x", minX, maxX);
                xAnimator.setDuration(alarmSpeed);
                xAnimator.setRepeatMode(ValueAnimator.REVERSE);
                xAnimator.setRepeatCount(ValueAnimator.INFINITE);

                // Create an ObjectAnimator for the Y coordinate
                ObjectAnimator yAnimator = ObjectAnimator.ofFloat(stopAlarmButton, "y", minY, maxY);
                yAnimator.setDuration(alarmSpeed);
                yAnimator.setRepeatMode(ValueAnimator.REVERSE);
                yAnimator.setRepeatCount(ValueAnimator.INFINITE);

                // Start the animations
                xAnimator.start();
                yAnimator.start();

            });
        }).start();

        stopAlarmButton.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(this, AlarmService.class);
            stopService(serviceIntent);

            //Close the alarm screen
            finish();
        });

        //Set current time in 24h to textview
        TextView currentTime = findViewById(R.id.current_alarmTime);

        //Update date and time every minute
        final Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                currentTime.setText(java.time.LocalTime.now().toString());
                handler.postDelayed(this, 60000); // 60 seconds delay
            }
        };

        handler.post(runnable); // start the updates

        Button snoozeButton = findViewById(R.id.snooze_button);
        snoozeButton.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(this, AlarmService.class);
            stopService(serviceIntent);

            //int alarmId = getIntent().getIntExtra("ALARM_ID", -1);
            if (alarmId != -1) {
                new Thread(() -> {
                    AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                            AppDatabase.class, "database-name").build();
                    AlarmEntity alarm = db.alarmDao().getById(alarmId);
                    if (alarm != null && alarm.isEnabled()) {
                        // Snooze for 1-10 minutes
                        int snoozeTime = (int) (Math.random() * 10 + 1);
                        //Generate new time string adding snooze time to current time in 00:00 format
                        String newTime = java.time.LocalTime.now().plusMinutes(snoozeTime).toString();
                        alarm.time = newTime.substring(0, newTime.length() - 3);
                        db.alarmDao().update(alarm);
                        alarm.scheduleAlarm(AlarmScreenActivity.this);
                    }
                }).start();
            }

            //Close the alarm screen
            finish();
        });
    }
}