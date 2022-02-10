package com.example.myvideoviewer.provider;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.example.myvideoviewer.contents.ContentsItem;
import com.example.myvideoviewer.contents.ContentsLoader;

import java.util.ArrayList;

public class LibraryLoader extends ContentsLoader {

    public static final String KEY = "Library";
    private DownloadManager mDownloadManager;
    private boolean loaded = false;

    @Override
    public void init() {
        loaded = false;
        mDownloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    @Override
    public void search(String keyword) {

    }

    @Override
    public void loadList() {
        if (loaded) return;
        Cursor cursor = mDownloadManager.query(new DownloadManager.Query());
        int columnId = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID);
        int columnTitle = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE);
        int columnLocalUri = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI);
        int columnStatus = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS);
        ArrayList<ContentsItem> arr = new ArrayList<>();
        while(cursor.moveToNext()) {
            String meta ="";
            if (cursor.getInt(columnStatus) == DownloadManager.STATUS_SUCCESSFUL) {
                meta = "완료";
            } else if (cursor.getInt(columnStatus) == DownloadManager.STATUS_RUNNING) {
                meta = "다운로드중";
            } else if (cursor.getInt(columnStatus) == DownloadManager.STATUS_PENDING) {
                meta = "대기중";
            } else if (cursor.getInt(columnStatus) == DownloadManager.STATUS_PAUSED) {
                meta = "일시정지";
            } else if (cursor.getInt(columnStatus) == DownloadManager.STATUS_FAILED) {
                meta = "실패";
            }
            ContentsItem item = new ContentsItem("file", cursor.getString(columnLocalUri), cursor.getString(columnTitle), meta);
            item.id = cursor.getInt(columnId);
            arr.add(item);
        }
        listListener.onListLoad(arr);
        loaded = true;
    }

    @Override
    public void loadDetail(ContentsItem item) {
        detailListener.onVideoLoad(item.pageUrl);
    }

    @Override
    public void onLongClick(ContentsItem item) {

    }
}
