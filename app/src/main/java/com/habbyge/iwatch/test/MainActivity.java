package com.habbyge.iwatch.test;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Keep;
import androidx.core.app.ActivityCompat;

import com.habbyge.iwatch.R;
import com.habbyge.iwatch.patch.PatchManager;
import com.habbyge.iwatch.util.FileUtil;

import java.io.File;
import java.util.Enumeration;
import java.util.jar.JarEntry;

import dalvik.system.DexClassLoader;

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

    /**
     * 这里是解析 patch.apk 的样例
     */
    private void test4LoadPatch() {
        final File patchDir = this.getExternalFilesDir(Environment.DIRECTORY_MUSIC);

//        final String patchPath = patchDir + "/" + "boot-arm64-v8-base-0x28000150.apk";
        final String patchPath = patchDir + "/" + "amm_app_preview_arm64_v8a.apk";
        try {
            final ClassLoader cl = MainActivity.class.getClassLoader();
            final DexClassLoader dexCl = new DexClassLoader(patchPath, null, null, cl);

            File patchFile = new File(patchPath);
            Enumeration<JarEntry> jarEntries = FileUtil.parseJarFile(patchFile);
            if (jarEntries == null) {
                Log.e(TAG, "test4LoadPatch, jarEntries is NULL");
                return;
            }
            Class<?> clazz;
            String entry;
            while (jarEntries.hasMoreElements()) {
                entry = jarEntries.nextElement().getName();

                if (entry.endsWith(".dex")) {
                    if (entry.equals("test.dex")) {
                        clazz = dexCl.loadClass("com.tencent.tinker.loader.TinkerTestDexLoad");
                        Log.d(TAG, "entry=" + entry + ", class=" + clazz.getName());
                    } else if (entry.equals("classes.dex")) {
                        clazz = dexCl.loadClass("com.tencent.mm.plugin.webview.ui.tools.jsapi.f");
                        Log.d(TAG, "entry=" + entry + ", class=" + clazz.getName());
                    }
////                    clazz = dexCl.loadClass(entry);
////                    DexFile dexFile = new DexFile(entry);
////                    Enumeration<String> classes = dexFile.entries();
////                    if (classes != null) {
////                        while (classes.hasMoreElements()) {
////                            String className = classes.nextElement();
                            Log.d(TAG, "entry=" + entry);
////                        }
////                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "test4LoadPatch, exception: ", e);
        }
    }

    private final static String[] PERMISSIONS_STORAGE = {
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE"
    };

    public static void verifyStoragePermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity, "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, 100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        verifyStoragePermissions(this);

        Button btnClick = findViewById(R.id.click);
        Button btnHookMethod = findViewById(R.id.method);
        Button btnHookField = findViewById(R.id.field);
        Button btnHookClass = findViewById(R.id.clazz);
        Button btnHookClickLsn = findViewById(R.id.clickListener);

        btnClick.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // fixme 测试用例:
                PatchManager.getInstance().testFix(
                        "com.habbyge.iwatch.test.MainActivity", "printf",
                        new Class<?>[]{String.class}, void.class, false,

                        "com.habbyge.iwatch.test.MainActivity2", "printf_hook",
                        new Class<?>[]{String.class}, void.class, false);
            }
        });

        btnHookMethod.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                printf("Mali_Mango_Pidan_Habby");
            }
        });

        btnHookField.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO: 2021/2/25 测试加载patch的样例：
                // /sdcard/boot-arm64-v8-base-0x28000150.apk 这是一个补丁包
                test4LoadPatch();
            }
        });

        btnHookClass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                MethodHook.hookClass2(Objects.requireNonNull(TestCase0.class.getCanonicalName()));
                Log.i(TAG, "btnHookClass.onClick, hook success !");
            }
        });

        btnHookClickLsn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { // 被替换 onClick() 函数的样例
                /*Log.i(TAG, "onClick, this=" + this.getClass().getName());*/
                Log.i(TAG, "btnHookClickLsn.onClick, hook success !");
            }
        });

        findViewById(R.id.click_inline).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TestInlineCase().getIx();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

    @Keep
    @SuppressWarnings("all")
    private void printf(String text) {
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