package com.jalcdeveloper.zowiapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.jalcdeveloper.zowiapp.R;

public class SplashScreenActivity extends ImmersiveActivity {

    private static int SPLASH_TIME = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashScreenActivity.this, ConnectActivity.class));
                SplashScreenActivity.this.finish();
            }
        },SPLASH_TIME);

    }

}
