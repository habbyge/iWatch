package com.habbyge.iwatch;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Keep;
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
                PatchManager.getInstance().testFix(
                        "com.habbyge.iwatch.MainActivity", "printf",
                        new Class<?>[]{String.class}, void.class, false,

                        "com.habbyge.iwatch.test.MainActivity2", "printf_hook",
                        new Class<?>[]{String.class}, void.class, false);
            }
        });

        btnHookMethod.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
//                printf("Mali");
                printf("Mali_Mango_Pidan_Habby");
            }
        });

        btnHookField.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
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

    @Keep
    @SuppressWarnings("all")
    public void printf(String text) {
        Log.i(TAG, "printf: " + text);
        ix = 100;
    }

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