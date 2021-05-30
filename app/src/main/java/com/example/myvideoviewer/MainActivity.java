package com.example.myvideoviewer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivityTAG";

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    private static final int BT_REQUEST_ENABLE = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private final static UUID SERVICE_UUID = UUID.fromString("4f63756c-7573-2054-6872-65656d6f7465");
    private final static UUID WRITE_UUID = UUID.fromString("c8c51726-81bc-483b-a052-f7a14ea3d282");
    private final static UUID NOTIFY_UUID = UUID.fromString("c8c51726-81bc-483b-a052-f7a14ea3d281");
    private final static UUID NOTIFY_ENABLE_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID Battery_Service_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    private static final UUID Battery_Level_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");

    private Button bluetoothButton;
    private TextView bluetoothBattery;
    private BluetoothGatt gatt;
    private boolean connecting = false;
    private BluetoothGattCharacteristic writeChar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkAndRequestPermission();
        registerButtonEvent();

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(ACTION_GATT_CONNECTED);
        filter.addAction(ACTION_GATT_DISCONNECTED);
        filter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
        filter.addAction(ACTION_DATA_AVAILABLE);
        filter.addAction(EXTRA_DATA);
        registerReceiver(receiver, filter);
    }

    private void checkAndRequestPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
        )  {
            requestPermissions(
                    new String[] {
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    0);
        }
    }

    private void registerButtonEvent() {
        findViewById(R.id.move_to_web).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), WebSearchActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.move_to_library).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LibraryActivity.class);
                startActivity(intent);
            }
        });
        bluetoothBattery = findViewById(R.id.bluetooth_battery);
        bluetoothButton = findViewById(R.id.bluetooth);
        bluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent intentBluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intentBluetoothEnable, BT_REQUEST_ENABLE);
                } else {
                    searchDevice();
                }
            }
        });
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        long discovered = 0;
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if ("Gear VR Controller(E8B8)".equals(device.getName())) {
                    if (System.currentTimeMillis() > discovered + 10*1000) {
                        discovered = System.currentTimeMillis();
                        mBluetoothAdapter.cancelDiscovery();
                        connectDevice(device);
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                bluetoothButton.setEnabled(false);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                bluetoothButton.setEnabled(true);
            } else if (ACTION_GATT_CONNECTED.equals(action)) {
                bluetoothButton.setEnabled(false);
                gatt.discoverServices();
            } else if (ACTION_GATT_DISCONNECTED.equals(action)) {
                bluetoothButton.setEnabled(true);
                Toast.makeText(getApplicationContext(), "VR 컨트롤러와 연결이 끊어졌습니다.", Toast.LENGTH_LONG).show();
            } else if (ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                writeChar = service.getCharacteristic(WRITE_UUID);
                BluetoothGattCharacteristic notifyChar = service.getCharacteristic(NOTIFY_UUID);
                BluetoothGattDescriptor desc = notifyChar.getDescriptor(NOTIFY_ENABLE_UUID);
                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(desc);
                boolean r = gatt.setCharacteristicNotification(notifyChar, true);
                Log.d(TAG, "setCharacteristicNotification " + r);
                try{
                    Thread.sleep(500);
                    writeChar.setValue(new byte[]{0x01,0x00});
                    gatt.writeCharacteristic(writeChar);
                } catch (Exception e) {}

                try{
                    Thread.sleep(500);
                    BluetoothGattService batteryService = gatt.getService(Battery_Service_UUID);
                    BluetoothGattCharacteristic batteryLevel = batteryService.getCharacteristic(Battery_Level_UUID);
                    BluetoothGattDescriptor batterDesc = batteryLevel.getDescriptor(NOTIFY_ENABLE_UUID);
                    batterDesc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    boolean batterySuccess = gatt.readCharacteristic(batteryLevel);
                Log.d(TAG, "battery read - " + batterySuccess);
                } catch (Exception e) {}
            } else if (EXTRA_DATA.equals(action)) {
                int battery = intent.getIntExtra("device_battery",0);
                bluetoothBattery.setText("컨트롤러 배터리 : " + battery + "%");
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case BT_REQUEST_ENABLE:
                if (requestCode == RESULT_OK) {
                    searchDevice();
                } else {
                    Toast.makeText(this, "블루투스 활성화 실패", Toast.LENGTH_LONG).show();
                }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void searchDevice() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if ("Gear VR Controller(E8B8)".equals(device.getName())) {
                    connectDevice(device);
                }
            }
        } else {
            boolean result = mBluetoothAdapter.startDiscovery();
            Log.d(TAG, "discover - "+ result);
        }
    }

    private void connectDevice(BluetoothDevice device) {
        Log.d(TAG, "connectDevice");
        if (!connecting) {
            connecting = true;
            gatt = device.connectGatt(this, false, new BluetoothGattCallback() {

                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    connecting = false;
                    Log.d(TAG, "onConnectionStateChange - " + newState);
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        broadcastUpdate(ACTION_GATT_CONNECTED);
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        broadcastUpdate(ACTION_GATT_DISCONNECTED);
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                    } else {
                        Log.w(TAG, "onServicesDiscovered received: " + status);
                    }
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//                    super.onCharacteristicRead(gatt, characteristic, status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(EXTRA_DATA, characteristic);
                    } else {
                        Log.d(TAG, "onCharacteristicRead - " + status);
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    final Intent intent = new Intent(VRControllerReceiver.ACTION_DATA_EVENT);
                    intent.putExtra("data", characteristic.getValue());
                    sendBroadcast(intent);
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    StringBuilder builder = new StringBuilder();
                    for(byte b : characteristic.getValue()) {
                        builder.append(b);
                    }
                    String code = builder.toString();
                    boolean r = gatt.executeReliableWrite();

                    Log.d(TAG, "onCharacteristicWrite - "+ status + " " + code + " " + r);
                }
            });
        }
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        intent.putExtra("device_battery", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
        sendBroadcast(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        if (gatt != null) {
            gatt.close();
            gatt = null;
        }
    }
}