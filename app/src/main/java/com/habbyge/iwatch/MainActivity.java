package com.habbyge.iwatch;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.habbyge.iwatch.patch.PatchManager;
import com.habbyge.iwatch.test.TestCase0;
import com.habbyge.iwatch.test.TestInlineCase;

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
                Log.i(TAG, "btnClick click !");

                // 测试用例:
                PatchManager.getInstance().testFix("com.habbyge.iwatch.MainActivity",
                            "printf", new Class<?>[] {String.class}, void.class, false,
                            "com.habbyge.iwatch.test.MainActivity2",
                            "printf_hook", new Class<?>[] {String.class}, void.class, false);
            }
        });

        btnHookMethod.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                printf("Mali");

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

//                try {
//                    Class<?> srcClass = Class.forName(
//                            "com.habbyge.iwatch.MainActivity",
//                            true, getClassLoader());
//                    Class<?> dstClass = Class.forName(
//                            "com.habbyge.iwatch.test.MainActivity2",
//                            true, getClassLoader());
//                    HookManager.get().fixMethod(
//                            srcClass, "printf", new Class<?>[]{String.class},
//                            dstClass, "printf_hook", new Class<?>[]{String.class});
//                } catch (Exception e) {
//                    Log.e(TAG, "hook fail: " + e.getMessage());
//                }

//                try {
//                    Class<?> srcClass = MainActivity.class;
//                    Class<?> dstClass = MainActivity2.class;
//
//                    HookManager.get().fixMethod(
//                            srcClass, "printf", new Class<?>[]{String.class},
//                            dstClass, "printf_hook", new Class<?>[]{String.class});
//                } catch (Exception e) {
//                    Log.e(TAG, "hook fail: " + e.getMessage());
//                }
            }
        });

        btnHookField.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
//                try {
//                    Field srcField = MainActivity.class.getDeclaredField("iStr");
//                    srcField.setAccessible(true);
//                    Field dstField = MainActivity.class.getDeclaredField("iStr_HOOK");
//                    dstField.setAccessible(true);
//                    HookManager.get().hookField(srcField, dstField);
//
//                    Field srcField1 = MainActivity.class.getDeclaredField("ix");
//                    srcField1.setAccessible(true);
//                    Field dstField1 = MainActivity.class.getDeclaredField("ix_HOOK");
//                    dstField1.setAccessible(true);
//                    HookManager.get().hookField(srcField1, dstField1);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
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
    public void printf(String text) {
        Log.i(TAG, "printf: " + text);
        ix = 100;
    }

//    @SuppressWarnings("all")
//    public void printf_Hook(String iStr) {
//        Log.i(TAG, "printf-Hook: " + iStr);
//    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume ix = " + ix);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause ix = " + ix);
    }
}