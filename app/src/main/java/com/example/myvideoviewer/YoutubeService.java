package com.example.myvideoviewer;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Random;

public class YoutubeService extends Service {

    final private static String TAG = "YoutubeServiceTAG";
    final private static String NOTIFICATION_CHANNEL_ID = "YoutubeService";
    private int notificationId = 2;
    private NotificationManager notificationManager;
    private ArrayList<Integer> idList = new ArrayList<>();
    private ServerSocket httpServerSocket;
    private File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "youtubedl-android");

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID, NotificationManager.IMPORTANCE_LOW);
        notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        Notification noti = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("다운로드 서비스")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .build();
        notificationManager.notify(1, noti);
        startForeground(1, noti);
        new HttpServerThread().start();
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(receiver, filter);
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                if (idList.isEmpty()) {
                    stopForeground(true);
                    stopSelf();
                }
            }
        }
    };

    private String getRandomString() {
        StringBuffer temp = new StringBuffer();
        Random rnd = new Random();
        for (int i = 0; i < 20; i++) {
            int rIndex = rnd.nextInt(3);
            switch (rIndex) {
                case 0:
                    // a-z
                    temp.append((char) ((int) (rnd.nextInt(26)) + 97));
                    break;
                case 1:
                    // A-Z
                    temp.append((char) ((int) (rnd.nextInt(26)) + 65));
                    break;
                case 2:
                    // 0-9
                    temp.append((rnd.nextInt(10)));
                    break;
            }
        }
        return temp.toString();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String youtubeLink = intent.getStringExtra("youtubeLink");
        String title = intent.getStringExtra("title");
        try{
            Notification.Builder notification = startNotification(title);
            final int localNotificationId = notificationId;
            notificationId += 1;
            YoutubeDL.getInstance().init(getApplicationContext());
            YoutubeDLRequest request = new YoutubeDLRequest(youtubeLink);
            VideoInfo info = YoutubeDL.getInstance().getInfo(youtubeLink);

            String filename = getRandomString() + "." + info.getExt();
            request.addOption("-o", directory.getAbsolutePath() + "/" + filename);
            idList.add(localNotificationId);
            notificationManager.notify(localNotificationId, notification.build());
            new Thread() {
                @Override
                public void run() {
                    try {
                        YoutubeDL.getInstance().execute(request, (progress, etaInSeconds) -> {
                            notification.setProgress(100, (int) progress, false);
                            notificationManager.notify(localNotificationId, notification.build());
                            if (progress == 100f) {
                                Log.d(TAG, "success download");
                            }
                        });
                        notificationManager.cancel(localNotificationId);
                        idList.remove((Integer) localNotificationId);
                        addFileToDownloadManager(info, filename);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();

        } catch (Exception e) {
            Log.e(TAG, "fail init ", e);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void addFileToDownloadManager(VideoInfo info, String filename) throws Exception {
        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse("http://localhost:8888/"+filename));
        request.setTitle(info.getTitle());
        request.setDescription("다운로드중...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS,
                filename);
        downloadManager.enqueue(request);
    }

    public Notification.Builder startNotification(String subject) {
        return new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("다운로드중...")
                .setContentText(subject)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .setProgress(0, 0, false);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (httpServerSocket != null) {
            try {
                httpServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    private class HttpServerThread extends Thread {
        static final int PORT = 8888;

        @Override
        public void run() {
            Socket socket = null;
            BufferedReader is;
            DataOutputStream os;
            String request;
            try {
                httpServerSocket = new ServerSocket(PORT);
                while (true) {
                    socket = httpServerSocket.accept();
                    is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    request = is.readLine();
                    String path = request.split(" ")[1];
                    os = new DataOutputStream(socket.getOutputStream());
                    File file = new File(directory.getAbsoluteFile()+path);
                    FileInputStream fi = new FileInputStream(file);
                    os.writeBytes("HTTP/1.0 200" +"\r\n");
                    os.writeBytes("Content type: "+ Files.probeContentType(file.toPath()) +"\r\n");
                    os.writeBytes("Content-Length: " + file.length() + "\r\n");
                    os.writeBytes("\r\n");

                    byte[] buf = new byte[16 * 1024];
                    int read = 0;
                    while((read=fi.read(buf)) > 0) {
                        os.write(buf, 0, read);
                    }
                    os.flush();
                    fi.close();
                    os.close();
                    socket.close();
                    file.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
