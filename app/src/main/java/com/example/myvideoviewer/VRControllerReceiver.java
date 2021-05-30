package com.example.myvideoviewer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;
import android.view.View;

import java.util.Stack;

public class VRControllerReceiver extends BroadcastReceiver {
    final private static String TAG = "VRControllerReceiver";
    final public static String ACTION_ACTIVE =
            "com.example.myservice.VRControllerReceiver.ACTION_ACTIVE";
    final public static String ACTION_INACTIVE =
            "com.example.myservice.VRControllerReceiver.ACTION_INACTIVE";
    final public static String ACTION_DATA_EVENT =
            "com.example.myservice.VRControllerReceiver.ACTION_DATA_EVENT";

    boolean prevTriggerButton = false;
    boolean prevHomeButton = false;
    boolean prevBackButton = false;
    boolean prevTouchPadButton = false;
    boolean prevVolumeUpButton = false;
    boolean prevVolumeDownButton = false;

    private Activity activity;
    private String name;
    private Stack<String> activeActivity = new Stack<>();

    public VRControllerReceiver(Activity activity, String name) {
        this.activity = activity;
        this.name = name;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_DATA_EVENT.equals(action)) {
            if (!activeActivity.empty() && name.equals(activeActivity.peek()))
            processData(intent.getByteArrayExtra("data"));
        } else if (ACTION_ACTIVE.equals(action)) {
            activeActivity.push(intent.getStringExtra("activity"));
        } else if (ACTION_INACTIVE.equals(action)) {
            activeActivity.pop();
        }
    }

    private void processData(byte[] bytes) {
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
            volumeControl(-1);
        }
        prevVolumeDownButton = volumeDownButton;

        if(!prevVolumeUpButton && volumeUpButton) {
            volumeControl(1);
        }
        prevVolumeUpButton = volumeUpButton;

        if(!prevTouchPadButton && touchPadButton && touched) {
            if ("LibraryActivity".equals(name)) {
                LibraryActivity ac = ((LibraryActivity)activity);
                if ( axisX > 100 && axisX < 250 && axisY > 300) {
                    ac.cursor += 1;
                    if (ac.cursor >= ac.mAdapter.getCount()) {
                        ac.cursor = ac.mAdapter.getCount()-1;
                    }
                } else if (axisX > 100 && axisX < 250 && axisY < 50) {
                    ac.cursor -= 1;
                    if (ac.cursor < 0) {
                        ac.cursor = 0;
                    }
                }
                ac.listview.smoothScrollToPosition(ac.cursor);
                ac.mAdapter.notifyDataSetChanged();
            } else if ("VideoActivity".equals(name)) {
                VideoActivity ac = (VideoActivity) activity;
                if (axisX < 100) {
                    ac.prevButton.callOnClick();
                } else if (axisX > 300) {
                    ac.nextButton.callOnClick();
                }
            }

            Log.d(TAG, "Touch pad " + axisX + ", " + axisY);
        }
        prevTouchPadButton = touchPadButton;

        if (!prevBackButton && backButton) {
            if ("VideoActivity".equals(name)) {
                activity.finish();
            }
            Log.d(TAG, "back");
        }
        prevBackButton = backButton;

        if (!prevHomeButton && homeButton) {
            if("VideoActivity".equals(name)) {
                VideoActivity ac = (VideoActivity) activity;
                ac.rotateButton.callOnClick();
            }
            Log.d(TAG, "home");
        }
        prevHomeButton = homeButton;

        if (!prevTriggerButton && triggerButton) {
            if ("LibraryActivity".equals(name)) {
                LibraryActivity ac = ((LibraryActivity)activity);
                LibraryActivity.LibraryItem item = (LibraryActivity.LibraryItem) ac.mAdapter.getItem(ac.cursor);
                Intent intent = new Intent(activity, VideoActivity.class);
                intent.putExtra("title", item.title);
                intent.putExtra("localUri", item.localUri);
                activity.startActivity(intent);
            } else if ("VideoActivity".equals(name)) {
                VideoActivity ac = (VideoActivity) activity;
                if (ac.playerMenu.getVisibility() == View.INVISIBLE) {
                    ac.playerMenu.setVisibility(View.VISIBLE);
                } else {
                    ac.playerMenu.setVisibility(View.INVISIBLE);
                }
            }
            Log.d(TAG, "Trigger");
        }
        prevTriggerButton = triggerButton;
    }

    private void volumeControl(int delta) {
        AudioManager audio = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        int volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        audio.setStreamVolume(AudioManager.STREAM_MUSIC,volume+delta, AudioManager.FLAG_PLAY_SOUND);
    }
}
