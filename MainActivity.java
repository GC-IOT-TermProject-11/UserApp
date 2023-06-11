package com.example.userapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
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

    MediaPlayer mediaPlayer;

    private static final int PERMISSIONS_REQUEST_CODE = 1;

    private WifiManager wifiManager;
    private Button scanBtn;
    private Button sendDestinationBtn;
    private TextView currLocation;
    private TextView destination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_locations);

        scanBtn = findViewById(R.id.currentLocationButton);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        scanBtn.setOnClickListener(new View.OnClickListener() {
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

        //send 버튼 클릭시 서버로 현재 위치와 목적지 전송, navigationActivity로 전환
        sendDestinationBtn = findViewById(R.id.sendDestination);
        destination = findViewById(R.id.destTextView);
        sendDestinationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String dest = ""+destination.getText();

                mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.strat); // 안내 시작 음성 재생
                mediaPlayer.start(); // 안내 음성 시작

//                JSONObject dataObject = new JSONObject();
//                try {
//                    dataObject.put("dest",dest);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//                String jsonData = dataObject.toString();
                sendDestinationToServer(dest);

                // 서버로부터 데이터 전송이 안돼서 앱이 중간에 꺼지는 걸 방지 하기 위해 일단 주석 처리
//                Intent intent = new Intent(getApplicationContext(), navigationActivity.class);
//                startActivity(intent);
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
            if (result.SSID.equalsIgnoreCase("GC_free_WiFi")) {
                String bssid = result.BSSID;
                int signalStrength = result.level;
//                System.out.println(bssid);
                JSONObject wifiObject = new JSONObject();
                try {
                    wifiObject.put("BSSID", bssid);
                    wifiObject.put("RSSI", signalStrength);
                    wifiArray.put(wifiObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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
        System.out.println(jsonData);
        sendWiFiDataToServer(jsonData);
    }

    //현재 위치 센싱하여 서버로 와이파이 리스트 전송
    private void sendWiFiDataToServer(String jsonData) {
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
                            currLocation = findViewById(R.id.resultTextView);
                            currLocation.setText(serverResponse);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            // JSON 파싱 오류 처리
                        }
                    });
                } else {
                    // 서버 응답 실패 처리
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Failed to send WiFi list to server.", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                // 서버 요청 실패 처리
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Failed to send WiFi list to server.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    //목적지 입력하여 서버로 전송
    private void sendDestinationToServer(String destination) {
        String currentLocation = currLocation.getText().toString(); // 현재 위치 가져오기

        JSONObject dataObject = new JSONObject();
        try {
            dataObject.put("currentLocation", currentLocation);
            dataObject.put("destination", destination);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        System.out.println("sendToServer"+dataObject);
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(mediaType, dataObject.toString());

        Request request = new Request.Builder()
                .url("http://gyuwon.pythonanywhere.com/pathfind") // 서버 URL 입력
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
                            String path = json.getString("shortestPath");
                            String serverResponse = path;
                            System.out.println("path is "+ path);
                            serverResponse = serverResponse.replaceAll("[^0-9]", "");
                            Intent pathIntent = new Intent(getApplicationContext(), navigationActivity.class);
                            pathIntent.putExtra("shortestPath", serverResponse);
                            pathIntent.putExtra("destination", destination);
                            startActivity(pathIntent);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } else {
                    // 서버 응답 실패 처리
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Failed to send destination to server.", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                // 서버 요청 실패 처리
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Failed to send destination to server.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

}