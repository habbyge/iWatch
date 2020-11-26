package com.habbyge.iwatch;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by habbyge on 2020/11/24.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "iWatch.MainActivity";

    // 字符-测试样例
    private static int ix = 10;
    private static int ix_HOOK = 10000;

    private String iStr = "iWatch";
    private String iStr_HOOK = "iWatch.HOOK";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnClick = findViewById(R.id.click);
        Button btnHookMethod = findViewById(R.id.method);
        Button btnHookField = findViewById(R.id.field);

        btnClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printf("I love My family: Wifi Daught, Son !");
                Log.d(TAG, "iStr = " + iStr);
                Log.d(TAG, "iX = " + ix);
            }
        });

        btnHookMethod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Method srcMethod = MainActivity.class.getDeclaredMethod(
                            "printf", String.class);
                    Method destMethod = MainActivity.class.getDeclaredMethod(
                            "printf_Hook", String.class);
                    HookManager.get().hookMethod(srcMethod, destMethod);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        });

        btnHookField.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    Field srcField = MainActivity.class.getDeclaredField("iStr");
                    srcField.setAccessible(true);
                    Field dstField = MainActivity.class.getDeclaredField("iStr_HOOK");
                    dstField.setAccessible(true);
                    HookManager.get().hookField(srcField, dstField);

                    Field srcField1 = MainActivity.class.getDeclaredField("ix");
                    srcField1.setAccessible(true);
                    Field dstField1 = MainActivity.class.getDeclaredField("ix_HOOK");
                    dstField1.setAccessible(true);
                    HookManager.get().hookField(srcField1, dstField1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printf(String text) {
        Log.i(TAG, "printf: " + text);
    }

    public void printf_Hook(String iStr) {
        Log.i(TAG, "printf-Hook: " + iStr);
        Log.i(TAG, "printf-Hook: 0");
        Log.i(TAG, "printf-Hook: 1");
        Log.i(TAG, "printf-Hook: 2");
        Log.i(TAG, "printf-Hook: 3");
        Log.i(TAG, "printf-Hook: 4");

        int ix = 10000;
        Log.i(TAG, "printf-Hook: " + ix + 0);
        Log.i(TAG, "printf-Hook: " + ix + 1);
        Log.i(TAG, "printf-Hook: " + ix + 2);
        Log.i(TAG, "printf-Hook: " + ix + 3);
        Log.i(TAG, "printf-Hook: " + ix + 4);

        HookManager.get().callOrigin(this, "I love Mali");
    }
}