package com.example.h8fulalarmclock;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
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

    private MediaPlayer mediaPlayer;
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
                    AppDatabase.class, "alarms").build();
            AlarmEntity alarm = db.alarmDao().getById(alarmId);
            if (alarm == null) Log.d("AlarmScreenActivity", "Alarm is null ");
            if (alarm != null) {
                try {
                    mediaPlayer = MediaPlayer.create(this, Uri.parse(alarm.getRingtone()));
                }
                catch (Exception e) {
                    mediaPlayer = MediaPlayer.create(this, R.raw.alarm);
                }
                mediaPlayer.start();
            }
            else {
                mediaPlayer = MediaPlayer.create(this, R.raw.alarm);
                mediaPlayer.start();
            }

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
                int xPos0 = 0;
                int xPos1 = screenWidth - buttonWidth;
                int yPos0 = 0;
                int yPos1 = screenHeight - buttonHeight;
                //Randomize the min and max values without going out of screen
                xPos0 = (int) (Math.random() * (xPos1 - xPos0 - 400) + xPos0);
                yPos0 = (int) (Math.random() * (yPos1 - yPos0 - 400) + yPos0);
                xPos1 = (int) (Math.random() * (xPos1 - xPos0 - 400) + xPos0);
                yPos1 = (int) (Math.random() * (yPos1 - yPos0 - 400) + yPos0);
                //Randomly switch the min and max values
                if (Math.random() > 0.5) {
                    int temp = xPos0;
                    xPos0 = xPos1;
                    xPos1 = temp;
                }
                if (Math.random() > 0.5) {
                    int temp = yPos0;
                    yPos0 = yPos1;
                    yPos1 = temp;
                }

                //The shorter the range the faster the button moves
                int rangeX = xPos1 - xPos0;
                int rangeY = yPos1 - yPos0;
                //Set the range to positive value
                if(rangeX < 0) rangeX = -rangeX;
                if(rangeY < 0) rangeY = -rangeY;
                //If the range is too short, set the positions at least 400px apart
                if(rangeX < 800) {
                    xPos0 -= 400 * rangeX / 400;
                    xPos1 += 400 * rangeX / 400;
                }
                if(rangeY < 800) {
                    yPos0 -= 400 * rangeY / 400;
                    yPos1 += 400 * rangeY / 400;
                }

                long alarmSpeed = rangeX*rangeY*2/duration;
                //Set alarmSpeed to positive value
                if(alarmSpeed < 0) alarmSpeed = -alarmSpeed;
                //Set minimum speed
                if(alarmSpeed < 100) alarmSpeed = 100;

                // Create an ObjectAnimator for the X coordinate
                ObjectAnimator xAnimator = ObjectAnimator.ofFloat(stopAlarmButton, "x", xPos0, xPos1);
                xAnimator.setDuration(alarmSpeed);
                xAnimator.setRepeatMode(ValueAnimator.REVERSE);
                xAnimator.setRepeatCount(ValueAnimator.INFINITE);

                // Create an ObjectAnimator for the Y coordinate
                ObjectAnimator yAnimator = ObjectAnimator.ofFloat(stopAlarmButton, "y", yPos0, yPos1);
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
            //Stop the alarm sound
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
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
                            AppDatabase.class, "alarms").build();
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
            //Stop the alarm sound
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
            //Close the alarm screen
            finish();
        });



    }

    //If back button is pressed, stop the alarm sound and close the alarm screen
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //Stop the alarm sound
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        Intent intent = new Intent(AlarmScreenActivity.this, MainActivity.class);
        startActivity(intent);
    }
}