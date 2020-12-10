package com.habbyge.iwatch;

import android.app.Application;

import com.habbyge.iwatch.test.TestClickListener;
import com.habbyge.iwatch.test.TestInlineCase_Fix;

public class IWatchApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        TestClickListener.hook();
        TestInlineCase_Fix.fix();
    }
}
