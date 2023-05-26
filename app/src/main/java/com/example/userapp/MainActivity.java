package com.example.userapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 1;

    private WifiManager wifiManager;
    private Button scanButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_locations);

        scanButton = findViewById(R.id.currentLocationButton);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSIONS_REQUEST_CODE);
                } else {
                    scanWifiNetworks();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanWifiNetworks();
            } else {
                Toast.makeText(this, "Permission denied. Cannot scan Wi-Fi networks.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void scanWifiNetworks() {
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "Wi-Fi is disabled. Enable Wi-Fi and try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        wifiManager.startScan();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        List<ScanResult> scanResults = wifiManager.getScanResults();

        //스캔되는지 테스트
        for (ScanResult result : scanResults) {
            String ssid = result.SSID;
            String bssid = result.BSSID;
            int signalStrength = result.level;
            // 여기에서 원하는 출력 형태로 조정할 수 있습니다.
            String output = "BSSID: " + bssid + ", SSID: " + ssid + ", Signal Strength: " + signalStrength + "dBm";
            System.out.println(output);
        }
    }
}
