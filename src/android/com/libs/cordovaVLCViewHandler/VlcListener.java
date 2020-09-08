package com.libs.cordovaVLCViewHandler;

public interface VlcListener {
    void onPlayVlc();

    void onPauseVlc();

    void onStopVlc();

    void onVideoEnd();

    void onError();
}
