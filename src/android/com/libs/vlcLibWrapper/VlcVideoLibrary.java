package com.libs.vlcLibWrapper;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.os.Handler;

import org.videolan.libvlc.interfaces.IVLCVout;
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
            case MediaPlayer.Event.Buffering:
                vlcListener.onBuffering(event.getBuffering());
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
            Media media = new Media(vlcInstance, Uri.parse(endPoint));
            if(endPoint.endsWith(".sdp")) {
                media.addOption(":network-caching=2000");
                media.addOption(":rtsp-caching=2000");
                media.addOption(":clock-jitter=150");
                media.addOption(":clock-synchro=1");
                media.addOption(":no-avcodec-hurry-up");
                media.addOption(":no-skip-frames");
                media.setHWDecoderEnabled(true, false);
            } else {
                media.addOption(":network-caching=3000");
                media.addOption(":clock-jitter=500");
                media.addOption(":clock-synchro=0");
                media.addOption(":http-caching=3000");
                media.addOption(":file-caching=300000");
                media.addOption(":live-caching=300000");
                media.addOption(":sout-mux-caching=300000");
                media.addOption(":gl=any");
                media.addOption(":no-skip-frames");
                media.addOption(":no-drop-late-frames");
                media.addOption(":no-avcodec-hurry-up");
                media.addOption(":prefetch-buffer-size=1048576");
                media.addOption(":prefetch-read-size=1048576");
                media.addOption(":prefetch-seek-threshold=1024");
                media.setHWDecoderEnabled(false, false);
            }
            setMedia(media);
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
        media.addOption(":fullscreen");

        player = new MediaPlayer(vlcInstance);
        player.setMedia(media);
        player.setEventListener(this);

        IVLCVout vlcOut = player.getVLCVout();

        if (surfaceView != null) {
            vlcOut.setVideoView(surfaceView);
            width = surfaceView.getWidth();
            height = surfaceView.getHeight();
        } else if (textureView != null) {
            vlcOut.setVideoView(textureView);
            width = textureView.getWidth();
            height = textureView.getHeight();
        } else if (surfaceTexture != null) {
            vlcOut.setVideoSurface(surfaceTexture);
        } else if (surface != null) {
            vlcOut.setVideoSurface(surface, surfaceHolder);
        } else {
            throw new RuntimeException("You cant set a null render object");
        }

        if (width != 0 && height != 0) vlcOut.setWindowSize(width, height);
        vlcOut.attachViews();
        player.setVideoTrackEnabled(true);
        player.play();
    }


    public void changeVideoResolution(int width, int height) {
        IVLCVout vlcOut = null;
        if (player != null){
            vlcOut = player.getVLCVout();
            if (vlcOut != null)
                vlcOut.setWindowSize(width, height);
        }
 
    }
}
