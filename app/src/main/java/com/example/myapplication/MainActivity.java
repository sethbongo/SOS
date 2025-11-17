package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SmsManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private double latitude = 0.0;
    private double longitude = 0.0;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private TextView statusText;
    private Spinner simSpinner;
    private List<Integer> subscriptionIds = new ArrayList<>();

    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Button btn = findViewById(R.id.button);
        EditText editTextNumber = findViewById(R.id.editTextNumber);
        statusText = findViewById(R.id.status);
        simSpinner = findViewById(R.id.simSpinner);

        setupLocationListener();
        setupSimSpinner();

        btn.setOnClickListener(v -> sendSosWithSelectedSim(editTextNumber.getText().toString().trim()));
    }

    private void setupSimSpinner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            loadSimCards();
        } else {
            statusText.setText("Need phone permission for SIM selection");
        }
    }

    @SuppressLint("MissingPermission")
    private void loadSimCards() {
        SubscriptionManager subscriptionManager = getSystemService(SubscriptionManager.class);
        List<SubscriptionInfo> subsInfoList = subscriptionManager.getActiveSubscriptionInfoList();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        subscriptionIds.clear();

        if (subsInfoList != null && !subsInfoList.isEmpty()) {
            for (SubscriptionInfo info : subsInfoList) {
                String carrier = info.getCarrierName().toString();
                String simSlot = "SIM " + (info.getSimSlotIndex() + 1);
                String display = simSlot + " - " + carrier + " (" + info.getNumber() + ")";
                if (info.getNumber() == null || info.getNumber().isEmpty()) {
                    display = simSlot + " - " + carrier;
                }
                adapter.add(display);
                subscriptionIds.add(info.getSubscriptionId());
            }
        } else {
            adapter.add("Default SIM (Single SIM)");
            subscriptionIds.add(-1); // Use default
        }

        simSpinner.setAdapter(adapter);
        statusText.setText("SIM loaded • Waiting for GPS...");
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestPermissionsIfNeeded();
    }

    private void requestPermissionsIfNeeded() {
        String[] permissions = {
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_PHONE_STATE
        };

        boolean allGranted = true;
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            startLocationUpdatesSafely();
            loadSimCards();
        } else {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allOk = true;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) allOk = false;
            }
            if (allOk) {
                startLocationUpdatesSafely();
                loadSimCards();
            } else {
                Toast.makeText(this, "All permissions required!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupLocationListener() {
        locationListener = location -> {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            runOnUiThread(() -> {
                statusText.setText("GPS READY • Tap SEND SOS");
                statusText.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            });
        };
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdatesSafely() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 5, locationListener);
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 5, locationListener);
            }
        } catch (Exception ignored) {}
    }

    private void sendSosWithSelectedSim(String input) {
        if (input.length() != 9 || !input.matches("\\d+")) {
            Toast.makeText(this, "Enter valid 9-digit number!", Toast.LENGTH_LONG).show();
            return;
        }

        String phoneNumber = "09" + input;
        int selectedPos = simSpinner.getSelectedItemPosition();
        int subscriptionId = subscriptionIds.get(selectedPos);

        String message = (latitude == 0.0 || longitude == 0.0)
                ? "EMERGENCY SOS!\nI need help NOW!\nGPS loading... Please call me!"
                : "EMERGENCY! PICK ME UP!\nhttps://maps.google.com/?q=" + latitude + "," + longitude +
                "\nLat: " + latitude + " | Long: " + longitude;

        try {
            SmsManager smsManager;
            if (subscriptionId == -1) {
                smsManager = SmsManager.getDefault();
            } else {
                smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId);
            }

            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "SOS sent via " + simSpinner.getSelectedItem().toString() + "!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send! Check SIM or balance.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }
}