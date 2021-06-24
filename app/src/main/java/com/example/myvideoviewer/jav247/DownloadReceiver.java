package com.example.myvideoviewer.jav247;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

public class DownloadReceiver extends BroadcastReceiver {
    private static final String TAG = "DownloadReceiverTAG";
    @Override
    public void onReceive(Context context, Intent intent) {

        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

            Log.d(TAG, intent.getAction() + " " + id);
            Jav247DbHelper dbHelper = new Jav247DbHelper(context);
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(id);
            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            Cursor c = dm.query(query);
            if (c.moveToFirst()) {
                String title = c.getString(c.getColumnIndex(DownloadManager.COLUMN_TITLE));
                dbHelper.update(title, "삭제");
            }
        }
    }
}
