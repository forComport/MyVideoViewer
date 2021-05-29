package com.example.myservice;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;

public class LibraryActivity extends AppCompatActivity {

    LibraryListAdapter mAdapter;
    private DownloadManager mDownloadManager;
    private VRControllerReceiver receiver;
    int cursor = 0;
    ListView listview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        mDownloadManager = (DownloadManager) this.getSystemService(Context.DOWNLOAD_SERVICE);
        mAdapter = new LibraryListAdapter(this);

        listview = findViewById(R.id.library_list);
        listview.setAdapter(mAdapter);
        Cursor cursor = mDownloadManager.query(new DownloadManager.Query());
        mAdapter.loadList(cursor);

        IntentFilter filter = new IntentFilter();
        filter.addAction(VRControllerReceiver.ACTION_DATA_EVENT);
        filter.addAction(VRControllerReceiver.ACTION_ACTIVE);
        filter.addAction(VRControllerReceiver.ACTION_INACTIVE);
        receiver = new VRControllerReceiver(this, "LibraryActivity");
        registerReceiver(receiver, filter);
        Intent intent = new Intent(VRControllerReceiver.ACTION_ACTIVE);
        intent.putExtra("activity", "LibraryActivity");
        sendBroadcast(intent);
    }

    @Override
    protected void onDestroy() {
        Intent intent = new Intent(VRControllerReceiver.ACTION_INACTIVE);
        sendBroadcast(intent);
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    public static class LibraryListAdapter extends BaseAdapter {

        private LibraryActivity activity;
        private final ArrayList<LibraryItem> itemList = new ArrayList<>();

        public LibraryListAdapter(LibraryActivity activity) {
            this.activity = activity;
        }


        public void loadList(Cursor cursor) {
            itemList.clear();
            int columnId = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID);
            int columnTitle = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE);
            int columnStatus = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS);
            while (cursor.moveToNext()) {
                LibraryItem item = new LibraryItem();
                item.id = cursor.getLong(columnId);
                item.title = cursor.getString(columnTitle);
                item.status = cursor.getInt(columnStatus);
                itemList.add(item);
            }
        }

        @Override
        public int getCount() {
            return itemList.size();
        }

        @Override
        public Object getItem(int position) {
            return itemList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) parent.getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.library_list_item, parent, false);
            }
            if (activity.cursor == position) {
                convertView.setBackground(activity.getDrawable(android.R.color.darker_gray));
            } else {
                convertView.setBackground(activity.getDrawable(R.color.white));
            }
            ImageView thumbnail = convertView.findViewById(R.id.item_thumbnail);
            TextView title = convertView.findViewById(R.id.item_title);
            TextView desc = convertView.findViewById(R.id.item_desc);
            Button actionButton = convertView.findViewById(R.id.item_action);
            Button deleteButton = convertView.findViewById(R.id.item_delete);

            LibraryItem item = itemList.get(position);
            File file = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),item.title);
            Glide.with(convertView)
                    .load(file)
                    .into(thumbnail);
            title.setText(item.title);
            actionButton.setText("시청");
            switch (item.status){
                case DownloadManager.STATUS_SUCCESSFUL:
                    desc.setText("다운 완료");
                    actionButton.setEnabled(true);
                    break;
                case DownloadManager.STATUS_RUNNING:
                case DownloadManager.STATUS_PENDING:
                case DownloadManager.STATUS_PAUSED:
                    desc.setText("다운로드 대기중...");
                    actionButton.setEnabled(false);
                    break;
                case DownloadManager.STATUS_FAILED:
                    desc.setText("실패");
                    actionButton.setEnabled(false);
                    break;
                default:
                    break;
            }

            actionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(activity, VideoActivity.class);
                    intent.putExtra("filename", item.title);
                    activity.startActivity(intent);
                }
            });
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Light_Dialog);
                    dialog.setMessage("영상을 삭제하시겠습니까?")
                            .setTitle("삭제 확인")
                            .setPositiveButton("네", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    activity.mDownloadManager.remove(item.id);
                                    itemList.remove(position);
                                    notifyDataSetChanged();
                                }
                            })
                            .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .setCancelable(true)
                            .show();
                }
            });
            return convertView;
        }
    }

    public static class LibraryItem {
        long id;
        String title;
        int status;
    }
}