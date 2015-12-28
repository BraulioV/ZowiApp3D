package com.jalcdeveloper.zowiapp;

import android.app.Application;

import com.jalcdeveloper.zowiapp.io.Zowi;
import com.jalcdeveloper.zowiapp.io.ZowiBluetooth;

public class ZowiApp extends Application {

    public Zowi zowi;

    @Override
    public void onCreate() {
        super.onCreate();

        zowi = new ZowiBluetooth(this);

    }
}
