package com.example.h8fulalarmclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import androidx.core.app.NotificationCompat;
import androidx.room.Room;

public class ReminderReceiver extends BroadcastReceiver {
    String reminderText = "reminder text";
    @Override
    public void onReceive(Context context, Intent intent) {
        int reminderId = intent.getIntExtra("REMINDER_ID", 0);

        // Create an explicit intent for an Activity in your app
        Intent notificationIntent = new Intent(context, RemindersActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        //Get reminder text from database

        if (reminderId != -1) {
            AppDatabase db = Room.databaseBuilder(context, AppDatabase.class, "reminders").build();
            new Thread(() -> {
                ReminderEntity reminder = db.reminderDao().getById(reminderId);
;
                if (reminder != null) {
                    reminderText = reminder.message;
                }
            }).start();
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "reminderChannel")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Reminder")
                .setContentText(reminderText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(reminderId, builder.build());
    }
}