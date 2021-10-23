package com.example.myvideoviewer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuItemCompat;

import android.app.SearchManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.myvideoviewer.contents.DetailActivity;
import com.example.myvideoviewer.contents.VRController;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.UUID;

public class WebViewActivity extends AppCompatActivity implements VRController.Listener {

    private static final String TAG = "WebViewActivityTAG";
    private static final String BOOK_MARK = "BookMark";
    private static final String PREF_NAME = "WebViewActivity";

    private VRController vrCtrl;
    private Menu optionMenu;
    private SearchView mSearchView;
    private WebView mWebView;
    private ArrayAdapter mAdapter;
    private ArrayList<String> mSpinner = new ArrayList<>();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        optionMenu = menu;

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchView = (SearchView) menu.findItem(R.id.search).getActionView();
        mSearchView.setIconified(false);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

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
        SharedPreferences pref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        mSpinner = toArray(pref.getString(BOOK_MARK, null));
        mAdapter = new ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, mSpinner);
        MenuItem item = menu.findItem(R.id.favorite);
        Spinner spinner = (Spinner) MenuItemCompat.getActionView(item);
        spinner.setAdapter(mAdapter);
        spinner.setSelection(0, false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d("WebViewActivityTag", "onItemSelected " + position);
                if (position == 0) {
                    String url = mWebView.getUrl();
                    mSpinner.add(url);
                    SharedPreferences pref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                    pref.edit().putString(BOOK_MARK, toJson(mSpinner)).commit();
                    mAdapter.clear();
                    mAdapter.addAll(mSpinner);
                    mAdapter.notifyDataSetChanged();
                } else {
                    String url = mSpinner.get(position);
                    mWebView.loadUrl(url);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d("WebViewActivityTag", "onNothingSelected");
            }
        });
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);


        mWebView = findViewById(R.id.webView);
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.setWebViewClient(new WebViewClient());
        mWebView.setWebContentsDebuggingEnabled(true);
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
//        mWebView.loadUrl("https://google.co.kr");
        mWebView.loadUrl("https://blacktoon130.com");

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

    private String toJson(ArrayList<String> arr){
        JSONArray json = new JSONArray();
        for(String item : arr) {
            json.put(item);
        }
        return json.toString();
    }

    private ArrayList toArray(String json) {
        ArrayList<String> arr = new ArrayList<>();
        if (json == null) {
            arr.add("추가");
        } else {
            try {
                JSONArray array = new JSONArray(json);
                for(int i=0;i<array.length();i++) {
                    arr.add(array.getString(i));
                }
            } catch (JSONException e) {}
        }
        Log.d("WebViewActivityTag", arr.toString());
        return arr;
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