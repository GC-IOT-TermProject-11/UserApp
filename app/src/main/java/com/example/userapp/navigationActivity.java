package com.example.userapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class navigationActivity extends AppCompatActivity {
    private WifiManager wifiManager;
    private TextView currLocation;

    MediaPlayer mediaPlayer;
    private TextView path;
    private TextView Destination;
    private TextView distanceView;
    private TextView directionView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation);

        //mainActivity에서 넘어온 경로를 출력
        Intent pathIntent = getIntent();
        path = findViewById(R.id.path);
        path.setText(pathIntent.getStringExtra("path"));
        Destination = findViewById(R.id.destination);
        Destination.setText("목적지: " + pathIntent.getStringExtra("destination"));
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        //5초마다 와이파이 스캔 및 서버에 현위치 전송
        Timer timer =new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                scanWifiNetworks();
            }
        };
        timer.scheduleAtFixedRate(task, 0,5000);
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
                wifiObject.put("RSSI", signalStrength);
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
                .url("http://gyuwon.pythonanywhere.com/navigate") // 서버 URL 입력
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
                            String predictionsResponse = predictions;
                            predictionsResponse = predictionsResponse.replaceAll("[^0-9]", "");
                            currLocation = findViewById(R.id.currentLocaiton);
                            currLocation.setText("현 위치: "+predictionsResponse);

                            String distance = json.getString("distance");
                            distanceView = findViewById(R.id.distance);
                            distanceView.setText("거리: "+distance+"m");

                            String direction = json.getString("direction");
                            directionView = findViewById(R.id.directionText);
                            directionView.setText("방향: "+direction);
                            if(direction.equalsIgnoreCase("직진")) {
                                mediaPlayer = MediaPlayer.create(navigationActivity.this, R.raw.straight); // 안내 시작 음성 재생
                                mediaPlayer.start(); // 안내 음성 시작
                            }
                            if(direction.equalsIgnoreCase("우회전")) {
                                mediaPlayer = MediaPlayer.create(navigationActivity.this, R.raw.right); // 안내 시작 음성 재생
                                mediaPlayer.start(); // 안내 음성 시작
                            }
                            if(direction.equalsIgnoreCase("좌회전")) {
                                mediaPlayer = MediaPlayer.create(navigationActivity.this, R.raw.left); // 안내 시작 음성 재생
                                mediaPlayer.start(); // 안내 음성 시작
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            // JSON 파싱 오류 처리
                        }
                    });
                } else {
                    // 서버 응답 실패 처리
                    runOnUiThread(() -> {
                        Toast.makeText(navigationActivity.this, "Failed to receive data from server.", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                // 서버 요청 실패 처리
                runOnUiThread(() -> {
                    Toast.makeText(navigationActivity.this, "Failed to send data to server.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}