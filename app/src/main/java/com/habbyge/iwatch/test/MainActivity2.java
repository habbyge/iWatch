package com.habbyge.iwatch.test;

import android.app.Activity;
import android.util.Log;

public class MainActivity2 extends Activity {
    private static final String TAG = "iWatch.MainActivity2";

//    private AppBarConfiguration appBarConfiguration;
//
//    private int testCaseField = 100;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main2);
//        setSupportActionBar(findViewById(R.id.toolbar));
//
//        NavController navController = Navigation.findNavController(
//                this, R.id.nav_host_fragment_content_main);
//
//        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
//        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
//
//        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null)
//                        .show();
//            }
//        });
//    }

//    @Override
//    public boolean onSupportNavigateUp() {
//        NavController navController = Navigation.findNavController(
//                this, R.id.nav_host_fragment_content_main);
//
//        return NavigationUI.navigateUp(navController, appBarConfiguration)
//                || super.onSupportNavigateUp();
//    }
//
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        Log.d(TAG, "onPause, testCaseField: " + testCaseField);
//    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        Log.d(TAG, "onResume");
//        testCaseField = 10;
//    }

    @SuppressWarnings("all")
    public void printf_hook(String iStr) {
        Log.i(TAG, "printf-Hook: " + iStr);

//        try {
//            Field field = this.getClass().getDeclaredField("ix");
//            field.setAccessible(true);
//            field.set(this, 10000);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}