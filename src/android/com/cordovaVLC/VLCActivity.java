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
import android.util.TypedValue;
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
import android.support.constraint.ConstraintSet;
import android.widget.Chronometer;
import android.transition.TransitionManager;
import android.view.animation.AnticipateInterpolator;
import android.transition.ChangeBounds;

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
    private boolean isLayoutToched = false;
    private boolean isRecording = false;
    private boolean isPTZVisible = false;
    private boolean isRecordingBtnVisible = true;
    private boolean isRecordingStoped = false;

    private String currentLoc = "00:00";
    private String duration = "00:00";
    private RelativeLayout rlUpArrow, rlDownArrow, rlLeftArrow, rlRightArrow, rlLive, rlRecordingTimer,rlRecordingCnt, rlClose, rlCameraView;
    private ImageView upJoy, downJoy, leftJoy, rightJoy, ivClose, joystickLayout, ivRecordingIdle, ivRecordingActive;
    private ConstraintLayout clJoystick, recordSavedLayout, mainLayout;
    private Chronometer cmRecordingTimer;
    private TextView tvLive, tvRecordingSaved;


    public static String UP = "1";
    public static String DOWN = "2";
    public static String LEFT = "3";
    public static String RIGHT = "4";
    public static String NONE = "0";

    public static String PORTRAIT = "portrait";
    public static String LANDSCAPE = "landscape";

    public static final float RATIO  = 16f/9f;

    BroadcastReceiver br = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String method = intent.getStringExtra("method");
                Log.d("usao u redive", method);
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
                        closeLayout();
                    }
                    else if (method.equals("close")) {
                        closeLayout();
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
                 
                    }
                    else if (method.equals(CordovaAPIKeys.WEBVIEW_SHOW_PTZ_BUTTONS)) {
                        boolean value = intent.getBooleanExtra("data",false);
                        isPTZVisible = value;
                        showPTZBtn(value);
                    }
                    else if (method.equals(CordovaAPIKeys.WEBVIEW_SHOW_RECORDING_BUTTON)) {
                        boolean value = intent.getBooleanExtra("data",false);
                        isRecordingBtnVisible = value;
                        showRecordingBtn(value);
                    }
                    else if (method.equals(CordovaAPIKeys.WEBVIEW_UPDATE_REC_STATUS)) {
                        boolean value = intent.getBooleanExtra("data",false);
                        isRecording = value;
                        if (value) {
                            recordingHasStarted(value);
                        } else {
                            recordingIsStopped(value);
                        }
                    }
                    else if (method.equals(CordovaAPIKeys.WEBVIEW_ELEMENTS_VISIBILITY)) {
                        boolean value = intent.getBooleanExtra("data",false);
                        showOrHideElements(value);
                    }
                    else if (method.equals(CordovaAPIKeys.WEBVIEW_SET_TRANSLATIONS)) {
                        try {
                            String value = intent.getStringExtra("data");
                            JSONObject translationJson = new JSONObject(value);
                            setTranslations(translationJson);
                        } catch (JSONException err){
                            Log.d("Error", err.toString());
                        }
                       
                    }
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
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
        
        // play
        _initPlayer();
        joystickLayout.setBackgroundResource(_getResource("ic_joystick_background","drawable"));
    }

        private void _UIListener() {

        mainLayout = findViewById(_getResource("main_layout", "id"));
        mSeekBar = (SeekBar) findViewById(_getResource("videoSeekBar", "id"));

        surfaceView = (SurfaceView) findViewById(_getResource("vlc_surfaceView", "id"));
    
        videoCurrentLoc = (TextView) findViewById(_getResource("videoCurrentLoc", "id"));
        videoDuration = (TextView) findViewById(_getResource("videoDuration", "id"));

        mediaPlayerView = (LinearLayout) findViewById(_getResource("mediaPlayerView", "id"));
        mediaPlayerControls = (LinearLayout) findViewById(_getResource("mediaPlayerControls", "id"));
        mediaPlayerControls.bringToFront();

        rlLive = findViewById(_getResource("rl_live","id"));
        rlRecordingTimer = findViewById(_getResource("rl_recording_timer","id"));
        rlRecordingCnt = findViewById(_getResource("rl_recording_cnt","id"));
        rlClose = findViewById(_getResource("rl_close","id"));
        rlCameraView = findViewById(_getResource("rl_camera_layout","id"));

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
        tvLive = findViewById(_getResource("tv_live","id"));
        tvRecordingSaved = findViewById(_getResource("tv_recording_saved","id"));

        setClickListeners();
        vlcVideoLibrary = new VlcVideoLibrary(this, this, surfaceView);
        changeVideoViewProperties(PORTRAIT, RATIO);
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
        if (vlcVideoLibrary != null && vlcVideoLibrary.isPlaying()) {
            vlcVideoLibrary.stop();
        }
        if(vlcVideoLibrary.getVlcInstance() != null) {
            vlcVideoLibrary.getVlcInstance().release();
        }
        closeLayout();
    }

    @Override
    public void onResume() {
        super.onResume();
        _sendBroadCast("onViewCreated");
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
    }

    @Override
    public void onPauseVlc() {
        _sendBroadCast("onPauseVlc");

        Drawable drawableIcon = getResources().getDrawable(_getResource("ic_play_arrow_white_24dp", "drawable"));
    }

    @Override
    public void onStopVlc() {
        _sendBroadCast("onStopVlc");

        Drawable drawableIcon = getResources().getDrawable(_getResource("ic_play_arrow_white_24dp", "drawable"));
    }

    @Override
    public void onVideoEnd() {
        _sendBroadCast("onVideoEnd");

        Drawable drawableIcon = getResources().getDrawable(_getResource("ic_play_arrow_white_24dp", "drawable"));
    }

    @Override
    public void onError() {
        _sendBroadCast("onError");
        closeLayout();

        Drawable drawableIcon = getResources().getDrawable(_getResource("ic_play_arrow_white_24dp", "drawable"));
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
                        if (vlcVideoLibrary != null && vlcVideoLibrary.isPlaying()) {
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

        mainLayout.setOnClickListener(v -> {
            if(!isRecording) {
                try {
                    isLayoutToched = !isLayoutToched;
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("type", CordovaAPIKeys.PLAYER_SCREEN_TOUCH_EVENT);
                    jsonObject.put("value", isLayoutToched);
                    _sendBroadCast(CordovaAPIKeys.PLAYER_SCREEN_TOUCH_EVENT, jsonObject);
                }catch (JSONException err){
                    Log.d("Error", err.toString());
                }
            }
        });

        ivRecordingIdle.setOnClickListener(v -> {
            activateRecording();
        });

        ivRecordingActive.setOnClickListener(v -> {
            stopRecording();
        });

        rlClose.setOnClickListener(v -> {
            ivClose.setAlpha(0.2f);
            rlClose.postDelayed(() -> {
                ivClose.setAlpha(1f);
                closeLayout();
                 }, 100);
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

    private void setTranslations(JSONObject jsonObject) {
        try {
            tvLive.setText(jsonObject.getString(CordovaAPIKeys.LIVE_TRANSLATION));
            tvRecordingSaved.setText(jsonObject.getString(CordovaAPIKeys.FINISHED_RECORDING_TRANSLATION));
        } catch (JSONException e){
            Log.d("jsonException", e.toString());
        }
        
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
            isRecordingStoped = true;
        }catch (JSONException err){
            Log.d("Error", err.toString());
            ivRecordingActive.setAlpha(1f);
        }

    }

    private void recordingIsStopped(boolean isStarted) {
        setRecordingViewProperties(isStarted);
        cmRecordingTimer.stop();
        if (isRecordingStoped) {
            activateNotification();
        }
       
    }

    private void activateNotification() {
         // activate notification
        recordSavedLayout.setVisibility(View.VISIBLE);
        ivClose.setVisibility(View.INVISIBLE);
        recordSavedLayout.postDelayed(() -> {
            recordSavedLayout.setVisibility(View.INVISIBLE);
            ivClose.setVisibility(View.VISIBLE);
            isRecordingStoped = false;
        }, 3000);
    }

    private void showOrHideElements(boolean isHidden) {
        if(isHidden) {
            rlRecordingCnt.setVisibility(View.INVISIBLE);
            clJoystick.setVisibility(View.INVISIBLE);
        } else {
            showRecordingBtn(isRecordingBtnVisible);
            showPTZBtn(isPTZVisible);
        }
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
        if(activity != null) {
            activity.finish();
        }
    
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
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            createPortraitLayoutProperties();
        }
    }

    public void createLandscapeLayoutProperties() {
        changeVideoViewProperties(LANDSCAPE, RATIO);
        joystickLayout.setBackgroundResource(_getResource("ic_joystick_landscape","drawable"));

        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) clJoystick.getLayoutParams();
        params.horizontalBias = 0.9f;
        params.verticalBias = 0.85f;
        clJoystick.setLayoutParams(params);

        ConstraintLayout.LayoutParams rlLiveParams = (ConstraintLayout.LayoutParams) rlLive.getLayoutParams();
        rlLiveParams.horizontalBias = 0.1f;
        rlLiveParams.verticalBias = 0.05f;
        rlLive.setLayoutParams(rlLiveParams);

        ConstraintLayout.LayoutParams closeParams = (ConstraintLayout.LayoutParams) rlClose.getLayoutParams();
        closeParams.horizontalBias = 0.03f;
        closeParams.verticalBias = 0.035f;
        rlClose.setLayoutParams(closeParams);
        
        // adding new constraints to record button -------------------------------------------------------------------------------
        ConstraintSet recordBtnSet = new ConstraintSet();
        ConstraintLayout recordBtnMainLayout = findViewById(_getResource("main_layout", "id"));
        recordBtnSet.clone(recordBtnMainLayout);
        recordBtnSet.connect(_getResource("rl_recording_cnt","id"), ConstraintSet.TOP, ConstraintSet.PARENT_ID,ConstraintSet.TOP, 0);
        recordBtnSet.connect(_getResource("rl_recording_timer","id"), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0);
        recordBtnSet.connect(_getResource("rl_recording_timer","id"), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);
        recordBtnSet.connect(_getResource("rl_recording_timer","id"), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);
        recordBtnSet.applyTo(recordBtnMainLayout);
    
        ConstraintLayout.LayoutParams recordParams = (ConstraintLayout.LayoutParams) rlRecordingCnt.getLayoutParams();
        recordParams.horizontalBias = 0.9f;
        recordParams.verticalBias = 0.3f;
        rlRecordingCnt.setLayoutParams(recordParams);
        // ----------------------------------------------------------------------------------------------------------------------


        // adding new constraints to recording time image -----------------------------------------------------------------------------
        ConstraintSet recordTimeSet = new ConstraintSet();
        ConstraintLayout recordTimeLayout = findViewById(_getResource("main_layout", "id"));
        recordTimeSet.clone(recordTimeLayout);
        recordTimeSet.connect(_getResource("rl_recording_timer","id"), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0);
        recordTimeSet.connect(_getResource("rl_recording_timer","id"), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0);
        recordTimeSet.connect(_getResource("rl_recording_timer","id"), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);
        recordTimeSet.connect(_getResource("rl_recording_timer","id"), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);
        recordTimeSet.applyTo(recordTimeLayout);

        ConstraintLayout.LayoutParams rlRecordingTimerParams = (ConstraintLayout.LayoutParams) rlRecordingTimer.getLayoutParams();
        rlRecordingTimerParams.horizontalBias = 0.5f;
        rlRecordingTimerParams.verticalBias = 0.02f;
        rlRecordingTimer.setLayoutParams(rlRecordingTimerParams);
        //-----------------------------------------------------------------------------------------------------------------------------------
    }

    public void createPortraitLayoutProperties() {
        changeVideoViewProperties(PORTRAIT, RATIO);
        joystickLayout.setBackgroundResource(_getResource("ic_joystick_background","drawable"));
        
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) clJoystick.getLayoutParams();
        params.horizontalBias = 0.5f;
        params.verticalBias = 0.98f;
        clJoystick.setLayoutParams(params);

        ConstraintLayout.LayoutParams rlLiveParams = (ConstraintLayout.LayoutParams) rlLive.getLayoutParams();
        rlLiveParams.horizontalBias = 0.05f;
        rlLiveParams.verticalBias = 0.1f;
        rlLive.setLayoutParams(rlLiveParams);

        ConstraintLayout.LayoutParams closeParams = (ConstraintLayout.LayoutParams) rlClose.getLayoutParams();
        closeParams.horizontalBias = 0.05f;
        closeParams.verticalBias = 0.05f;
        rlClose.setLayoutParams(closeParams);

        // adding new constraints to record button -----------------------------------------------------------------------------
        ConstraintSet recordBtnSet = new ConstraintSet();
        ConstraintLayout recordBtnMainLayout = findViewById(_getResource("main_layout", "id"));
        recordBtnSet.clone(recordBtnMainLayout);
        recordBtnSet.connect(_getResource("rl_recording_cnt","id"), ConstraintSet.TOP,_getResource("rl_camera_layout","id"), ConstraintSet.BOTTOM, 0);
        recordBtnSet.connect(_getResource("rl_recording_timer","id"), ConstraintSet.BOTTOM,_getResource("cl_joystick","id"), ConstraintSet.TOP, 0);
        recordBtnSet.connect(_getResource("rl_recording_timer","id"), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);
        recordBtnSet.connect(_getResource("rl_recording_timer","id"), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);
        recordBtnSet.applyTo(recordBtnMainLayout);

        ConstraintLayout.LayoutParams recordParams = (ConstraintLayout.LayoutParams) rlRecordingCnt.getLayoutParams();
        recordParams.horizontalBias = 0.5f;
        recordParams.verticalBias = 0.9f;
        rlRecordingCnt.setLayoutParams(recordParams);
        // ----------------------------------------------------------------------------------------------------------------------

        // adding new constraints to recording time image -----------------------------------------------------------------------------
        ConstraintSet recordTimeSet = new ConstraintSet();
        ConstraintLayout recordTimeLayout = findViewById(_getResource("main_layout", "id"));
        recordTimeSet.clone(recordTimeLayout);
        recordTimeSet.connect(_getResource("rl_recording_timer","id"), ConstraintSet.BOTTOM, _getResource("rl_camera_layout","id"), ConstraintSet.TOP, 0);
        recordTimeSet.connect(_getResource("rl_recording_timer","id"), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0);
        recordTimeSet.connect(_getResource("rl_recording_timer","id"), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);
        recordTimeSet.connect(_getResource("rl_recording_timer","id"), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);
        recordTimeSet.applyTo(recordTimeLayout);

        ConstraintLayout.LayoutParams rlRecordingTimerParams = (ConstraintLayout.LayoutParams) rlRecordingTimer.getLayoutParams();
        rlRecordingTimerParams.horizontalBias = 0.5f;
        rlRecordingTimerParams.verticalBias = 0.95f;
        rlRecordingTimer.setLayoutParams(rlRecordingTimerParams);
        
    }

    public void changeVideoViewProperties(String orientation, float ratio) {
        ConstraintLayout.LayoutParams cameraViewParams = (ConstraintLayout.LayoutParams) rlCameraView.getLayoutParams();
        int height = 0;
        int width = 0;

        if(orientation.equals(PORTRAIT)) {
            height = (int) (getDisplayMetrics().widthPixels/ratio);
             width = getDisplayMetrics().widthPixels;
            cameraViewParams.height = height;
            cameraViewParams.width = width;
        } else if(orientation.equals(LANDSCAPE)) {
            height = (int) getDisplayMetrics().heightPixels;
            width = (int) (getDisplayMetrics().heightPixels * ratio);
            cameraViewParams.height = height;
            cameraViewParams.width = width;
        }

        rlCameraView.setLayoutParams(cameraViewParams);

        Log.d("resolutionsCameraHeigth",  String.valueOf(cameraViewParams.height));
        Log.d("resolutionsCameraWidth",  String.valueOf(cameraViewParams.width));

        if (vlcVideoLibrary != null)
            vlcVideoLibrary.changeVideoResolution(width, height);
    }

    public DisplayMetrics getDisplayMetrics() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics;
    }

    public float getAutoRatio() {
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        return ((float)metrics.heightPixels / (float)metrics.widthPixels);
    }
}
