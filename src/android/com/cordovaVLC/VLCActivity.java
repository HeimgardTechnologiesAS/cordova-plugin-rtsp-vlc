package com.cordovaVLC;

import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.view.MotionEvent;
import android.view.Surface;
import android.widget.RelativeLayout;
import android.support.constraint.ConstraintLayout;
import android.widget.Chronometer;

import com.libs.vlcLibWrapper.VlcListener;
import com.libs.vlcLibWrapper.VlcVideoLibrary;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Author: Archie, Disono (webmonsph@gmail.com)
 * Website: http://www.webmons.com
 * <p>
 * Created at: 1/09/2018
 */

public class VLCActivity extends Activity implements VlcListener, View.OnClickListener {
    private Activity activity;
    public static final String BROADCAST_LISTENER = "com.libVLC.Listener";
    public final String TAG = "VLCActivity";

    SurfaceView surfaceView;
    LinearLayout mediaPlayerView;
    LinearLayout mediaPlayerControls;
    SeekBar mSeekBar;
    int mProgress = 0;
    boolean isSeeking = false;
    private VlcVideoLibrary vlcVideoLibrary;
    private ImageButton bStartStop;
    private Handler handlerSeekBar;
    private Runnable runnableSeekBar;
    private TextView videoCurrentLoc;
    private TextView videoDuration;

    private Handler handlerOverlay;
    private Runnable runnableOverlay;
    private int playingPos;

    private String _url;

    private boolean _autoPlay = false;
    private boolean _hideControls = false;

    private String currentLoc = "00:00";
    private String duration = "00:00";
    private RelativeLayout rlUpArrow, rlDownArrow, rlLeftArrow, rlRightArrow, rlLive, rlRecordingTimer,rlRecordingCnt;
    private ImageView upJoy, downJoy, leftJoy, rightJoy, ivClose, joystickLayout, ivRecordingIdle, ivRecordingActive;
    private ConstraintLayout clJoystick, recordSavedLayout;
    private Chronometer cmRecordingTimer;


    public static String UP = "1";
    public static String DOWN = "2";
    public static String LEFT = "3";
    public static String RIGHT = "4";
    public static String NONE = "0";

    BroadcastReceiver br = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String method = intent.getStringExtra("method");

                if (method != null) {
                    if (method.equals("playNext")) {
                        _url = intent.getStringExtra("url");
                        _autoPlay = intent.getBooleanExtra("autoPlay", false);
                        _hideControls = intent.getBooleanExtra("hideControls", false);

                        _initPlayer();
                    }
                    else if (method.equals("pause")) {
                        if (vlcVideoLibrary.isPlaying()) {
                            vlcVideoLibrary.pause();
                        }
                    }
                    else if (method.equals("resume")) {
                        if (vlcVideoLibrary.isPlaying()) {
                            vlcVideoLibrary.getPlayer().play();
                        }
                    }
                    else if (method.equals("stop")) {
                        if (vlcVideoLibrary.isPlaying()) {
                            vlcVideoLibrary.stop();
                        }
                    }
                    else if (method.equals("close")) {
                        /*
                        if (vlcVideoLibrary.isPlaying()) {
                            vlcVideoLibrary.stop();
                        }
                        if(vlcVideoLibrary.getVlcInstance() != null) {
                            vlcVideoLibrary.getVlcInstance().release();
                        }
                        activity.finish();
                        */
                    }
                    else if (method.equals("getPosition")) {
                        if (vlcVideoLibrary.isPlaying()) {
                            JSONObject obj = new JSONObject();
                            try {
                                obj.put("position", playingPos);
                                obj.put("current_location", currentLoc);
                                obj.put("duration", duration);
                                _sendBroadCast("getPosition", obj);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else if (method.equals("seekPosition")) {
                        Log.d(TAG, "Seek: " + intent.getFloatExtra("position", 0));
                        if (vlcVideoLibrary.isPlaying()) {
                            isSeeking = true;
                            _changePosition(intent.getFloatExtra("position", 0));
                        }
                    }
                    else if (method.equals("webview_show_ptz_buttons")) {
                        boolean value = intent.getBooleanExtra("data",false);
                        showPTZBtn(value);
                    }
                    else if (method.equals("webview_show_recording_button")) {
                        boolean value = intent.getBooleanExtra("data",false);
                        showRecordingBtn(value);
                    }
                    else if (method.equals("webview_update_rec_status")) {
                        boolean value = intent.getBooleanExtra("data",false);
                        if (value) {
                            recordingHasStarted(value);
                        } else {
                            recordingIsStopped(value);
                        }
                    }
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
       

        activity = this;
        ActionBar actionBar = activity.getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        setContentView(_getResource("vlc_player", "layout"));
        _UIListener();
        _broadcastRCV();

        Intent intent = getIntent();
        _url = intent.getStringExtra("url");
        // do not show the controls
        _hideControls = intent.getBooleanExtra("hideControls", false);
        // auto play the video after launching
        _autoPlay = intent.getBooleanExtra("autoPlay", false);

        _handlerSeekBar();
        _handlerMediaControl();

        // play
        _initPlayer();
        joystickLayout.setBackgroundResource(_getResource("ic_joystick_background","drawable"));
    }

        private void _UIListener() {
        mSeekBar = (SeekBar) findViewById(_getResource("videoSeekBar", "id"));

        surfaceView = (SurfaceView) findViewById(_getResource("vlc_surfaceView", "id"));
        bStartStop = (ImageButton) findViewById(_getResource("vlc_start_stop", "id"));

        videoCurrentLoc = (TextView) findViewById(_getResource("videoCurrentLoc", "id"));
        videoDuration = (TextView) findViewById(_getResource("videoDuration", "id"));

        mediaPlayerView = (LinearLayout) findViewById(_getResource("mediaPlayerView", "id"));
        mediaPlayerControls = (LinearLayout) findViewById(_getResource("mediaPlayerControls", "id"));
        mediaPlayerControls.bringToFront();

        rlLive = findViewById(_getResource("rl_live","id"));
        rlRecordingTimer = findViewById(_getResource("rl_recording_timer","id"));
        rlRecordingCnt = findViewById(_getResource("rl_recording_cnt","id"));

        rlUpArrow = findViewById(_getResource("up_arrow_click","id"));
        rlDownArrow = findViewById(_getResource("down_arrow_click","id"));
        rlLeftArrow = findViewById(_getResource("left_arrow_click","id"));
        rlRightArrow = findViewById(_getResource("right_arrow_click","id"));

        ivClose = findViewById(_getResource("iv_close","id"));
        ivRecordingIdle = findViewById(_getResource("iv_recording_idle","id"));
        ivRecordingActive = findViewById(_getResource("iv_record_active","id"));

        upJoy = findViewById(_getResource("iv_up_joy","id"));
        downJoy = findViewById(_getResource("iv_down_joy","id"));
        leftJoy = findViewById(_getResource("iv_left_joy","id"));
        rightJoy = findViewById(_getResource("iv_right_joy","id"));

        clJoystick = findViewById(_getResource("cl_joystick","id"));
        joystickLayout = findViewById(_getResource("iv_joystick_layout","id"));

        cmRecordingTimer = findViewById(_getResource("cm_recording_timer","id"));
        recordSavedLayout = findViewById(_getResource("rl_recording_saved","id"));

        setClickListeners();

        bStartStop.setOnClickListener(this);
        vlcVideoLibrary = new VlcVideoLibrary(this, this, surfaceView);
    }

        /**
     * Resource ID
     *
     * @param name
     * @param type layout, drawable, id
     * @return
     */
    private int _getResource(String name, String type) {
        String package_name = getApplication().getPackageName();
        Resources resources = getApplication().getResources();
        return resources.getIdentifier(name, type, package_name);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (vlcVideoLibrary.isPlaying()) {
            vlcVideoLibrary.stop();
        }
        if(vlcVideoLibrary.getVlcInstance() != null) {
            vlcVideoLibrary.getVlcInstance().release();
        }
        activity.finish();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        activity.unregisterReceiver(br);
        _sendBroadCast("onDestroyVlc");
    }

    @Override
    public void onClick(View v) {
        if (!vlcVideoLibrary.isPlaying()) {
            vlcVideoLibrary.play(_url);
        } else {
            vlcVideoLibrary.pause();
        }
    }

    @Override
    public void onPlayVlc() {
        _sendBroadCast("onPlayVlc");

        Drawable drawableIcon = getResources().getDrawable(_getResource("ic_pause_white_24dp", "drawable"));
        bStartStop.setImageDrawable(drawableIcon);
    }

    @Override
    public void onPauseVlc() {
        _sendBroadCast("onPauseVlc");

        Drawable drawableIcon = getResources().getDrawable(_getResource("ic_play_arrow_white_24dp", "drawable"));
        bStartStop.setImageDrawable(drawableIcon);
    }

    @Override
    public void onStopVlc() {
        _sendBroadCast("onStopVlc");

        Drawable drawableIcon = getResources().getDrawable(_getResource("ic_play_arrow_white_24dp", "drawable"));
        bStartStop.setImageDrawable(drawableIcon);
    }

    @Override
    public void onVideoEnd() {
        _sendBroadCast("onVideoEnd");

        Drawable drawableIcon = getResources().getDrawable(_getResource("ic_play_arrow_white_24dp", "drawable"));
        bStartStop.setImageDrawable(drawableIcon);
    }

    @Override
    public void onError() {
        _sendBroadCast("onError");

        if (vlcVideoLibrary != null) {
            vlcVideoLibrary.stop();
        }

        Drawable drawableIcon = getResources().getDrawable(_getResource("ic_play_arrow_white_24dp", "drawable"));
        bStartStop.setImageDrawable(drawableIcon);
    }

    @Override
    public void onBuffering(float percentage) {
        rlLive.setVisibility(View.INVISIBLE); 
        lockOrientation();
        if(percentage < 80) {
            findViewById(_getResource("loadingPanel", "id")).bringToFront();
            if (findViewById(_getResource("loadingPanel", "id")).getVisibility() != View.VISIBLE) {
                findViewById(_getResource("loadingPanel", "id")).setVisibility(View.VISIBLE);
            }
        } else {
            if (findViewById(_getResource("loadingPanel", "id")).getVisibility() == View.VISIBLE) {
                findViewById(_getResource("loadingPanel", "id")).setVisibility(View.GONE);
            }
        }
         if (percentage == 100) {
            unlockOrientation();
            rlLive.setVisibility(View.VISIBLE); 
        }
    }

    public void lockOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();

        switch(rotation) {
            case Surface.ROTATION_180:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
            case Surface.ROTATION_270:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
            case  Surface.ROTATION_0:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case Surface.ROTATION_90:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
        }
    }

    public void unlockOrientation() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private void _initPlayer() {
        new Timer().schedule(
            new TimerTask() {
                @Override
                public void run() {
                    if (_hideControls) {
                        Thread thread = new Thread() {
                            @Override
                            public void run() {
                                runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            mediaPlayerControls.setVisibility(View.GONE); 
                                        }
                                    }
                                );
                            }
                        };

                        thread.start();
                    }

                    if (_autoPlay && vlcVideoLibrary != null && _url != null) {
                        if (vlcVideoLibrary.isPlaying()) {
                            vlcVideoLibrary.stop();
                        }

                        vlcVideoLibrary.play(_url);
                    }
                }
            },
            300
        );
    }

    private void _broadcastRCV() {
        IntentFilter filter = new IntentFilter(VideoPlayerVLC.BROADCAST_METHODS);
        activity.registerReceiver(br, filter);
    }

    private void setClickListeners() {

        ivRecordingIdle.setOnClickListener(v -> {
            activateRecording();
        });

        ivRecordingActive.setOnClickListener(v -> {
            stopRecording();
        });

        ivClose.setOnClickListener(v -> {
            closeLayout();
        });

        rlUpArrow.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                upJoy.setVisibility(View.INVISIBLE);
                _requestCameraMove(NONE);
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                upJoy.setVisibility(View.VISIBLE);
               _requestCameraMove(UP);
            }
            return true;
        });

        rlDownArrow.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                downJoy.setVisibility(View.INVISIBLE);
                _requestCameraMove(NONE);
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                downJoy.setVisibility(View.VISIBLE);
                _requestCameraMove(DOWN);
            }
            return true;
        });

        rlLeftArrow.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                leftJoy.setVisibility(View.INVISIBLE);
                _requestCameraMove(NONE);
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                leftJoy.setVisibility(View.VISIBLE);
               _requestCameraMove(LEFT);
            }
            return true;
        });

        rlRightArrow.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                rightJoy.setVisibility(View.INVISIBLE);
               _requestCameraMove(NONE);
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                rightJoy.setVisibility(View.VISIBLE);
                _requestCameraMove(RIGHT);
            }
            return true;
        });
    }

    private void activateRecording() {
        //send recording flag to cordova
        try {
            ivRecordingIdle.setAlpha(0.5f);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", CordovaAPIKeys.PLAYER_RECORDING_REQUEST);
            jsonObject.put("value", true);
            _sendBroadCast(CordovaAPIKeys.PLAYER_RECORDING_REQUEST, jsonObject);
        }catch (JSONException err){
            Log.d("Error", err.toString());
            ivRecordingIdle.setAlpha(1f);
        }
    }

    private void recordingHasStarted(boolean isStarted){
        setRecordingViewProperties(isStarted);
        cmRecordingTimer.setBase(SystemClock.elapsedRealtime());
        cmRecordingTimer.start();
    }


    private void stopRecording() {
         //send stop recording flag to cordova
         try {
            ivRecordingActive.setAlpha(0.5f);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", CordovaAPIKeys.PLAYER_RECORDING_REQUEST);
            jsonObject.put("value", false);
            _sendBroadCast(CordovaAPIKeys.PLAYER_RECORDING_REQUEST, jsonObject);
        }catch (JSONException err){
            Log.d("Error", err.toString());
            ivRecordingActive.setAlpha(1f);
        }

    }

    private void recordingIsStopped(boolean isStarted) {
        setRecordingViewProperties(isStarted);
        cmRecordingTimer.stop();
    }

    private void setRecordingViewProperties(boolean isRecordingActivated) {
        if(isRecordingActivated){
            ivRecordingIdle.setVisibility(View.INVISIBLE);
            ivRecordingActive.setAlpha(1f);
            ivRecordingActive.setVisibility(View.VISIBLE);
            rlLive.setVisibility(View.INVISIBLE);
            rlRecordingTimer.setVisibility(View.VISIBLE);
            
        } else {
            ivRecordingActive.setVisibility(View.INVISIBLE);
            ivRecordingIdle.setAlpha(1f);
            ivRecordingIdle.setVisibility(View.VISIBLE);
            rlLive.setVisibility(View.VISIBLE);
            rlRecordingTimer.setVisibility(View.INVISIBLE);

            // activate notification
            // recordSavedLayout.setVisibility(View.VISIBLE);
            // ivClose.setVisibility(View.INVISIBLE);
            // recordSavedLayout.postDelayed(() -> {
            //     recordSavedLayout.setVisibility(View.INVISIBLE);
            //     ivClose.setVisibility(View.VISIBLE);
            // }, 2000);
        }
    }


    private void showRecordingBtn(boolean value) {
        if(value) {
            rlRecordingCnt.setVisibility(View.VISIBLE);
        } else {
            rlRecordingCnt.setVisibility(View.INVISIBLE);
        }   
    }

    private void showPTZBtn(boolean value) {
        if(value) {
            clJoystick.setVisibility(View.VISIBLE);
        } else {
            clJoystick.setVisibility(View.INVISIBLE);
        }   
    }


    private void _requestCameraMove(String value) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", CordovaAPIKeys.PLAYER_CAMERA_MOVE_REQUEST);
            jsonObject.put("value", value);
            _sendBroadCast(CordovaAPIKeys.PLAYER_CAMERA_MOVE_REQUEST, jsonObject);
        }catch (JSONException err){
            Log.d("Error", err.toString());
        }
    }

    private void closeLayout() {
        activity.finish();
    }

    private void _handlerSeekBar() {
        // SEEK BAR
        handlerSeekBar = new Handler();
        runnableSeekBar = new Runnable() {
            @Override
            public void run() {
                try {
                    if (vlcVideoLibrary.getPlayer() != null && vlcVideoLibrary.isPlaying()) {
                        long curTime = vlcVideoLibrary.getPlayer().getTime();
                        long totalTime = (long) (curTime / vlcVideoLibrary.getPlayer().getPosition());
                        int minutes = (int) (curTime / (60 * 1000));
                        int seconds = (int) ((curTime / 1000) % 60);
                        int endMinutes = (int) (totalTime / (60 * 1000));
                        int endSeconds = (int) ((totalTime / 1000) % 60);
                        currentLoc = String.format(Locale.US, "%02d:%02d", minutes, seconds);
                        duration = String.format(Locale.US, "%02d:%02d", endMinutes, endSeconds);

                        videoCurrentLoc.setText(currentLoc);
                        videoDuration.setText(duration);

                        if (!isSeeking) {
                            playingPos = (int) (vlcVideoLibrary.getPlayer().getPosition() * 100);
                            mSeekBar.setProgress(playingPos);
                        }
                    }

                    handlerSeekBar.postDelayed(runnableSeekBar, 1000);
                } catch (Exception ignored) {

                }
            }
        };
        runnableSeekBar.run();
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                mProgress = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                _changePosition((float) mProgress);
            }
        });
    }

    private void _changePosition(float progress) {
        // progress
        if (vlcVideoLibrary.getPlayer() != null && vlcVideoLibrary.getPlayer().getTime() > 0 && progress > 0 && isSeeking) {
            vlcVideoLibrary.getPlayer().pause();
            vlcVideoLibrary.getPlayer().setPosition((progress / 100.0f));

            new Timer().schedule(
                    new TimerTask() {
                        @Override
                        public void run() {
                            vlcVideoLibrary.getPlayer().play();
                        }
                    },
                    600
            );
        }

        isSeeking = false;
    }

    private void _handlerMediaControl() {
        // OVERLAY
        handlerOverlay = new Handler();
        runnableOverlay = new Runnable() {
            @Override
            public void run() {
                mediaPlayerControls.setVisibility(View.GONE);
            }
        };
        final long timeToDisappear = 3000;
        handlerOverlay.postDelayed(runnableOverlay, timeToDisappear);
        mediaPlayerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!_hideControls) {
                    mediaPlayerControls.setVisibility(View.VISIBLE);
                }

                handlerOverlay.removeCallbacks(runnableOverlay);
                handlerOverlay.postDelayed(runnableOverlay, timeToDisappear);
            }
        });
    }

    private void _sendBroadCast(String methodName) {
        Intent intent = new Intent();
        intent.setAction(BROADCAST_LISTENER);
        intent.putExtra("method", methodName);
        activity.sendBroadcast(intent);
    }

    private void _sendBroadCast(String methodName, JSONObject object) {
        Intent intent = new Intent();
        intent.setAction(BROADCAST_LISTENER);
        intent.putExtra("method", methodName);
        intent.putExtra("data", object.toString());
        activity.sendBroadcast(intent);
    }

    private void _sendBroadCast(String methodName, String data) {
        Intent intent = new Intent();
        intent.setAction(BROADCAST_LISTENER);
        intent.putExtra("method", methodName);
        intent.putExtra("data", data);
        activity.sendBroadcast(intent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            createLandscapeLayoutProperties();
            vlcVideoLibrary.changeVideoResolution(getDisplayMetrics().widthPixels,getDisplayMetrics().heightPixels);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            createPortraitLayoutProperties();
            vlcVideoLibrary.changeVideoResolution(getDisplayMetrics().widthPixels,getDisplayMetrics().heightPixels);
        }
    }

    public void createLandscapeLayoutProperties() {
        joystickLayout.setBackgroundResource(_getResource("ic_joystick_landscape","drawable"));

        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) clJoystick.getLayoutParams();
        params.horizontalBias = 0.9f;
        params.verticalBias = 0.85f;
        clJoystick.setLayoutParams(params);

        ConstraintLayout.LayoutParams rlLiveParams = (ConstraintLayout.LayoutParams) rlLive.getLayoutParams();
        rlLiveParams.horizontalBias = 0.1f;
        rlLiveParams.verticalBias = 0.05f;
        rlLive.setLayoutParams(rlLiveParams);

        ConstraintLayout.LayoutParams closeParams = (ConstraintLayout.LayoutParams) ivClose.getLayoutParams();
        closeParams.horizontalBias = 0.02f;
        closeParams.verticalBias = 0.05f;
        ivClose.setLayoutParams(closeParams);
        
        ConstraintLayout.LayoutParams recordParams = (ConstraintLayout.LayoutParams) rlRecordingCnt.getLayoutParams();
        recordParams.horizontalBias = 0.9f;
        recordParams.verticalBias = 0.17f;
        rlRecordingCnt.setLayoutParams(recordParams);

        ConstraintLayout.LayoutParams rlRecordingTimerParams = (ConstraintLayout.LayoutParams) rlRecordingTimer.getLayoutParams();
        rlRecordingTimerParams.horizontalBias = 0.5f;
        rlRecordingTimerParams.verticalBias = 0.05f;
        rlRecordingTimer.setLayoutParams(rlRecordingTimerParams);
    }

    public void createPortraitLayoutProperties() {
        joystickLayout.setBackgroundResource(_getResource("ic_joystick_background","drawable"));
        
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) clJoystick.getLayoutParams();
        params.horizontalBias = 0.5f;
        params.verticalBias = 0.95f;
        clJoystick.setLayoutParams(params);

        ConstraintLayout.LayoutParams rlLiveParams = (ConstraintLayout.LayoutParams) rlLive.getLayoutParams();
        rlLiveParams.horizontalBias = 0.05f;
        rlLiveParams.verticalBias = 0.4f;
        rlLive.setLayoutParams(rlLiveParams);

        ConstraintLayout.LayoutParams closeParams = (ConstraintLayout.LayoutParams) ivClose.getLayoutParams();
        closeParams.horizontalBias = 0.05f;
        closeParams.verticalBias = 0.05f;
        ivClose.setLayoutParams(closeParams);

        ConstraintLayout.LayoutParams recordParams = (ConstraintLayout.LayoutParams) rlRecordingCnt.getLayoutParams();
        recordParams.horizontalBias = 0.5f;
        recordParams.verticalBias = 0.96f;
        rlRecordingCnt.setLayoutParams(recordParams);

        ConstraintLayout.LayoutParams rlRecordingTimerParams = (ConstraintLayout.LayoutParams) rlRecordingTimer.getLayoutParams();
        rlRecordingTimerParams.horizontalBias = 0.5f;
        rlRecordingTimerParams.verticalBias = 0.3f;
        rlRecordingTimer.setLayoutParams(rlRecordingTimerParams);
        
    }

    public DisplayMetrics getDisplayMetrics() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics;
    }
}
