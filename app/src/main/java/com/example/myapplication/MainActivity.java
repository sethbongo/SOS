package com.example.myapplication;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    double latitude = 0;
    double longitude = 0;
    LocationManager manager;
    private GPSReceiver receiver;

    Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        btn = findViewById(R.id.button);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SmsManager sms = SmsManager.getDefault();
                String phoneNumber = "xxxxxxxxxxxx";
                String messageBody = "Please take me from longitude: " + Double.toString(longitude) + " and latitude: " + Double.toString(latitude);
                try {
                    sms.sendTextMessage(phoneNumber, null,
                            messageBody ,null, null);
                    Toast.makeText(getApplicationContext(),
                            "S.O.S. message sent!", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(),
                            "Message sending failed!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public class GPSReceiver implements LocationListener{

        @Override
        public void onLocationChanged(@NonNull Location location) {
            if (location != null){
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                Toast.makeText(getApplicationContext(), "Ready to send!!", Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(getApplicationContext(), "Not yet ready to send!", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            LocationListener.super.onProviderEnabled(provider);
            Toast.makeText(getApplicationContext(), "GPS Enabled!", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            LocationListener.super.onProviderDisabled(provider);
            Toast.makeText(getApplicationContext(), "Please enable GPS!!", Toast.LENGTH_LONG).show();

        }
    }
}