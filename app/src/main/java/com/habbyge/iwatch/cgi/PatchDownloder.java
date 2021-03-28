package com.habbyge.iwatch.cgi;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.util.Log;

import com.habbyge.iwatch.patch.PatchManager;

import java.io.File;
import java.io.IOException;

/**
 * Created by habbyge on 2021/1/5.
 */
public final class PatchDownloder {
    private static final String TAG = "iWatch.PatchDownloder";

    private static final String DOWNLOAD_FOLDER_NAME = "iwatch";
    public static final String DOWNLOAD_FILE_NAME = "mm_iwatch.apatch";

    private long downloadId = 0L;

    public void request(Context context, String url) {
        File f = context.getExternalFilesDir(DOWNLOAD_FOLDER_NAME);
        if (!f.exists() || !f.isDirectory()) {
            f.mkdirs();
        }

        context.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

        request.setDestinationInExternalPublicDir(DOWNLOAD_FOLDER_NAME, DOWNLOAD_FILE_NAME);

        // 表示不显示任何通知栏提示
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);

        // 表示下载允许的网络类型
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);

        downloadId = dm.enqueue(request);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            long completeDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (completeDownloadId == downloadId) {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    String patchFilePath = context.getExternalFilesDir(DOWNLOAD_FOLDER_NAME).getAbsolutePath()
                            + File.separator + DOWNLOAD_FILE_NAME;

                    try { // TODO: 2021/3/29 ing......
                        boolean addPatchRet = PatchManager.getInstance().addPatch(patchFilePath, true);
                        Log.i(TAG, "onReceive, patchFilePath= " + patchFilePath +
                                ", downloadId=" + downloadId +
                                ", addPatchRet=" + addPatchRet);
                    } catch (IOException e) {
                        Log.e(TAG, "BroadcastReceiver, exception: " + e.getMessage());
                    }
                }
            }
        }
    };
}
