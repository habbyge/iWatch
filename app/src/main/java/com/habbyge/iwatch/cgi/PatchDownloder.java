package com.habbyge.iwatch.cgi;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.util.Log;

/**
 * Created by habbyge on 2021/1/5.
 */
public final class PatchDownloder {
    private static final String TAG = "iWatch.PatchDownloder";

    private long downloadId = 0L;
    private IPatchDownloadCallback callback;

    public PatchDownloder(IPatchDownloadCallback callback) {
        this.callback = callback;
    }

    public void request(Context context, String url) {
        context.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

        // TODO: 2021/3/28 ing......
//        request.setDestinationInExternalPublicDir("itbox","zongshu.mp4");
//        request.setDestinationInExternalFilesDir();

        // 表示不显示任何通知栏提示
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);

        // 表示下载允许的网络类型
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);

        downloadId = dm.enqueue(request);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                Log.i(TAG, "onReceive, action = " + action);
                if (callback != null) {
                    callback.onCallback(); // TODO: 2021/3/28
                }
            }
        }
    };
}
