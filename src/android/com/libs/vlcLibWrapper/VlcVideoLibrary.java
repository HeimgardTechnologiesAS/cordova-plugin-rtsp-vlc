package com.libs.vlcLibWrapper;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Handler;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;


public class VlcVideoLibrary implements MediaPlayer.EventListener {
    private int width = 0, height = 0;

    private SurfaceView surfaceView;
    private TextureView textureView;
    private SurfaceTexture surfaceTexture;
    private Surface surface;
    private SurfaceHolder surfaceHolder;
    private LibVLC vlcInstance;
    private MediaPlayer player;
    private VlcListener vlcListener;
    private int streamHeight = 0, streamWidth = 0;

    public VlcVideoLibrary(Context context, VlcListener vlcListener, SurfaceView surfaceView) {
        this.vlcListener = vlcListener;
        this.surfaceView = surfaceView;
        vlcInstance = new LibVLC(context, new VlcOptions().getDefaultOptions());
    }

    public VlcVideoLibrary(Context context, VlcListener vlcListener, TextureView textureView) {
        this.vlcListener = vlcListener;
        this.textureView = textureView;
        vlcInstance = new LibVLC(context, new VlcOptions().getDefaultOptions());
    }

    public VlcVideoLibrary(Context context, VlcListener vlcListener, SurfaceTexture surfaceTexture) {
        this.vlcListener = vlcListener;
        this.surfaceTexture = surfaceTexture;
        vlcInstance = new LibVLC(context, new VlcOptions().getDefaultOptions());
    }

    public VlcVideoLibrary(Context context, VlcListener vlcListener, Surface surface) {
        this.vlcListener = vlcListener;
        this.surface = surface;
        surfaceHolder = null;
        vlcInstance = new LibVLC(context, new VlcOptions().getDefaultOptions());
    }

    public VlcVideoLibrary(Context context, VlcListener vlcListener, Surface surface,
                           SurfaceHolder surfaceHolder) {
        this.vlcListener = vlcListener;
        this.surface = surface;
        this.surfaceHolder = surfaceHolder;
        vlcInstance = new LibVLC(context, new VlcOptions().getDefaultOptions());
    }

    public VlcVideoLibrary(Context context, VlcListener vlcListener, Surface surface, int width,
                           int height) {
        this.vlcListener = vlcListener;
        this.surface = surface;
        this.width = width;
        this.height = height;
        surfaceHolder = null;
        vlcInstance = new LibVLC(context, new VlcOptions().getDefaultOptions());
    }

    public VlcVideoLibrary(Context context, VlcListener vlcListener, Surface surface,
                           SurfaceHolder surfaceHolder, int width, int height) {
        this.vlcListener = vlcListener;
        this.surface = surface;
        this.surfaceHolder = surfaceHolder;
        this.width = width;
        this.height = height;
        vlcInstance = new LibVLC(context, new VlcOptions().getDefaultOptions());
    }

    @Override
    public void onEvent(MediaPlayer.Event event) {
        switch (event.type) {
            case MediaPlayer.Event.Playing:
                vlcListener.onPlayVlc();
                break;
            case MediaPlayer.Event.Paused:
                vlcListener.onPauseVlc();
                break;
            case MediaPlayer.Event.Stopped:
                vlcListener.onStopVlc();
                break;
            case MediaPlayer.Event.EndReached:
                player.stop();
                vlcListener.onVideoEnd();
                break;
            case MediaPlayer.Event.EncounteredError:
                vlcListener.onError();
                break;
            default:
                break;
        }
    }

    public MediaPlayer getPlayer() {
        return player;
    }

    public LibVLC getVlcInstance() {
        return vlcInstance;
    }

    public boolean isPlaying() {
        return vlcInstance != null && player != null && player.isPlaying();
    }

    public void play(String endPoint) {
        if (player == null || player.isReleased()) {
            setMedia(new Media(vlcInstance, Uri.parse(endPoint)));
        } else if (!player.isPlaying()) {
            player.play();
        }
    }

    public void stop() {
        if (player != null && player.isPlaying()) {
            player.stop();
            player.release();
        }
    }

    public void pause() {
        if (player != null && player.isPlaying()) {
            player.pause();
        }
    }

    private void setMedia(Media media) {
        media.addOption(":network-caching=" + Constants.BUFFER);
        media.addOption(":file-caching=" + Constants.BUFFER);
        media.addOption(":fullscreen");
        media.setHWDecoderEnabled(true, false);

        player = new MediaPlayer(vlcInstance);
        player.setMedia(media);
        player.setEventListener(this);

        detachVLCView();
        if (streamWidth > 0 && streamHeight > 0){
            setVLCViewSize(streamWidth,streamHeight);
            player.setVideoTrackEnabled(true);
        }
    }

    public void setVLCViewSize(int width, int height) {

        IVLCVout vlcOut = player.getVLCVout();
        if (surfaceView != null) {
            vlcOut.setVideoView(surfaceView);
        } else if (textureView != null) {
            vlcOut.setVideoView(textureView);
        } else if (surfaceTexture != null) {
            vlcOut.setVideoSurface(surfaceTexture);
        } else if (surface != null) {
            vlcOut.setVideoSurface(surface, surfaceHolder);
        } else {
            throw new RuntimeException("You cant set a null render object");
        }
        if (width != 0 && height != 0) vlcOut.setWindowSize(width, height);
        player.pause();
        vlcOut.attachViews();

        new Handler().postDelayed(() -> {
                player.play();
                surfaceView.setVisibility(View.VISIBLE);
        }, 100);

    }
    public void detachVLCView() {
        IVLCVout vlcOut = player.getVLCVout();
        if(vlcOut != null){
            vlcOut.detachViews();
        }

    }
    public void setStreamDimensions(int width,int height) {
        streamWidth = width;
        streamHeight = height;
    }

    
}
