package com.habbyge.iwatch.cgi;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;

/**
 * Created by habbyge on 2021/1/5.
 */
public final class PatchDownloder {

    void request(Context context, String url, IPatchDownloadCallback callback) {
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

        // TODO: 2021/3/28 ing......
//        request.setDestinationInExternalPublicDir("itbox","zongshu.mp4");
//        request.setDestinationInExternalFilesDir();

        // 表示不显示任何通知栏提示
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);

        // 表示下载允许的网络类型
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);

        long downloadId = dm.enqueue(request);
    }
}
