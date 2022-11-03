package com.example.myvideoviewer.contents;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.example.myvideoviewer.R;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DetailActivity extends AppCompatActivity implements ContentsLoader.DetailListener, VRController.Listener {
    private static final String TAG = "DetailActivityTAG";

    private PlayerView videoView;
//    private MediaController mediaCtrl;
    private VRController vrCtrl;
    private ArrayList<ContentsItem> RelativeVideos = new ArrayList<>();
    private String mUrl;
    private ContentsItem mItem;
    private MediaPlayer mMediaPlayer;
    private SimpleExoPlayer player;

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
//        mediaCtrl = new MediaController(this);
//        videoView.setMediaController(mediaCtrl);
//        videoView.setOnPreparedListener((v)->{
//            int width = videoView.getMeasuredWidth();
//            int height = videoView.getMeasuredHeight();
//            if (height > width) {
//                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//            }
//            videoView.start();
//            findViewById(R.id.progressBar).setVisibility(View.GONE);
//            mMediaPlayer = v;
//        });

        Intent intent = getIntent();
        String provider = intent.getStringExtra("provider");
        ContentsItem item;
        ContentsLoader loader;
        if (provider == null) {
        } else {
            item = (ContentsItem) intent.getParcelableExtra("item");
            loader = ContentsLoader.Provider.get(provider).setContext(this);
            mItem = item;
            loader.setOnDetailListener(this);
            loader.loadDetail(item);
        }

        vrCtrl = new VRController(this);
        vrCtrl.setListener(this);

        registerButton();
    }

    Map<String, String> headers;

    private void initializePlayer(String url, Map<String, String> headers) {
        if (player == null) {
            player = new SimpleExoPlayer.Builder(this).build();
            videoView.setPlayer(player);
        }
//        String sample = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
        this.headers = headers;
        MediaSource mediaSource = buildMediaSource(Uri.parse(url), headers);
        player.setMediaSource(mediaSource);
        player.prepare();
        player.setPlayWhenReady(true);
        findViewById(R.id.progressBar).setVisibility(View.GONE);
    }

//    static class MyHttpDataSource extends DefaultHttpDataSource {
//        @Override
//        public long open(DataSpec dataSpec) throws HttpDataSourceException {
////            setRequestProperty("referer", "https://missav.com/ko/dse-1305");
////            setRequestProperty("origin", "https://missav.com");
//            Log.d(TAG, "set header");
//            return super.open(dataSpec);
//        }
//    }

    private MediaSource buildMediaSource(Uri uri, Map<String, String> headers) {

        String userAgent = Util.getUserAgent(this, "blackJin");

        if (uri.getLastPathSegment().contains("mp3") || uri.getLastPathSegment().contains("mp4")) {

            return new ProgressiveMediaSource.Factory(new DefaultHttpDataSource.Factory())
                    .createMediaSource(uri);

        } else if (uri.getLastPathSegment().contains("m3u8")) {
            //com.google.android.exoplayer:exoplayer-hls 확장 라이브러리를 빌드 해야 합니다.
            return new HlsMediaSource.Factory(new DefaultHttpDataSource.Factory().setDefaultRequestProperties(headers))
                    .createMediaSource(MediaItem.fromUri(uri));

        } else {

            return new ProgressiveMediaSource.Factory(new DefaultDataSourceFactory(this, userAgent))
                    .createMediaSource(uri);
        }

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

        findViewById(R.id.btn_download).setOnClickListener((v)->{
            if (mUrl != null) {
                DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (mUrl.startsWith("file://")) {
                    downloadManager.remove(mItem.id);
                    Toast.makeText(getApplicationContext(), "삭제완료", Toast.LENGTH_LONG).show();
                } else {
                    Uri uri = Uri.parse(mUrl);
                    DownloadManager.Request request = new DownloadManager.Request(uri);
                    request.setTitle(mItem.title);
                    request.setDescription("다운로드중...");
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
                    request.setDestinationInExternalFilesDir(DetailActivity.this, Environment.DIRECTORY_DOWNLOADS,
                            uri.getLastPathSegment());
                    downloadManager.enqueue(request);
                    Toast.makeText(getApplicationContext(), "다운로드 시작", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(player != null) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        if(player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }

    @Override
    public void onVideoLoad(String url, Map<String, String> headers) {
        Log.d(TAG, url);
        mUrl = url;
//        videoView.setVideoURI(Uri.parse(url), headers);
//        videoView.requestFocus();
        initializePlayer(url, headers);
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
        runOnUiThread(()->{
            long p = player.getCurrentPosition();
            if (x > 180) {
                player.seekTo(p + 3000);
//            player.play();
            } else {
                player.seekTo(p - 3000);
//            player.play();
            }
        });
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
        runOnUiThread(()->{
            if (player != null) {
                if(player.isPlaying()) {
                    player.pause();
                } else {
                    player.play();
                }
            }
        });
    }

    @Override
    public void onTrigger() {
        runOnUiThread(()->{
            if (videoView.isControllerVisible()) {
                videoView.hideController();
            } else {
                videoView.showController();
            }
        });
    }

    @Override
    public void onRelativeListLoad(ArrayList<ContentsItem> items) {
        RelativeVideos = items;
    }
}