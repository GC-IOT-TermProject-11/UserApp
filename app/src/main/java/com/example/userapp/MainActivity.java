package com.example.userapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 1;

    private WifiManager wifiManager;
    private Button scanButton;
    private TextView currLocation;
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
            return;
        }
        List<ScanResult> scanResults = wifiManager.getScanResults();

        JSONArray wifiArray = new JSONArray();
        for (ScanResult result : scanResults) {
            String bssid = result.BSSID;
            int signalStrength = result.level;

            JSONObject wifiObject = new JSONObject();
            try {
                wifiObject.put("BSSID", bssid);
                wifiObject.put("SignalStrength", signalStrength);
                wifiArray.put(wifiObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        JSONObject dataObject = new JSONObject();
        try {
            dataObject.put("wifiList", wifiArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 전송할 데이터를 JSON 형식으로 생성
        String jsonData = dataObject.toString();

        sendDataToServer(jsonData);
    }

    private void sendDataToServer(String jsonData) {
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(mediaType, jsonData);

        Request request = new Request.Builder()
                .url("http://gyuwon.pythonanywhere.com/predict") // 서버 URL 입력
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    // 서버 응답 처리
                    runOnUiThread(() -> {
                        try {
                            JSONObject json = new JSONObject(responseBody);
                            String predictions = json.getString("predictions");
                            String serverResponse = predictions;
                            serverResponse = serverResponse.replaceAll("[^0-9]", "");
                            currLocation=findViewById(R.id.resultTextView);
                            currLocation.setText(serverResponse);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            // JSON 파싱 오류 처리
                        }
                    });
                } else {
                    // 서버 응답 실패 처리
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Failed to send data to server.", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                // 서버 요청 실패 처리
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Failed to send data to server.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
