package com.dwett.habits;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.NotificationCompat;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static android.content.Context.ALARM_SERVICE;

public class NotificationScheduler {

    public static final int REMINDER_REQUEST_CODE = 100;
    public static final String CHANNEL_ID = "HabitsChannel";

    public static void scheduleAlarm(Context context, Class<?> cls) {

        // Cancel any other scheduled alarms that we've made so far
        cancelAlarm(context, cls);

        // Enable a receiver
        ComponentName receiver = new ComponentName(context, cls);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        Intent intent1 = new Intent(context, cls);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                REMINDER_REQUEST_CODE,
                intent1,
                PendingIntent.FLAG_UPDATE_CURRENT);

        LocalDateTime nowTime = LocalDateTime.now();
        if (nowTime.getMinute() != 0) {
            nowTime = nowTime.plusMinutes(60 - nowTime.getMinute());
        }

        // TODO: Move this setting somewhere else
        // 11PM!
        while (nowTime.getHour() != 22) {
            nowTime = nowTime.plusHours(1);
        }

        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        am.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                nowTime.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
                AlarmManager.INTERVAL_DAY,
                pendingIntent);
    }

    public static void cancelAlarm(Context context, Class<?> cls) {
        // Disable a receiver
        ComponentName receiver = new ComponentName(context, cls);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(
                receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        Intent intent1 = new Intent(context, cls);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                REMINDER_REQUEST_CODE,
                intent1,
                PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        am.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    public static void showReminder(Context context, Class<?> cls) {
        Intent notificationIntent = new Intent(context, cls);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(cls);
        stackBuilder.addNextIntent(notificationIntent);

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(
                REMINDER_REQUEST_CODE,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context,
                CHANNEL_ID);
        Notification notification = builder
                .setContentTitle("Habits")
                .setContentText("Remember to update your habit status for the day!")
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher_round, 2)
                .setContentIntent(pendingIntent)
                .build();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        notificationManager.notify(REMINDER_REQUEST_CODE, notification);
    }
}
