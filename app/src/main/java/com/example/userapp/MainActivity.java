package com.example.userapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int WIFI_SCAN_PERMISSION_REQUEST_CODE = 100;
    private WifiManager wifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 와이파이 매니저 초기화
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        // 와이파이 스캔 권한 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    WIFI_SCAN_PERMISSION_REQUEST_CODE);
        } else {
            startWiFiScan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == WIFI_SCAN_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startWiFiScan();
            } else {
                Toast.makeText(this, "와이파이 스캔 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startWiFiScan() {
        // 와이파이 스캔 시작
        wifiManager.startScan();

        // 와이파이 스캔 결과 가져오기
        List<ScanResult> scanResults = wifiManager.getScanResults();

        // 스캔 결과를 사용하여 원하는 작업을 수행할 수 있습니다.
        // 예를 들어, 스캔 결과를 로그에 출력하는 코드는 다음과 같습니다.
        for (ScanResult scanResult : scanResults) {
            String ssid = scanResult.SSID;
            int signalLevel = scanResult.level;

            // 로그에 출력
            System.out.println("SSID: " + ssid + ", 신호 강도: " + signalLevel);
        }

        // 스플래시 화면을 나타내는 동안 와이파이 스캔 작업이 진행됩니다.
        // 이후 원하는 작업으로 이동하면 됩니다.
    }
}
