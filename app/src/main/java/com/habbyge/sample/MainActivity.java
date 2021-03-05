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

    // 字符-测试样例
    public static int ix = 10;
    public int ix_HOOK = 10000;

    @SuppressWarnings("all")
    private String iStr = "iWatch";
    @SuppressWarnings("all")
    private String iStr_HOOK = "iWatch.HOOK";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(TAG, "onCreate, ix_HOOK=" + ix_HOOK + ", ix=" + ix);
        printf("onCreate");

        findViewById(R.id.method).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.i(TAG, "New-Fix, btnHookMethod, onClick success !");

// [问题1]：这里是为了解决内部类导致的编译期在其外部类中合成 syntthetic(合成) 方法，即：在外部类中合成：
// static synthetic xxx(外部类对象的引用) {
//     外部类_CF->字段;
// }
// 2021/3/4 (其实应该是做成策略模式callback出去的，这里懒了):
// 这里是为了配合iWatch，根据需要设置为public的field(用注解(com.habbyge.iwatch.patch.StopSyntheticAnno)标注)，
// 防止由于内部类新增引用到外部类的private字段，而导致编译期生成 synthetic 方法，从而导致java.lang.VerifyError


// 2021-03-01 10:55:55.022 23916-23916/? E/AndroidRuntime: FATAL EXCEPTION: main
// Process: com.habbyge.iwatch, PID: 23916
// java.lang.IllegalAccessError: Method 'void com.habbyge.sample.MainActivity.a(java.lang.String)'
// is inaccessible to class 'com.habbyge.sample.MainActivity$2_CF'
// (declaration of 'com.habbyge.sample.MainActivity$2_CF' appears in
// /storage/emulated/0/Android/data/com.habbyge.iwatch/files/Music/app-release-2-4c68d301e8924fcd37a28f04a32da936.apatch)
// at com.habbyge.sample.MainActivity$2_CF.onClick(MainActivity.java:73)
// 解决方案: ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// 要求调用的原始类中的字段和方法必须是public的，因为，Patch中的修复方法所属的类是修复后的类(原始类名_CF)，
// 虽然传入的对象依旧是旧的原始对象，但是类名已经不同了，会导致只能访问public的字段和方法(相当于在A类中调用B类中方法，所以只能访问public).
// 注意: 如果是内部类引用外部类的 private 对象，则必须修改为public才行，避免生成get(旧的对象)，出现crash:
// 2021-03-03 20:31:48.979 7013-7013/? E/AndroidRuntime: FATAL EXCEPTION: main
//    Process: com.habbyge.iwatch, PID: 7013
//    java.lang.VerifyError: Verifier rejected class com.habbyge.sample.MainActivity_CF:
//      int com.habbyge.sample.MainActivity_CF.a(com.habbyge.sample.MainActivity) failed to verify:
//      int com.habbyge.sample.MainActivity_CF.a(com.habbyge.sample.MainActivity):
//      [0x2] cannot access instance field int com.habbyge.sample.MainActivity_CF.b from object of type Reference:
//      com.habbyge.sample.MainActivity (declaration of 'com.habbyge.sample.MainActivity_CF' appears in /storage/emulated/0/Android/data/com.habbyge.iwatch/files/Music/app-release-2-a078ddf55dbaee1fd8b70ff022c4f491.apatch)
//
//        at com.habbyge.sample.MainActivity_CF.a(Unknown Source:0)
//        at com.habbyge.sample.MainActivity$1_CF.onClick(MainActivity.java:70)
//        at android.view.View.performClick(View.java:7187)
//        at android.view.View.performClickInternal(View.java:7164)
//        at android.view.View.access$3500(View.java:813)
//        at android.view.View$PerformClick.run(View.java:27649)
//        at android.os.Handler.handleCallback(Handler.java:883)
//        at android.os.Handler.dispatchMessage(Handler.java:100)
//        at android.os.Looper.loop(Looper.java:230)
//        at android.app.ActivityThread.main(ActivityThread.java:7752)
//        at java.lang.reflect.Method.invoke(Native Method)
//        at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:526)
//        at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1034)

// todo 这里问题的关键是：编译期自动生成了synthetic方法，一般在内部类调用外部类是会在外部类中生成，如果修复包在某个内部类中
//  新增了一个外部类中某个字段的调用，这样在打修复包时会新增的static syncthetic方法，但是传入的参数仍旧是修复前的类对象(
//  不带_CF)的，这就导致 java.lang.VerifyError，有两个方案来解决：
//  方案1：在修复包生成时，编译期阻止生成该合成类型的方法，即设置字段为public，在transform中，根据手动增加的注解，来设
//  置该字段设置为public; 结合 Hellhound 项目即可
//  方案2：不阻止修复包生成Synthetic方法，尝试解决 ？？？？？？
//                ix = 1000; // 这里有坑，
//                ix_HOOK = 1000; // 这里有坑
//                printf("Mango_Pidan_Mali_Habby-New!!");
//  采用方案：第1个方案.

// TODO: 2021/3/1
//  2021-03-01 17:38:28.774 15008-15008/? E/AndroidRuntime: FATAL EXCEPTION: main
//  Process: com.habbyge.iwatch, PID: 15008
//  java.lang.NoClassDefFoundError: Failed resolution of: Lcom/habbyge/iwatch/test/a;
//  at com.habbyge.sample.MainActivity$1_CF.onClick(MainActivity.java:71)
//  Caused by: java.lang.ClassNotFoundException: com.habbyge.sample.a
//  at com.habbyge.sample.MainActivity$1_CF.onClick(MainActivity.java:71)
//  解决方案: ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//  用Patch中的自定义的DexClassLoader来load patch中新增的类，在这里(这里本质上patch类中)是获取不到非DexClassLoader加载
//  过的类的，所以，必须在框架(具体是iwatch.java)中生成DexClassLoader的地方加载补丁中的所有class.
//                new Test().print("Mango_Pidan_Mali_Habby");
            }
        });

// [问题2]
// TODO: 2021/3/1 以上两个问题，可以统一用同一个方案来解决：
//  生成补丁时，需要diff，
    }

    @Override
    protected void onResume() {
        super.onResume();
        ix = 1001;
        ix_HOOK = 10001;
        printf("onResume");
    }

    public void printf(String text) { // 通过修改
        Log.w(TAG, "printf-bengin: " + text + "ix=" + ix + ", i love my family !");
        for (int i = 0; i < 10; ++i) {
            ++ix_HOOK;
            ++ix;
        }
        Log.d(TAG, "printf-end: " + text + ", ix_HOOK=" + ix_HOOK);
    }
}
