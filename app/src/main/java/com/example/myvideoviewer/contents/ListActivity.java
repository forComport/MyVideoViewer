package com.example.myvideoviewer.contents;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DownloadManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.myvideoviewer.R;

import java.util.ArrayList;
import java.util.Map;

public class ListActivity extends AppCompatActivity implements AbsListView.OnScrollListener,
        ContentsAdapter.Listener, ContentsLoader.ListListener {
    private static final String TAG = "ListActivityTAG";
    private ContentsLoader loader;
    private ContentsAdapter adapter = new ContentsAdapter();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jav247);

        String provider = getIntent().getStringExtra("provider");
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(provider);
        }
        loader = ContentsLoader.Provider.get(provider).setContext(this);
        loader.init();
        loader.setOnListListener(this);
        ListView listView = findViewById(R.id.jav247_list);
        listView.setAdapter(adapter);
        listView.setOnScrollListener(this);
        adapter.setOnLinkListener(this);
//        loader.loadList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.clear();
                loader.search(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

//    @Override
//    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//        int id = item.getItemId();
//        if (id == R.id.search) {
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        int lastItem = firstVisibleItem + visibleItemCount;
        if (lastItem == totalItemCount && !loader.loading) {
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            loader.loadList();
        }
    }

    @Override
    public void onClick(ContentsItem item) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("item", item);
        intent.putExtra("provider", getIntent().getStringExtra("provider"));
        startActivity(intent);
    }

    @Override
    public void onLongClick(ContentsItem item) {
        loader.setOnDetailListener(new ContentsLoader.DetailListener() {
            @Override
            public void onVideoLoad(String url, Map<String, String> headers) {
                DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (url == null || url.startsWith("file://")) {
                    downloadManager.remove(item.id);
                    Toast.makeText(getApplicationContext(), "삭제완료", Toast.LENGTH_LONG).show();
                } else {
                    Uri uri = Uri.parse(url);
                    DownloadManager.Request request = new DownloadManager.Request(uri);
                    request.setTitle(item.title);
                    request.setDescription("다운로드중...");
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
                    request.setDestinationInExternalFilesDir(ListActivity.this, Environment.DIRECTORY_DOWNLOADS,
                            uri.getLastPathSegment());
                    downloadManager.enqueue(request);
                    Toast.makeText(getApplicationContext(), "다운로드 시작", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onRelativeListLoad(ArrayList<ContentsItem> items) {

            }
        });
        loader.loadDetail(item);
    }

    @Override
    public void onListLoad(ArrayList<ContentsItem> items) {
        for(ContentsItem item : items) {
            adapter.add(item);
        }
        adapter.notifyDataSetChanged();
        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
    }


}