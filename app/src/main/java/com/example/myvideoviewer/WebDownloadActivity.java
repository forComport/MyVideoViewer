package com.example.myvideoviewer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class WebDownloadActivity extends AppCompatActivity {

    private DownloadListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_download);
        ArrayList<String> videoUrls = (ArrayList<String>) getIntent().getSerializableExtra("videoUrls");
        mAdapter = new DownloadListAdapter();
        ListView listview = findViewById(R.id.download_list);
        listview.setAdapter(mAdapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String url = videoUrls.get(position);
                Uri uri = Uri.parse(url);
                DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                DownloadManager.Request request = new DownloadManager.Request(uri);
                String title = getIntent().getStringExtra("title");
                request.setTitle(title);
                request.setDescription("다운로드중...");
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
                request.setDestinationInExternalFilesDir(WebDownloadActivity.this, Environment.DIRECTORY_DOWNLOADS,
                        uri.getLastPathSegment());
                downloadManager.enqueue(request);
                Toast.makeText(getApplicationContext(), "다운로드 시작", Toast.LENGTH_LONG).show();
            }
        });



        for(String url:videoUrls) {
            mAdapter.addItem(url);
        }
    }

    public static class DownloadListAdapter extends BaseAdapter {

        private ArrayList<DownloadItem> itemList = new ArrayList<>();

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
                convertView = inflater.inflate(R.layout.download_list_item, parent, false);
            }
            ImageView thumbnail = convertView.findViewById(R.id.item_thumbnail);
            TextView title = convertView.findViewById(R.id.item_title);
            TextView desc = convertView.findViewById(R.id.item_desc);

            DownloadItem item = itemList.get(position);
            Glide.with(convertView).load(item.url)
                    .placeholder(R.drawable.default_image)
                    .into(thumbnail);
            title.setText(item.title);
            item.getFileSize(desc);

            return convertView;
        }

        public void addItem(String url) {
            itemList.add(new DownloadItem(url));
        }
    }

    public static class DownloadItem {
        private String url;
        private String title;

        public DownloadItem(String url) {
            this.url = url;
            this.title = Uri.parse(url).getLastPathSegment();
        }


        public void getFileSize(TextView view) {
            Handler handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    int fileSize = msg.what;
                    float size = fileSize / (1024f * 1024f);
                    DecimalFormat decimalFormat = new DecimalFormat("#0.00");
                    view.setText(decimalFormat.format(size) + "MB");
                }
            };
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL mUrl = new URL(url);
                        URLConnection urlConnection = mUrl.openConnection();
                        urlConnection.connect();
                        int fileSize = urlConnection.getContentLength();
                        handler.sendEmptyMessage(fileSize);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
}