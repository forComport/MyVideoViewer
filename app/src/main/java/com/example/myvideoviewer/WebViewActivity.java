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
import android.content.Context;
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
    private final static UUID SERVICE_UUID = UUID.fromString("4f63756c-7573-2054-6872-65656d6f7465");
    private final static UUID WRITE_UUID = UUID.fromString("c8c51726-81bc-483b-a052-f7a14ea3d282");
    private final static UUID NOTIFY_UUID = UUID.fromString("c8c51726-81bc-483b-a052-f7a14ea3d281");
    private final static UUID NOTIFY_ENABLE_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID Battery_Service_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    private static final UUID Battery_Level_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");

    private BluetoothGatt gatt;
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

        vrCtrl = new VRController();
        vrCtrl.setListener(this);
        connectVrController();
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
        gatt.disconnect();
        gatt.close();
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

    private void connectVrController() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
        for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices()) {
            if ("Gear VR Controller(E8B8)".equals(device.getName())) {
                Log.d(TAG, "connectVrController");
                gatt = device.connectGatt(this, true, new BluetoothGattCallback() {

                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            gatt.discoverServices();
                        }
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        Log.d(TAG, "onServicesDiscovered");
                        BluetoothGattService batteryService = gatt.getService(Battery_Service_UUID);
                        BluetoothGattCharacteristic batteryLevel = batteryService.getCharacteristic(Battery_Level_UUID);
                        BluetoothGattDescriptor batterDesc = batteryLevel.getDescriptor(NOTIFY_ENABLE_UUID);
                        batterDesc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        boolean batterySuccess = gatt.readCharacteristic(batteryLevel);
                        Log.d(TAG, "battery read - " + batterySuccess);
                    }

                    @Override
                    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        Log.d(TAG, "onCharacteristicRead");
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            int battery = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                            runOnUiThread(()-> {
                                Toast.makeText(WebViewActivity.this, "VR Ctrl 배터리 " + battery + "%", Toast.LENGTH_SHORT).show();
                            });

                            BluetoothGattService service = gatt.getService(SERVICE_UUID);
                            BluetoothGattCharacteristic writeChar = service.getCharacteristic(WRITE_UUID);
                            writeChar.setValue(new byte[]{0x01,0x00});
                            boolean success = gatt.writeCharacteristic(writeChar);
                            Log.d(TAG, "write - " + success);
                        } else {
                            Log.d(TAG, "onCharacteristicRead - " + status);
                        }
                    }

                    @Override
                    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        Log.d(TAG, "onCharacteristicWrite");
                        gatt.executeReliableWrite();

                        BluetoothGattService service = gatt.getService(SERVICE_UUID);
                        BluetoothGattCharacteristic notifyChar = service.getCharacteristic(NOTIFY_UUID);
                        BluetoothGattDescriptor desc = notifyChar.getDescriptor(NOTIFY_ENABLE_UUID);
                        desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(desc);
                        boolean r = gatt.setCharacteristicNotification(notifyChar, true);
                        Log.d(TAG, "setCharacteristicNotification " + r);
                    }

                    @Override
                    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                        vrCtrl.receiveData(characteristic.getValue());
                    }
                });
            }
        }
    }

    @Override
    public void onVolumeUp() {

    }

    @Override
    public void onVolumeDown() {

    }

    @Override
    public void onPadPress(int x, int y) {
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