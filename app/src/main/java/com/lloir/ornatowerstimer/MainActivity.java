package com.lloir.ornatowerstimer;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "floors_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final int MAX_TOWERS = 10;
    private Handler handler;
    private Runnable floorsUpdateRunnable;
    private List<Tower> towers;
    private int floors;

    private EditText floorInputEditText;
    private TextView floorsTextView;
    private Spinner towerSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();

        handler = new Handler(Looper.getMainLooper());
        floorsUpdateRunnable = this::updateFloors;
        towers = new ArrayList<>();

        floorInputEditText = findViewById(R.id.floorInputEditText);
        floorsTextView = findViewById(R.id.floorsTextView);
        towerSpinner = findViewById(R.id.towerSpinner);

        findViewById(R.id.calculateButton).setOnClickListener(v -> {
            String inputFloorText = floorInputEditText.getText().toString();
            int inputFloor = Integer.parseInt(inputFloorText);
            Tower tower = new Tower(inputFloor);
            if (towers.size() >= MAX_TOWERS) {
                towers.remove(0); // Remove the oldest tower
            }
            towers.add(tower);
            updateFloorsTextView();
        });

        findViewById(R.id.removeButton).setOnClickListener(v -> {
            int selectedTowerIndex = towerSpinner.getSelectedItemPosition();
            if (selectedTowerIndex >= 0 && selectedTowerIndex < towers.size()) {
                towers.remove(selectedTowerIndex);
                updateFloorsTextView();
            }
        });

        populateTowerSpinner();
        towerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedTowerIndex = towerSpinner.getSelectedItemPosition();
                if (selectedTowerIndex >= 0 && selectedTowerIndex < towers.size()) {
                    floors = towers.get(selectedTowerIndex).getCurrentFloor();
                    updateFloorsTextView();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        startFloorsUpdateTimer();
    }

    private void startFloorsUpdateTimer() {
        handler.postDelayed(floorsUpdateRunnable, FLOORS_UPDATE_INTERVAL);
    }

    private void updateFloors() {
        floors++;
        if (floors >= FLOORS_RESET_VALUE) {
            floors = INITIAL_FLOORS;
            showNotification();
        }

        updateFloorsTextView();

        startFloorsUpdateTimer();
    }

    private void showNotification() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NOTIFICATION_POLICY) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, handle the case
            // You can request the permission from the user here
            // For example, you can show a dialog or request the permission using a permission request flow
            return;
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Tower Reached " + FLOORS_RESET_VALUE + " Floors")
                .setContentText("The tower has reached " + FLOORS_RESET_VALUE + " floors. Resetting to " + INITIAL_FLOORS + " floors.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String name = "Floors Channel";
            String descriptionText = "Notification channel for floors";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(descriptionText);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void updateFloorsTextView() {
        floorsTextView.setText(String.valueOf(floors));
    }

    private void populateTowerSpinner() {
        List<String> towerNames = new ArrayList<>();
        for (int i = 0; i < towers.size(); i++) {
            towerNames.add("Tower " + (i + 1));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, towerNames);
        towerSpinner.setAdapter(adapter);
    }

    private static final int INITIAL_FLOORS = 15;
    private static final int FLOORS_RESET_VALUE = 50;
    private static final long FLOORS_UPDATE_INTERVAL = 5 * 1000L; // 5 seconds in milliseconds

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(floorsUpdateRunnable);
    }

    private static class Tower {
        private int currentFloor;

        public Tower(int currentFloor) {
            this.currentFloor = currentFloor;
        }

        public int getCurrentFloor() {
            return currentFloor;
        }
    }
}
