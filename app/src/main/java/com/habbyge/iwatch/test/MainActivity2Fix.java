package com.habbyge.iwatch.test;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.snackbar.Snackbar;
import com.habbyge.iwatch.R;

import java.lang.reflect.Field;

public class MainActivity2Fix extends AppCompatActivity {
    private static final String TAG = "iWatch.MainActivity2Fix";

    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        setSupportActionBar(findViewById(R.id.toolbar));

        NavController navController = Navigation.findNavController(
                this, R.id.nav_host_fragment_content_main);

        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 验证当前对象是否 是旧的对象，期望是旧的对象，经过验证：YES.
        Log.d(TAG, "onResume: class = " + this.getClass().getName());

        // 这里的例子是表示
        try {
            Field testCaseField = MainActivity2.class.getDeclaredField("testCaseField");
            testCaseField.setAccessible(true);
            Object obj = testCaseField.get(this);
            if (obj instanceof Integer) {
                int testCaseFieldOb = (int) obj;
                Log.i(TAG, "testCaseFieldOb=" + testCaseFieldOb);
            }
            testCaseField.set(this, 1000);
        } catch (Exception e) {
            Log.e(TAG, "crash = " + e.getMessage());
        }
    }
}
