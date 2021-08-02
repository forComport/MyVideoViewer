package com.example.myvideoviewer.jav247;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.myvideoviewer.R;

public class ZombiService extends Service {
    public ZombiService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("ZombiService", "onStartCommand");
        if ("STOP".equals(intent.getAction())) {
            stopForeground(true);
            stopSelf();
        } else {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("ONGOING","ONGOING", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "ONGOING")
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_download)) //BitMap 이미지 요구
                    .setContentTitle("다운로드 중입니다.") //타이틀 TEXT
                    .setContentText("잠시만 기다려주세요.") //서브 타이틀 TEXT
                    .setSmallIcon (R.drawable.ic_download) //필수 (안해주면 에러)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT) //중요도 기본
                    .setOngoing(true) // 사용자가 직접 못지우게 계속 실행하기.
                    .setChannelId("ONGOING");
            Notification noti =  builder.build();
//        notificationManager.notify(0, noti);

            startForeground(1, noti);
        }
        return START_NOT_STICKY;
    }
}