package com.dwett.habits;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && context != null) {
            if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
                // Set the alarm here.
                NotificationScheduler.scheduleAlarm(context, AlarmReceiver.class);
                return;
            }
        }

        // Display the notification
        NotificationScheduler.showReminder(context, MainActivity.class);
    }
}
