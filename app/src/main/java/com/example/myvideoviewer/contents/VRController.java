package com.example.myvideoviewer.contents;


import android.app.Activity;
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
import android.util.Log;
import android.widget.Toast;

import java.util.UUID;

public class VRController {
    private static final String TAG = "VRControllerTAG";
    private final static UUID SERVICE_UUID = UUID.fromString("4f63756c-7573-2054-6872-65656d6f7465");
    private final static UUID WRITE_UUID = UUID.fromString("c8c51726-81bc-483b-a052-f7a14ea3d282");
    private final static UUID NOTIFY_UUID = UUID.fromString("c8c51726-81bc-483b-a052-f7a14ea3d281");
    private final static UUID NOTIFY_ENABLE_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID Battery_Service_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    private static final UUID Battery_Level_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");

    boolean prevTriggerButton = false;
    boolean prevHomeButton = false;
    boolean prevBackButton = false;
    boolean prevTouchPadButton = false;
    boolean prevVolumeUpButton = false;
    boolean prevVolumeDownButton = false;
    private Activity context;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt gatt;
    public interface Listener {
        void onVolumeUp();
        void onVolumeDown();
        void onPadPress(int x, int y);
        void onBackPress();
        void onHomePress();
        void onTrigger();
    }
    private Listener listener;

    public VRController(Activity context) {
        this.context = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(receiver, filter);
        connectVrController();
    }

    public void destroy() {
        if (gatt != null) {
            gatt.disconnect();
            gatt.close();
        }
        context.unregisterReceiver(receiver);
    }

    private void connectVrController() {
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        boolean find = false;
        for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices()) {
            if ("Gear VR Controller(E8B8)".equals(device.getName())) {
                find = true;
                connectDevice(device);
            }
        }
        if (!find) {
            boolean result = mBluetoothAdapter.startDiscovery();
            Log.d(TAG, "discover - "+ result);
        }
    }

    private void connectDevice(BluetoothDevice device) {
        gatt = device.connectGatt(context, true, new BluetoothGattCallback() {

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                context.runOnUiThread(()-> {
                    BluetoothGattService service = gatt.getService(SERVICE_UUID);
                    BluetoothGattCharacteristic writeChar = service.getCharacteristic(WRITE_UUID);
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
                });
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Log.d(TAG, "onCharacteristicRead");
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    int battery = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    context.runOnUiThread(()-> {
                        Toast.makeText(context, "VR Ctrl 배터리 " + battery + "%", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    Log.d(TAG, "onCharacteristicRead - " + status);
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Log.d(TAG, "onCharacteristicWrite");
                gatt.executeReliableWrite();
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                receiveData(characteristic.getValue());
            }
        });
    }


    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, device.getName() + "");
                if ("Gear VR Controller(E8B8)".equals(device.getName())) {
//                    if (System.currentTimeMillis() > discovered + 10*1000) {
//                        discovered = System.currentTimeMillis();
                    mBluetoothAdapter.cancelDiscovery();
                    connectDevice(device);
//                        connectDevice(device);
//                    }
                }
            }
        }
    };

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void receiveData(byte[] bytes) {
        if (listener == null) return;
        int axisX = ((bytes[54] & 0xF) *64 + ((bytes[55] & 0xFC)/2)) & 0x3FF;
        int axisY = (((bytes[55] & 0x3) *256) + (bytes[56] & 0xFF)) & 0x3FF;
        boolean touched = (bytes[54] & 0x10) > 0;

        boolean triggerButton = (bytes[58] & (1 << 0)) > 0;
        boolean homeButton = (bytes[58] & (1 << 1)) > 0;
        boolean backButton = (bytes[58] & (1 << 2)) > 0;
        boolean touchPadButton = (bytes[58] & (1 << 3)) > 0;
        boolean volumeUpButton = (bytes[58] & (1 << 4)) > 0;
        boolean volumeDownButton = (bytes[58] & (1 << 5)) > 0;

        if(!prevVolumeDownButton && volumeDownButton) {
            listener.onVolumeDown();
        }
        prevVolumeDownButton = volumeDownButton;

        if(!prevVolumeUpButton && volumeUpButton) {
            listener.onVolumeUp();
        }
        prevVolumeUpButton = volumeUpButton;

        if(!prevTouchPadButton && touchPadButton && touched) {
            listener.onPadPress(axisX, axisY);
        }
        prevTouchPadButton = touchPadButton;

        if (!prevBackButton && backButton) {
            listener.onBackPress();
        }
        prevBackButton = backButton;

        if (!prevHomeButton && homeButton) {
            listener.onHomePress();
        }
        prevHomeButton = homeButton;

        if (!prevTriggerButton && triggerButton) {
            listener.onTrigger();
        }
        prevTriggerButton = triggerButton;
    }

}
