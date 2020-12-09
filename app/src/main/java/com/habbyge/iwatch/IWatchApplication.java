package com.habbyge.iwatch;

import android.app.Application;

import com.habbyge.iwatch.test.TestClickListener;

public class IWatchApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        TestClickListener.hook();
    }
}
