package com.example.myservice;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WebSearchActivity extends AppCompatActivity {

    private static final String TAG = "WebSearchActivity-TAG";

    private SearchView mSearchView;
    private WebView mWebView;
    private ImageButton mImageButton;
    private MyCursorAdapter mAdapter;
    private Menu optionMenu;
    private ProgressBar mProgressBar;
    private DbHelper mDb;

    private static final List<String> BLACKLIST = Arrays.asList(
            "bowerywill.com", "play-vids.com"
    );
    private static final String[] SUPPORT_EXT = new String[] {
            "avi", "wmv", "mp4", "mkv", "flv"
    };
    private ArrayList<String> videoUrls = new ArrayList<>();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        optionMenu = menu;

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchView = (SearchView) menu.findItem(R.id.search).getActionView();
        mSearchView.setIconified(false);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        mAdapter = new MyCursorAdapter(this);
        mSearchView.setSuggestionsAdapter(mAdapter);

        mDb = new DbHelper(this);
        mAdapter.changeCursor(remake(mDb.readFavorite()));

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mWebView.loadUrl(query);
                menu.findItem(R.id.search).collapseActionView();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.favorite:
                mDb.insertFavorite(mWebView.getUrl());
                mAdapter.changeCursor(remake(mDb.readFavorite()));
                Toast.makeText(this, "현재 페이지를 북마크에 추가했습니다.", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_search);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mProgressBar = findViewById(R.id.progress);
        mImageButton = findViewById(R.id.download_button);
        mImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!videoUrls.isEmpty()) {
                    Intent i = new Intent(getApplicationContext(), WebDownloadActivity.class);
                    i.putExtra("videoUrls", videoUrls);
                    startActivity(i);
                }
            }
        });
        mWebView = findViewById(R.id.webview);
        mWebView.setWebContentsDebuggingEnabled(true);
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setLoadWithOverviewMode(false);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                mSearchView.setQuery(mWebView.getUrl(),false);
                mProgressBar.setVisibility(View.VISIBLE);
                videoUrls.clear();
                mImageButton.setBackground(getDrawable(R.drawable.circle_button));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                getSupportActionBar().setTitle(mWebView.getTitle());
                mProgressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if(BLACKLIST.contains(request.getUrl().getHost())) {
                    return true;
                }
                Log.d(TAG, "shouldOverrideUrlLoading : " + request.getUrl().getHost());
                return false;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String path = request.getUrl().getPath();
                for (String ext : SUPPORT_EXT) {
                    if (path != null && path.endsWith("." + ext)) {
                        videoUrls.add(request.getUrl().toString());
                        mImageButton.setBackground(getDrawable(R.drawable.circle_button_active));
                        break;
                    }
                }
                return null;
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                mProgressBar.setProgress(newProgress);
            }
        });
        mWebView.loadUrl("https://naver.com");
    }

    @Override
    public void onDestroy() {
        mDb.close();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if(mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private Cursor remake(Cursor cursor) {
        final MatrixCursor c = new MatrixCursor(new String[]{BaseColumns._ID, ""});
        cursor.moveToFirst();
        while (cursor.moveToNext()) {
            c.addRow(new Object[]{
                    cursor.getInt(0),
                    cursor.getString(1)});
        }
        return c;
    }

    static public class MyCursorAdapter extends SimpleCursorAdapter {

        WebSearchActivity activity;

        public MyCursorAdapter(WebSearchActivity context) {
            super(context, R.layout.suggestion_list_item, null,
                new String[] {""},
                new int[] {R.id.item_label},
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            this.activity = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup group) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(activity);
                convertView = inflater.inflate(R.layout.suggestion_list_item, null);
            }
            convertView.setTag(position);
            return super.getView(position, convertView, group);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            super.bindView(view, context, cursor);
            view.findViewById(R.id.item_label).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = (Integer) view.getTag();
                    cursor.moveToPosition(position);
                    String url = cursor.getString(1);
                    activity.mWebView.loadUrl(url);
                    activity.mSearchView.clearFocus();
                    activity.optionMenu.findItem(R.id.search).collapseActionView();
                }
            });
            view.findViewById(R.id.item_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = (Integer) view.getTag();
                    cursor.moveToPosition(position);
                    String url = cursor.getString(1);
                    activity.mDb.deleteFavorite(url);
                    activity.mAdapter.changeCursor(activity.remake(activity.mDb.readFavorite()));
                }
            });
        }
    }
}