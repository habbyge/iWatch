package com.habbyge.iwatch;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.habbyge.iwatch.test.TestCase0;
import com.habbyge.iwatch.test.TestInlineCase;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * Created by habbyge on 2020/11/24.
 */
public class MainActivity extends AppCompatActivity {
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

        Button btnClick = findViewById(R.id.click);
        Button btnHookMethod = findViewById(R.id.method);
        Button btnHookField = findViewById(R.id.field);
        Button btnHookClass = findViewById(R.id.clazz);
        Button btnHookClickLsn = findViewById(R.id.clickListener);

        btnClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                printf("I love My family: Wifi Daught, Son !");
//                Log.d(TAG, "iStr = " + iStr);
//                Log.d(TAG, "iX = " + ix);

//                TestCase0.printf("This is not fix !");


//                startActivity(new Intent(MainActivity.this, MainActivity2.class));
                printf("Mali");
            }
        });

        btnHookMethod.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
//                HookManager.get().fixMethod(
//                        TestCase0.class, "printf", new Class<?>[] {String.class},
//                        Fix0.class, "printf_Hook", new Class<?>[] {String.class});

//                HookManager.get().fixMethod(
//                        MainActivity2.class, "onResume", null,
//                        MainActivity2Fix.class, "onResume", null);

//                try {
//                    Class<?> srcClass = Class.forName("com.habbyge.iwatch.MainActivity");
//                    Class<?> dstClass = Class.forName("com.habbyge.iwatch.test.MainActivity2");
//                    HookManager.get().fixMethod(
//                            srcClass, "printf", new Class<?>[]{String.class},
//                            dstClass, "printf_Hook", new Class<?>[]{String.class});
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
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

        btnHookClass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MethodHook.hookClass2(Objects.requireNonNull(TestCase0.class.getCanonicalName()));
            }
        });

        btnHookClickLsn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { // 被替换 onClick() 函数的样例
                Log.i(TAG, "onClick, this=" + this.getClass().getName());
            }
        });

        findViewById(R.id.click_inline).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TestInlineCase().getIx();
            }
        });
    }

    @SuppressWarnings("all")
    private void printf(String text) {
        Log.i(TAG, "printf: " + text);
        ix = 100;
    }

//    @SuppressWarnings("all")
//    public void printf_Hook(String iStr) {
//        Log.i(TAG, "printf-Hook: " + iStr);
//    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause ix = " + ix);
    }
}