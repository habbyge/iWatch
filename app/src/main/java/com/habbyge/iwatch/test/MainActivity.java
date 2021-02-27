package com.habbyge.iwatch.test;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Keep;

import com.habbyge.iwatch.R;

/**
 * Created by habbyge on 2020/11/24.
 */
public class MainActivity extends Activity {
    private static final String TAG = "iWatch.MainActivity";

    // 字符-测试样例
    @SuppressWarnings("all")
    private static int ix = 10;
    @SuppressWarnings("all")
    private static int ix_HOOK = 10000;

    @SuppressWarnings("all")
    private String iStr = "iWatch";
    @SuppressWarnings("all")
    private String iStr_HOOK = "iWatch.HOOK";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        verifyStoragePermissions(this);

        Button btnClick = findViewById(R.id.click);
        Button btnHookMethod = findViewById(R.id.method);
//        Button btnHookField = findViewById(R.id.field);
//        Button btnHookClass = findViewById(R.id.clazz);
//        Button btnHookClickLsn = findViewById(R.id.clickListener);

        btnClick.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.i(TAG, "new-btnClick, onClick success !");
            }
        });

        btnHookMethod.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO: 2021/2/28
                //  2021-02-28 00:16:21.685 26799-26799/? E/AndroidRuntime: FATAL EXCEPTION: main
                //    Process: com.habbyge.iwatch, PID: 26799
                //    java.lang.NoSuchMethodError: No static method a(Lcom/habbyge/iwatch/test/MainActivity;Ljava/lang/String;)V in class Lcom/habbyge/iwatch/test/MainActivity; or its super classes (declaration of 'com.habbyge.iwatch.test.MainActivity' appears in /data/app/~~FN3d8xyjdaZSwNFcPSFTrw==/com.habbyge.iwatch-kRoweD3y346LXGwwdwp7zQ==/base.apk)
                //        at com.habbyge.iwatch.test.MainActivity$2_CF.onClick(MainActivity.java:185)
                //        at android.view.View.performClick(View.java:7448)
                //        at android.view.View.performClickInternal(View.java:7425)
                //        at android.view.View.access$3600(View.java:810)
                //        at android.view.View$PerformClick.run(View.java:28305)
                //        at android.os.Handler.handleCallback(Handler.java:938)
                //        at android.os.Handler.dispatchMessage(Handler.java:99)
                //        at android.os.Looper.loop(Looper.java:223)
                //        at android.app.ActivityThread.main(ActivityThread.java:7660)
                //        at java.lang.reflect.Method.invoke(Native Method)
                //        at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:592)
                //        at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:947)
                printf("Mali_Mango_Pidan_Habby");
                Log.i(TAG, "new-btnClick, onClick success !");
            }
        });
    }

    @Keep
    @SuppressWarnings("all")
    private static void printf(String text) {
        Log.i(TAG, "printf: " + text);
        ix = 100;
    }
}