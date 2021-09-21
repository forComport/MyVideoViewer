package com.example.myvideoviewer.contents;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.example.myvideoviewer.R;
import com.example.myvideoviewer.provider.YoutubeLoader;

import java.util.ArrayList;
import java.util.UUID;

public class DetailActivity extends AppCompatActivity implements ContentsLoader.DetailListener, VRController.Listener {
    private static final String TAG = "DetailActivityTAG";
    private final static UUID SERVICE_UUID = UUID.fromString("4f63756c-7573-2054-6872-65656d6f7465");
    private final static UUID WRITE_UUID = UUID.fromString("c8c51726-81bc-483b-a052-f7a14ea3d282");
    private final static UUID NOTIFY_UUID = UUID.fromString("c8c51726-81bc-483b-a052-f7a14ea3d281");
    private final static UUID NOTIFY_ENABLE_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID Battery_Service_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    private static final UUID Battery_Level_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");

    private VideoView videoView;
    private MediaController mediaCtrl;
    private BluetoothGatt gatt;
    private VRController vrCtrl;
    private ArrayList<ContentsItem> RelativeVideos = new ArrayList<>();

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

//        View decorView = getWindow().getDecorView();
//        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_FULLSCREEN;
//        decorView.setSystemUiVisibility(uiOptions);
        getWindow().setNavigationBarColor(R.color.black);
        videoView = findViewById(R.id.video);
        mediaCtrl = new MediaController(this);
        videoView.setMediaController(mediaCtrl);
        videoView.setOnPreparedListener((v)->{
            int width = videoView.getMeasuredWidth();
            int height = videoView.getMeasuredHeight();
            if (height > width) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            videoView.start();
            findViewById(R.id.progressBar).setVisibility(View.GONE);
        });

        Intent intent = getIntent();
        String provider = intent.getStringExtra("provider");
        ContentsItem item;
        ContentsLoader loader;
        if (provider == null) {
            String url = intent.getStringExtra(Intent.EXTRA_TEXT);
            YoutubeLoader youtubeLoader = new YoutubeLoader();
            loader = youtubeLoader.setContext(this);
            loader.init();
            item = youtubeLoader.makeItem(url);
        } else {
            item = (ContentsItem) intent.getParcelableExtra("item");
            loader = ContentsLoader.Provider.get(provider).setContext(this);
        }
        loader.setOnDetailListener(this);
        loader.loadDetail(item);

        vrCtrl = new VRController();
        vrCtrl.setListener(this);

        connectVrController();
        registerButton();
    }

    private void registerButton() {
        findViewById(R.id.btn_alarm).setOnClickListener((v)->{
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("자동 종료 알람");
            final EditText edittext = new EditText(this);
            builder.setView(edittext);
            builder.setPositiveButton("설정", (dialog, id)->{
                try {
                    int minute = Integer.parseInt(String.valueOf(edittext.getText()));
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(()->{
                        System.exit(0);
                    }, minute * 60*1000);
                    Toast.makeText(getApplicationContext(), minute+"분 후 종료", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                }
                dialog.dismiss();
            });
            builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        });

        findViewById(R.id.btn_series).setOnClickListener((v)->{
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("관련 영상 선택");
            builder.setPositiveButton("닫기", (dialog, id)->{
                dialog.dismiss();
            });
            HorizontalScrollView scrollView = new HorizontalScrollView(v.getContext());
            LinearLayout layout = new LinearLayout(v.getContext());
            for(ContentsItem item : RelativeVideos) {
                LinearLayout itemLayout = new LinearLayout(v.getContext());
                itemLayout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(15,5,15,5);
                itemLayout.setLayoutParams(layoutParams);

                TextView textView = new TextView(v.getContext());
                textView.setText(item.title);
                textView.setTextSize(10);
                ImageView imageView = new ImageView(v.getContext());
                imageView.setMaxHeight(400);
                imageView.setMinimumHeight(400);
                Glide.with(v.getContext())
                        .load(item.thumbnail)
                        .into(imageView);
                itemLayout.addView(imageView);
                itemLayout.addView(textView);
                itemLayout.setOnClickListener((vv)->{
                    Intent intent = new Intent(this, DetailActivity.class);
                    intent.putExtra("item", item);
                    intent.putExtra("provider", getIntent().getStringExtra("provider"));
                    startActivity(intent);
                    finish();
                });
                layout.addView(itemLayout);
            }
            scrollView.addView(layout);
            builder.setView(scrollView);
            AlertDialog dialog = builder.create();
            dialog.show();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null) {
            videoView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        gatt.disconnect();
        gatt.close();
        videoView.stopPlayback();
        super.onDestroy();
    }

    @Override
    public void onVideoLoad(String url) {
        Log.d(TAG, url);
        videoView.setVideoURI(Uri.parse(url));
        videoView.requestFocus();
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
                                Toast.makeText(DetailActivity.this, "VR Ctrl 배터리 " + battery + "%", Toast.LENGTH_SHORT).show();
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
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        audio.setStreamVolume(AudioManager.STREAM_MUSIC,volume+1, AudioManager.FLAG_PLAY_SOUND);
    }

    @Override
    public void onVolumeDown() {
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        audio.setStreamVolume(AudioManager.STREAM_MUSIC,volume-1, AudioManager.FLAG_PLAY_SOUND);
    }

    @Override
    public void onPadPress(int x, int y) {
        int p = videoView.getCurrentPosition();
        if (x > 180) {
            videoView.seekTo(p + 20000);
        } else {
            videoView.seekTo(p - 20000);
        }
    }

    @Override
    public void onBackPress() {
        finish();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onHomePress() {
        if(videoView.isPlaying()) {
            videoView.pause();
        } else {
            videoView.start();
        }
    }

    @Override
    public void onTrigger() {
        runOnUiThread(()->{
            if (mediaCtrl != null) {
                try {
                    if(mediaCtrl.isShowing()) {
                        mediaCtrl.hide();
                    } else {
                        mediaCtrl.show(3000);
                    }
                } catch (Exception e) {

                }
            }
        });
    }

    @Override
    public void onRelativeListLoad(ArrayList<ContentsItem> items) {
        RelativeVideos = items;
    }
}