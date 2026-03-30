package com.weird.ringtonekeeper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "RingtoneKeeper";
    private static final String PREFS = "RingtoneKeeperPrefs";
    private static final int ALARM_REQUEST_CODE = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean wasActive = prefs.getBoolean("alarm_active", false);

        if (!wasActive) {
            Log.i(TAG, "Guardian was not active before reboot, not restarting.");
            return;
        }

        // Re-schedule the alarm after reboot
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(context, RingtoneResetReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, ALARM_REQUEST_CODE, alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long intervalMillis = 60 * 60 * 1000L; // 1 hour
        long triggerAt = System.currentTimeMillis() + intervalMillis;

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerAt, intervalMillis, pendingIntent);
        Log.i(TAG, "Guardian restarted after boot.");
    }
}
