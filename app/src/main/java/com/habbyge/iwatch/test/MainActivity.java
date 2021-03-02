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
    private static int ix = 10;
    private int ix_HOOK = 10000;

    @SuppressWarnings("all")
    private String iStr = "iWatch";
    @SuppressWarnings("all")
    private String iStr_HOOK = "iWatch.HOOK";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ix = 100;
        ix_HOOK = 1000;

//        verifyStoragePermissions(this);

//        Button btnClick = findViewById(R.id.click);
        Button btnHookMethod = findViewById(R.id.method);
//        Button btnHookField = findViewById(R.id.field);
//        Button btnHookClass = findViewById(R.id.clazz);
//        Button btnHookClickLsn = findViewById(R.id.clickListener);

//        btnClick.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                Log.i(TAG, "Old-Fix, btnClick, onClick success !");
////                printf("Mali_Mango_Pidan_Habby");
//            }
//        });

        btnHookMethod.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.i(TAG, "New-Fix, btnHookMethod, onClick success !");

// TODO: 2021/3/1
//  2021-03-01 10:55:55.022 23916-23916/? E/AndroidRuntime: FATAL EXCEPTION: main
//  Process: com.habbyge.iwatch, PID: 23916
//  java.lang.IllegalAccessError: Method 'void com.habbyge.iwatch.test.MainActivity.a(java.lang.String)'
//  is inaccessible to class 'com.habbyge.iwatch.test.MainActivity$2_CF'
//  (declaration of 'com.habbyge.iwatch.test.MainActivity$2_CF' appears in
//  /storage/emulated/0/Android/data/com.habbyge.iwatch/files/Music/app-release-2-4c68d301e8924fcd37a28f04a32da936.apatch)
//  at com.habbyge.iwatch.test.MainActivity$2_CF.onClick(MainActivity.java:73)
//  解决方案: ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//  要求调用的原始类中的字段和方法必须是public的，因为，Patch中的修复方法所属的类是修复后的类(原始类名_CF)，
//  虽然传入的对象依旧是旧的原始对象，但是类名已经不同了，会导致只能访问public的字段和方法(相当于在A类中调用B类中方法，所以只能访问public).
                ix = 1000; // 需要public todo
                ix_HOOK = 1000;
//                printf("Mango_Pidan_Mali_Habby-New!!");

// TODO: 2021/3/1
//  2021-03-01 17:38:28.774 15008-15008/? E/AndroidRuntime: FATAL EXCEPTION: main
//  Process: com.habbyge.iwatch, PID: 15008
//  java.lang.NoClassDefFoundError: Failed resolution of: Lcom/habbyge/iwatch/test/a;
//  at com.habbyge.iwatch.test.MainActivity$1_CF.onClick(MainActivity.java:71)
//  Caused by: java.lang.ClassNotFoundException: com.habbyge.iwatch.test.a
//  at com.habbyge.iwatch.test.MainActivity$1_CF.onClick(MainActivity.java:71)
//  解决方案: ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//  用Patch中的自定义的DexClassLoader来load patch中新增的类，在这里(这里本质上patch类中)是获取不到非DexClassLoader加载
//  过的类的，所以，必须在框架(具体是iwatch.java)中生成DexClassLoader的地方加载补丁中的所有class.
//                new Test().print("Mango_Pidan_Mali_Habby");
            }
        });

// TODO: 2021/3/1 以上两个问题，可以统一用同一个方案来解决：
//  生成补丁时，需要diff，
    }

    @Keep
    @SuppressWarnings("all")
    private void printf(String text) {
        Log.i(TAG, "printf: " + text + ", ix=" + ix + ", ix_HOOK=" + ix_HOOK);
        ix = 100;
    }
}
