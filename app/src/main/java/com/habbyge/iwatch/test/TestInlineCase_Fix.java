package com.habbyge.iwatch.test;

import android.util.Log;

public class TestInlineCase_Fix {
    private static final String TAG = "iWatch.TestInlineCase_Fix";

    public static void fix() {
//        try {
//            HookManager.get().fixMethod(
//                    TestInlineCase.class, "getIx", null,
//                    TestInlineCase_Fix.class, "getIx", null);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    public static int getIx() {
        Log.i(TAG, "getIx=" + 100);
        return 100;
    }
}
