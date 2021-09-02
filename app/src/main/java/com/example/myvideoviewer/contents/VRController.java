package com.example.myvideoviewer.contents;


import android.util.Log;

public class VRController {
    boolean prevTriggerButton = false;
    boolean prevHomeButton = false;
    boolean prevBackButton = false;
    boolean prevTouchPadButton = false;
    boolean prevVolumeUpButton = false;
    boolean prevVolumeDownButton = false;
    interface Listener {
        void onVolumeUp();
        void onVolumeDown();
        void onPadPress(int x, int y);
        void onBackPress();
        void onHomePress();
        void onTrigger();
    }
    private Listener listener;
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
