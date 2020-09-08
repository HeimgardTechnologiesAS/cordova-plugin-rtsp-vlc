package com.libs.vlcLibWrapper;

public interface VlcListener {
    void onPlayVlc();

    void onPauseVlc();

    void onStopVlc();

    void onVideoEnd();

    void onError();
}
