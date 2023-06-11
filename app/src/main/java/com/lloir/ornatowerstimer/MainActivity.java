package com.lloir.ornatowerstimer;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private Spinner towerSpinner;
    private TextView currentFloorTextView;
    private TextView hitTimeTextView;

    // Tower offsets
    private static final int SEL_ELE_OFFSET = 34;
    private static final int EOS_OFFSET = 29;
    private static final int OCE_OFFSET = 24;
    private static final int THE_OFFSET = 19;
    private static final int PRO_OFFSET = 14;

    // Notification channel constants
    private static final String CHANNEL_ID = "tower_notifications";
    private static final String CHANNEL_NAME = "Tower Notifications";
    private static final int NOTIFICATION_ID = 1;

    // Permission request code
    private static final int NOTIFICATION_POLICY_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        towerSpinner = findViewById(R.id.towerSpinner);
        currentFloorTextView = findViewById(R.id.currentFloorTextView);
        hitTimeTextView = findViewById(R.id.hitTimeTextView);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.tower_names, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        towerSpinner.setAdapter(adapter);

        towerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                calculateCurrentFloorAndHitTime(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        Button settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        });

        requestNotificationPermission();
    }

    private void calculateCurrentFloorAndHitTime(int position) {
        int offset = 0;

        switch (position) {
            case 0: // Oceanus
                offset = OCE_OFFSET;
                break;
            case 1: // Selene
                offset = SEL_ELE_OFFSET;
                break;
            case 2: // Eos
                offset = EOS_OFFSET;
                break;
            case 3: // Themis
                offset = THE_OFFSET;
                break;
            case 4: // Prometheus
                offset = PRO_OFFSET;
                break;
        }

        int currentFloor = getCurrentFloor(offset);
        String hitTime = calculateHitTime(currentFloor);

        currentFloorTextView.setText("Current Floor: " + currentFloor);
        hitTimeTextView.setText("Estimated Time of Arrival at Floor 50: " + hitTime);

        if (currentFloor == 50) {
            showNotification();
        }
    }

    private int getCurrentFloor(int offset) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentFloor = ((hour - 19) / 5) + 15 + offset;

        if (currentFloor > 50) {
            currentFloor = 15 + offset;
        }

        return currentFloor;
    }

    private String calculateHitTime(int currentFloor) {
        int remainingFloors = 50 - currentFloor;
        int remainingHours = remainingFloors * 5;
        int remainingDays = remainingHours / 24;
        remainingHours = remainingHours % 24;

        return remainingDays + "d " + remainingHours + "h";
    }

    private void openSettings() {
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showNotification() {
        // Create the notification channel (required for Android 8.0 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence channelName = CHANNEL_NAME;
            String channelId = CHANNEL_ID;
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // Build the notification
        String notificationTitle = getString(R.string.notification_title);
        String notificationText = getString(R.string.notification_text);
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Show the notification
        if (hasNotificationPermission()) {
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
            try {
                notificationManagerCompat.notify(NOTIFICATION_ID, builder.build());
            } catch (SecurityException e) {
                // Handle the SecurityException here
            }
        } else {
            requestNotificationPermission();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (!notificationManager.isNotificationPolicyAccessGranted()) {
                new AlertDialog.Builder(this)
                        .setTitle("Notification Permission")
                        .setMessage("Please grant permission to show notifications.")
                        .setPositiveButton("Grant", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            // Handle cancellation
                        })
                        .show();
            }
        }
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            return notificationManager.isNotificationPolicyAccessGranted();
        }
        return true; // If SDK version is below Oreo, assume permission is granted
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NOTIFICATION_POLICY_REQUEST_CODE) {
            if (hasNotificationPermission()) {
                // Notification policy access granted
            } else {
                // Notification policy access denied
            }
        }
    }
}
