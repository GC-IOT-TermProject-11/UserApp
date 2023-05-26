package com.example.userapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 스플래시 화면 표시 시간 (밀리초)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_layout);

        // 일정 시간이 지난 후 다음 화면으로 전환하기 위해 핸들러를 사용합니다.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // 다음 화면으로 전환하는 인텐트를 생성하고 실행합니다.
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // 스플래시 액티비티를 종료합니다.
            }
        }, SPLASH_DELAY);
    }
}

