package com.weird.ringtonekeeper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

public class RingtoneResetReceiver extends BroadcastReceiver {

    private static final String TAG = "RingtoneKeeper";
    private static final String PREFS = "RingtoneKeeperPrefs";
    private static final String KEY_URI = "ringtone_uri";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String uriStr = prefs.getString(KEY_URI, null);

        if (uriStr == null) {
            Log.w(TAG, "No ringtone URI saved, skipping.");
            return;
        }

        try {
            Uri uri = Uri.parse(uriStr);
            RingtoneManager.setActualDefaultRingtoneUri(
                context, RingtoneManager.TYPE_RINGTONE, uri
            );
            Log.i(TAG, "Ringtone reset to: " + uriStr);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set ringtone: " + e.getMessage());
        }
    }
}
