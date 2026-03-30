package com.weird.ringtonekeeper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "RingtoneKeeperPrefs";
    private static final String KEY_URI = "ringtone_uri";
    private static final int ALARM_REQUEST_CODE = 1001;

    private TextView statusText;
    private TextView selectedRingtoneText;
    private SharedPreferences prefs;

    private final ActivityResultLauncher<Intent> ringtonePicker =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                if (uri != null) {
                    prefs.edit().putString(KEY_URI, uri.toString()).apply();
                    updateSelectedRingtoneLabel(uri);
                    Toast.makeText(this, "Ringtone saved!", Toast.LENGTH_SHORT).show();
                }
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        statusText = findViewById(R.id.statusText);
        selectedRingtoneText = findViewById(R.id.selectedRingtoneText);

        // Restore previously saved ringtone label
        String savedUri = prefs.getString(KEY_URI, null);
        if (savedUri != null) {
            updateSelectedRingtoneLabel(Uri.parse(savedUri));
        }

        Button pickBtn = findViewById(R.id.pickRingtoneBtn);
        Button startBtn = findViewById(R.id.startServiceBtn);
        Button stopBtn = findViewById(R.id.stopServiceBtn);
        Button applyNowBtn = findViewById(R.id.applyNowBtn);

        pickBtn.setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
            String current = prefs.getString(KEY_URI, null);
            if (current != null) {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(current));
            }
            ringtonePicker.launch(intent);
        });

        applyNowBtn.setOnClickListener(v -> {
            if (applyRingtone()) {
                Toast.makeText(this, "✓ Ringtone applied right now!", Toast.LENGTH_SHORT).show();
            }
        });

        startBtn.setOnClickListener(v -> {
            if (prefs.getString(KEY_URI, null) == null) {
                Toast.makeText(this, "Pick a ringtone first!", Toast.LENGTH_SHORT).show();
                return;
            }
            scheduleAlarm();
            statusText.setText("Status: ● ACTIVE — runs every 1 hour");
            Toast.makeText(this, "Guardian started!", Toast.LENGTH_SHORT).show();
        });

        stopBtn.setOnClickListener(v -> {
            cancelAlarm();
            statusText.setText("Status: ○ STOPPED");
            Toast.makeText(this, "Guardian stopped.", Toast.LENGTH_SHORT).show();
        });

        // Show current alarm status
        if (isAlarmActive()) {
            statusText.setText("Status: ● ACTIVE — runs every 1 hour");
        } else {
            statusText.setText("Status: ○ STOPPED");
        }
    }

    private void updateSelectedRingtoneLabel(Uri uri) {
        try {
            android.media.Ringtone r = RingtoneManager.getRingtone(this, uri);
            String title = r != null ? r.getTitle(this) : uri.getLastPathSegment();
            selectedRingtoneText.setText("Selected: " + title);
        } catch (Exception e) {
            selectedRingtoneText.setText("Selected: " + uri.getLastPathSegment());
        }
    }

    public boolean applyRingtone() {
        String uriStr = prefs.getString(KEY_URI, null);
        if (uriStr == null) {
            Toast.makeText(this, "No ringtone selected yet.", Toast.LENGTH_SHORT).show();
            return false;
        }
        try {
            Uri uri = Uri.parse(uriStr);
            RingtoneManager.setActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE, uri);
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void scheduleAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, RingtoneResetReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        long intervalMillis = 60 * 60 * 1000L; // 1 hour
        long triggerAt = System.currentTimeMillis() + intervalMillis;

        // setRepeating works fine for 1h interval (Android restricts < 60s)
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerAt, intervalMillis, pendingIntent);
        prefs.edit().putBoolean("alarm_active", true).apply();
    }

    private void cancelAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, RingtoneResetReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
        prefs.edit().putBoolean("alarm_active", false).apply();
    }

    private boolean isAlarmActive() {
        return prefs.getBoolean("alarm_active", false);
    }
}
