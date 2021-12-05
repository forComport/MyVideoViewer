package com.example.myvideoviewer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.SearchView;

import com.example.myvideoviewer.contents.VRController;

import java.io.IOException;

public class WebViewActivity extends AppCompatActivity implements VRController.Listener {

    private static final String TAG = "WebViewActivityTAG";
    private static final String PrefName = "WebViewActivity";
    private static final String KEY_LAST_URL = "last_url";

    private VRController vrCtrl;
    private WebView mWebView;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mWebView.loadUrl(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.favorite) {
            SharedPreferences pref = getSharedPreferences(PrefName, Context.MODE_PRIVATE);
            pref.edit().putString(KEY_LAST_URL, mWebView.getUrl()).commit();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);


        mWebView = findViewById(R.id.webView);
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Log.d(TAG, request.getUrl().toString());
                return super.shouldOverrideUrlLoading(view, request);
            }

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.endsWith(".gif") || url.startsWith("https://1.bp.blogspot.com/")) {
                    Log.d(TAG, url);
                    try {
                        return new WebResourceResponse("", "", getAssets().open("blank.png"));
                    } catch (IOException e) {}
                }
                return super.shouldInterceptRequest(view, request);
            }
        });
        mWebView.setWebContentsDebuggingEnabled(true);
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        SharedPreferences pref = getSharedPreferences(PrefName, Context.MODE_PRIVATE);
        String url = pref.getString(KEY_LAST_URL, null);
        if(url == null) {
            mWebView.loadUrl("https://google.co.kr");
        } else {
            mWebView.loadUrl(url);
        }

        vrCtrl = new VRController(this);
        vrCtrl.setListener(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        vrCtrl.destroy();
        super.onDestroy();
    }

    @Override
    public void onVolumeUp() {

    }

    @Override
    public void onVolumeDown() {

    }

    @Override
    public void onPadPress(int x, int y) {
        Log.d(TAG, "onPadPress : "+ x +", " + y);
        int offsetY = mWebView.getScrollY();
        if (y > 150) {
            mWebView.scrollTo(0, offsetY +500);
        } else {
            mWebView.scrollTo(0, offsetY - 500);
        }
    }

    @Override
    public void onBackPress() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        }
    }

    @Override
    public void onHomePress() {
        runOnUiThread(()-> {
            mWebView.evaluateJavascript("(function() { page_nexts.click(); return 'this'; })();",null);
        });
    }

    @Override
    public void onTrigger() {

    }
}