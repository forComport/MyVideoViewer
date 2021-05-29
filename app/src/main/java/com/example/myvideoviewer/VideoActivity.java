package com.example.myvideoviewer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowMetrics;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;

public class VideoActivity extends AppCompatActivity implements SurfaceHolder.Callback, MediaPlayer.OnVideoSizeChangedListener {

    final static private String TAG = "VideoActivity";

    final static int menuShowDuration = 5000;
    static int playSeek = -1;
    MediaPlayer mediaPlayer;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    ProgressBar progressBar;
    Thread progressThread;
    ImageButton playButton;
    LinearLayout playerMenu;
    VRControllerReceiver receiver;
    ImageButton rotateButton;
    ImageButton prevButton;
    ImageButton nextButton;

    int lastSeek = 0;
    long lastSeekTime = 0;

    Handler hideMenuHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            playerMenu.setVisibility(View.INVISIBLE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        mediaPlayer = new MediaPlayer();
        surfaceView = findViewById(R.id.surface);
        playerMenu = findViewById(R.id.player_menu);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        registerButtonEvent();

        IntentFilter filter = new IntentFilter();
        filter.addAction(VRControllerReceiver.ACTION_DATA_EVENT);
        filter.addAction(VRControllerReceiver.ACTION_ACTIVE);
        filter.addAction(VRControllerReceiver.ACTION_INACTIVE);
        receiver = new VRControllerReceiver(this, "VideoActivity");
        registerReceiver(receiver, filter);
        Intent intent = new Intent(VRControllerReceiver.ACTION_ACTIVE);
        intent.putExtra("activity", "VideoActivity");
        sendBroadcast(intent);
    }

    private void registerButtonEvent() {
        progressBar = findViewById(R.id.progressBar);
        playButton = findViewById(R.id.btn_play);
        progressBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int x = (int) event.getX();
                int w = v.getWidth();
                int seek = (int)((float)x/(float)w * mediaPlayer.getDuration());
                mediaPlayer.seekTo(seek);
                hideMenuHandler.removeCallbacksAndMessages(null);
                hideMenuHandler.sendEmptyMessageDelayed(0, menuShowDuration);
                return false;
            }
        });
        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMenuHandler.removeCallbacksAndMessages(null);
                playerMenu.setVisibility(View.VISIBLE);
                hideMenuHandler.sendEmptyMessageDelayed(0, menuShowDuration);
            }
        });
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    playButton.setImageResource(R.drawable.ic_play);
                } else {
                    mediaPlayer.start();
                    playButton.setImageResource(R.drawable.ic_pause);
                    hideMenuHandler.removeCallbacksAndMessages(null);
                    hideMenuHandler.sendEmptyMessageDelayed(0, menuShowDuration);
                }
            }
        });
        prevButton = findViewById(R.id.btn_prev);
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() - 5000);
                hideMenuHandler.removeCallbacksAndMessages(null);
                hideMenuHandler.sendEmptyMessageDelayed(0, menuShowDuration);
            }
        });
        nextButton = findViewById(R.id.btn_next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lastSeekTime + 3000 < System.currentTimeMillis()) {
                    lastSeek = mediaPlayer.getCurrentPosition();
                }
                lastSeekTime = System.currentTimeMillis();
                lastSeek += 10000;
                mediaPlayer.seekTo(lastSeek);
                hideMenuHandler.removeCallbacksAndMessages(null);
                hideMenuHandler.sendEmptyMessageDelayed(0, menuShowDuration);
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                playButton.setImageResource(R.drawable.ic_play);
            }
        });
        rotateButton = findViewById(R.id.btn_rotate);
        rotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSeek = mediaPlayer.getCurrentPosition();
                progressThread.interrupt();
                if (getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            }
        });
    }

    private void updateProgressBar() {
        final int duration = mediaPlayer.getDuration();
        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                Bundle data = msg.getData();
                int duration = data.getInt("duration");
                int currentPosition = data.getInt("currentPosition");
                progressBar.setMax(duration);
                progressBar.setProgress(currentPosition);
            }
        };

        progressThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                        int currentPosition = mediaPlayer.getCurrentPosition();
                        Message message = Message.obtain();
                        Bundle bundle = new Bundle();
                        bundle.putInt("duration", duration);
                        bundle.putInt("currentPosition", currentPosition);
                        message.setData(bundle);
                        handler.sendMessage(message);
                    } catch (Exception e) {}
                }
            }
        };
        progressThread.start();

    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        try {
            Intent intent = getIntent();
            String filename = intent.getStringExtra("filename");
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename);
            mediaPlayer.setDataSource(file.getPath());
            mediaPlayer.prepare();
            mediaPlayer.setOnVideoSizeChangedListener(this);
            mediaPlayer.setDisplay(surfaceHolder);
            if (playSeek != -1) {
                mediaPlayer.seekTo(playSeek);
            }
            mediaPlayer.start();
            playButton.setImageResource(R.drawable.ic_pause);
            updateProgressBar();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "동영상 로딩에 실패했습니다.", Toast.LENGTH_LONG).show();;
            finish();
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        setFitToFillAspectRatio(mp, width, height);
    }

    private void setFitToFillAspectRatio(MediaPlayer mp, int videoWidth, int videoHeight) {
        if(mp != null) {
            WindowMetrics metrics = getWindowManager().getCurrentWindowMetrics();
            Rect bounds =  metrics.getBounds();
            int screenWidth = bounds.width();
            int screenHeight = bounds.height();
            ViewGroup.LayoutParams videoParams = surfaceView.getLayoutParams();
            if (videoWidth > videoHeight) {
                videoParams.width = screenWidth;
                videoParams.height = screenWidth * videoHeight / videoWidth;
            } else {
                videoParams.width = screenHeight * videoWidth / videoHeight;
                videoParams.height = screenHeight;
            }
            surfaceView.setLayoutParams(videoParams);
        }
    }

    @Override
    protected void onDestroy() {
        Intent intent = new Intent(VRControllerReceiver.ACTION_INACTIVE);
        sendBroadcast(intent);
        mediaPlayer.release();
        mediaPlayer = null;
        if (progressThread != null) {
            progressThread.interrupt();
            progressThread = null;
        }
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}