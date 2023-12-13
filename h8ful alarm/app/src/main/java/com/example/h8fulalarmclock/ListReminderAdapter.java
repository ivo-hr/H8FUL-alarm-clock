package com.example.h8fulalarmclock;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

public class ListReminderAdapter extends BaseAdapter {
    private Context context;
    private List<ReminderEntity> reminders;
    private AppDatabase db;

    public ListReminderAdapter(Context context, List<ReminderEntity> reminders, AppDatabase db) {
        this.context = context;
        this.reminders = reminders;
        this.db = db;
    }

    @Override
    public int getCount() {
        return reminders.size();
    }

    @Override
    public Object getItem(int position) {
        return reminders.get(position);
    }

    @Override
    public long getItemId(int position) {
        return reminders.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_reminder, parent, false);
        }

        TextView reminderText = convertView.findViewById(R.id.reminder_text);
        reminderText.setText(reminders.get(position).date + " " + reminders.get(position).time + "\n" + reminders.get(position).message);
        //When reminder is clicked, open EditReminderActivity
        reminderText.setOnClickListener(v -> {
            ReminderEntity clickedReminder = reminders.get(position);
            Intent intent = new Intent(context, EditReminderActivity.class);
            intent.putExtra("REMINDER_ID", clickedReminder.getId());
            context.startActivity(intent);
        });

        ImageView deleteReminder = convertView.findViewById(R.id.delete_reminder);
        deleteReminder.setOnClickListener(v -> {
            ReminderEntity reminderToDelete = reminders.get(position);
            reminderToDelete.cancelReminder(context);
            reminders.remove(position);
            notifyDataSetChanged();

            new Thread(() -> {
                db.reminderDao().delete(reminderToDelete);
            }).start();
        });

        Switch reminderSwitch = convertView.findViewById(R.id.reminder_switch);
        reminderSwitch.setChecked(reminders.get(position).isEnabled);

        reminderSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ReminderEntity reminderToUpdate = reminders.get(position);
            reminderToUpdate.setEnabled(isChecked);

            new Thread(() -> {
                db.reminderDao().update(reminderToUpdate);
            }).start();
        });

        return convertView;
    }
}