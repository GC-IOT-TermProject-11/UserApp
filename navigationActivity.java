package com.example.userapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
    private TextView path;
    private TextView Destination;

    private Intent pathIntent; // pathIntent 멤버 변수 선언
    private Handler handler;

    private Timer timer;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation);

        //mainActivity에서 넘어온 경로를 출력
        pathIntent = getIntent();
        path = findViewById(R.id.path);
        path.setText(pathIntent.getStringExtra("path"));
        Destination = findViewById(R.id.destination);
        Destination.setText("목적지: " + pathIntent.getStringExtra("destination"));
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        //5초마다 와이파이 스캔 및 서버에 현위치 전송
        timer =new Timer();
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
                            currLocation = findViewById(R.id.currentLocaiton);
                            if (serverResponse.equals(pathIntent.getStringExtra("destination"))) {
                                currLocation.setText("도착했습니다");
                                // 5초 후에 set_locations.xml 화면으로 돌아가기
                                handler = new Handler(Looper.getMainLooper());
                                handler.postDelayed(() -> {
                                    Intent intent = new Intent(navigationActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }, 5000);
                            }
                            else {
                                currLocation.setText("현 위치: " + serverResponse);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            // JSON 파싱 오류 처리
                        }
                    });
                } else {
                    // 서버 응답 실패 처리
                    runOnUiThread(() -> {
                        Toast.makeText(navigationActivity.this, "Failed to send data to server.", Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Timer 중지 및 null 처리
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        // Handler 중지
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }
}