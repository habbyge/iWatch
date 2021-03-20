package com.habbyge.sample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.habbyge.iwatch.R;

/**
 * Created by habbyge on 2020/11/24.
 */
public class MainActivity extends Activity {
    private static final String TAG = "iWatch.MainActivity";

    // 测试用例: private字段是否可访问
    public static int ix = 10;
    public int ix_HOOK = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.i(TAG, "onResume-2, ix_HOOK=" + ix_HOOK + ", ix=" + ix + ", " + ix);

        findViewById(R.id.method).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Fix-2, btnHookMethod, onClick !"); // 测试用例：验证内部类改变

                // 测试用例：内部类访问外部现有private字段(static or not)会失败，因为编译期会为private字段合成
                // 一个访问方法（类型flag：Synthetic），该访问方法的形参是旧的修复前的类型(对象)，但是函数中是修复
                // 类名，因此发生failed to verify的crash，因此必须修改成public才能阻止其生成合成方法。
                ix = 1;
                ix_HOOK = 10;
                Log.d(TAG, "HABBYGE-MALI, ix=" + ix + ", ix_HOOK=" + ix_HOOK);

                // 测试用例：测试(匿名)内部类访问新增的class(包括新增字段、方法)
//                Test test = new Test();
//                test.print("i love my family ! new !!!!!!");

                // 测试用例：测试(匿名)内部类访问现有方法(public/private)
//                printf("onClick");
            }
        });

        printf2("onCreate-End", 1000);
    }

    // 测试用例: 验证新增方法、访问类中字段......
    public void printf2(String text, int x1) { // 通过修改
        Log.w(TAG, "printf2-begin: " + text + ", ix=" + ix + "， strX_Added=");
        for (int i = 0; i < 100; ++i) {
            ++ix_HOOK;
            ++ix;
            ++x1;
//        printf(text + x1);
        }
        printf(text + x1);
        Log.w(TAG, "printf2-end: " + text + ", ix=" + ix);
    }

    // 测试用例: 验证新增方法、访问类中字段......
    public void printf(String text) { // 通过修改
        Log.w(TAG, "printf-bengin-2: " + text + ", ix=" + ix + "，beg !!!!!!");
        for (int i = 0; i < 100; ++i) {
            ++ix_HOOK;
            ++ix;
        }
        Log.w(TAG, "printf-Mid0-2: " + text + ", ix=" + ix + ", x1=" + "，end !!!!!!");
        int x = ix * ix_HOOK;
        int x2 = ix * ix_HOOK;
        Log.w(TAG, "printf-End-2: " + text + ", ix=" + ix + ", x2=" + x2 + "，end !!!!!!");
        Log.d(TAG, "printf-Mid1-2: " + text + "x, ix_HOOK2="
                + ix_HOOK + ", " /*+ test("ix_HOOK_ix")*/
                + ", x = " + x + ", xx=" + text);
    }
}
