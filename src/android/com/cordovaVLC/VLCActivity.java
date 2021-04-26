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
import android.view.ViewTreeObserver;

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
    private boolean _recordingTransitionActive = false;
    private boolean isPTZVisible = false;
    private boolean isRecordingBtnVisible = true;
    private boolean showRecordingNotification = false;
    private boolean isBuffering = false;
    private boolean isRecordingClicked = false;

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
    private String orientation = PORTRAIT;

    public static final float RATIO  = 16f/9f;
    int joystickSize = 0;
    /**
     * Broadcast receiver that receive messages dfrom VideoPlayerVLC.java
     * This is used when we receive data from cordova
     */
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
                        closeLayout();
                    }
                    else if (method.equals("close")) {
                        
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
                        boolean value = intent.getBooleanExtra("data", false);
                        isPTZVisible = value;
                        showPTZBtn(value);
                    }
                    else if (method.equals(CordovaAPIKeys.WEBVIEW_SHOW_RECORDING_BUTTON)) {
                        boolean value = intent.getBooleanExtra("data", false);
                        isRecordingBtnVisible = value;
                        showRecordingBtn(value);
                    }
                    else if (method.equals(CordovaAPIKeys.WEBVIEW_UPDATE_REC_STATUS)) {
                        boolean value = intent.getBooleanExtra("data", false);
                        isRecording = value;
                        _recordingTransitionActive = false;
                        if (value) {
                            recordingHasStarted(value);
                        } else {
                            recordingIsStopped(value);
                            showRecordingNotification = true;
                        }
                    }
                    else if (method.equals(CordovaAPIKeys.WEBVIEW_ELEMENTS_VISIBILITY)) {
                        boolean value = intent.getBooleanExtra("data", false);
                        if(orientation.equals(LANDSCAPE)) {
                            showOrHideElements(value);
                        }
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
        changeVideoViewProperties(orientation, RATIO);
        createJoystickLayout(orientation);
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
    /*
    onPause is part of android lifecycle that triggers everytime when the activity starts to become
     unactive (if app is in the background, or if the activity get destroyed).
     Because of that this event is used for releasing and stoping vlc. 
    */

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

        Drawable drawableIcon = getResources().getDrawable(_getResource("ic_play_arrow_white_24dp", "drawable"));
    }

    @Override
    public void onBuffering(float percentage) {
        //rlLive.setVisibility(View.INVISIBLE); 
        isBuffering = true;
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
            isBuffering = false;
            //rlLive.setVisibility(View.VISIBLE); 
        }
    }

    /** 
     * Locks orientation based on current orientation.
     * This is needed to lock orientation if the buffering of stream starts
    */
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
        /**
         * click listener on mainLayout that will send touch command from player to cordova if.
         * If recording is on, command will not be send.
         */
        mainLayout.setOnClickListener(v -> {
            if(!isRecording && !isRecordingClicked) {
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
            isRecordingClicked = true;
            if(!isBuffering && !_recordingTransitionActive) {
                activateRecording();
            }
        });

        ivRecordingActive.setOnClickListener(v -> {
            if(!_recordingTransitionActive) {
                stopRecording();
            }
        });

        /**
         * Click listener on close icon. It will preform click animation and finish activity.
         */
        rlClose.setOnClickListener(v -> {
            ivClose.setAlpha(0.2f);
            rlClose.postDelayed(() -> {
                ivClose.setAlpha(1f);
                closeLayout();
                 }, 100);
        });


        recordSavedLayout.setOnClickListener(v -> {
            requestRecordingsPage();
            closeLayout();
        });
        /**
         * ----------------------------------------------------------------------------------------
         * Next 4 touch listneres are used for PTZ camera joystick arrows
         */

        upJoy.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                upJoy.setAlpha(1f);
                _requestCameraMove(NONE);
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                upJoy.setAlpha(0.5f);
               _requestCameraMove(UP);
            }
            return true;
        });

        downJoy.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                downJoy.setAlpha(1f);
                _requestCameraMove(NONE);
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                downJoy.setAlpha(0.5f);
                _requestCameraMove(DOWN);
            }
            return true;
        });

        leftJoy.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                leftJoy.setAlpha(1f);
                _requestCameraMove(NONE);
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                leftJoy.setAlpha(0.5f);
               _requestCameraMove(LEFT);
            }
            return true;
        });

        rightJoy.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                rightJoy.setAlpha(1f);
               _requestCameraMove(NONE);
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                rightJoy.setAlpha(0.5f);
                _requestCameraMove(RIGHT);
            }
            return true;
        });
        //-----------------------------------------------------------------------------------------
    }

    /**
     * sets translation sent from cordova
     * jsonObject -> json that contains all translations
     */ 
    private void setTranslations(JSONObject jsonObject) {
        try {
            tvLive.setText(jsonObject.getString(CordovaAPIKeys.LIVE_TRANSLATION));
            tvRecordingSaved.setText(jsonObject.getString(CordovaAPIKeys.FINISHED_RECORDING_TRANSLATION));
        } catch (JSONException e){
            Log.d("jsonException", e.toString());
        }
    }

    /**
     * sends recording flag to cordova
     */
    private void activateRecording() {
        try {
            _recordingTransitionActive = true;
            ivRecordingIdle.setAlpha(0.5f);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", CordovaAPIKeys.PLAYER_RECORDING_REQUEST);
            jsonObject.put("value", true);
            _sendBroadCast(CordovaAPIKeys.PLAYER_RECORDING_REQUEST, jsonObject);
        } catch (JSONException err){
            Log.d("Error", err.toString());
            ivRecordingIdle.setAlpha(1f);
        }
    }
    
    /**
     * starts recording timer
     */
    private void recordingHasStarted(boolean isStarted){
        setRecordingViewProperties(isStarted);
        cmRecordingTimer.setBase(SystemClock.elapsedRealtime());
        cmRecordingTimer.start();
        showRecordingNotification = true;
    }

    /**
     * send stop recording flag to cordova
     */
    private void stopRecording() {
         try {
             _recordingTransitionActive = true;
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

    /**
     * stops recording timer and shows notification
     */
    private void recordingIsStopped(boolean isStarted) {
        setRecordingViewProperties(isStarted);
        cmRecordingTimer.stop();
        if (showRecordingNotification) {
            activateNotification();
        }
       
    }

    /**
     * shows notification that recording has finished
     */
    private void activateNotification() {
        recordSavedLayout.setVisibility(View.VISIBLE);
        ivClose.setVisibility(View.INVISIBLE);
        recordSavedLayout.postDelayed(() -> {
            recordSavedLayout.setVisibility(View.INVISIBLE);
            ivClose.setVisibility(View.VISIBLE);
        }, 3000);
    }

    /**
     * shows or hide elements from screen if the orientation is in the landscape mode
     * isHidden -> if true, hide elements
     */
    private void showOrHideElements(boolean isHidden) {
        if(isHidden && orientation.equals(LANDSCAPE)) {
            rlRecordingCnt.setVisibility(View.INVISIBLE);
            clJoystick.setVisibility(View.INVISIBLE);
        } else {
            showRecordingBtn(isRecordingBtnVisible);
            showPTZBtn(isPTZVisible);
        }
    }

    /**
     * Method that shows/hide layout images for active or inactive recording based on isRecordingActivated
     */
    private void setRecordingViewProperties(boolean isRecordingActivated) {
        if(isRecordingActivated){
            isRecordingClicked = false;
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

    /**
     * sends value to cordova, which arrow is clicked
     */
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

    /**
     * sends request to cordova to open recordings page
     */
    private void requestRecordingsPage() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", CordovaAPIKeys.PLAYER_REQUEST_RECORDING_PAGE);
            jsonObject.put("value", "");
            _sendBroadCast(CordovaAPIKeys.PLAYER_REQUEST_RECORDING_PAGE, jsonObject);
        }catch (JSONException err){
            Log.d("Error", err.toString());
        }
    }

    /**
     * activity destroy
     */
    private void closeLayout() {
        if(activity != null) {
            activity.finish();
        }
    
    }

    /**
     * all _sendBroadCast methods are responsible for communication with VideoPlayerVLC via broadcast
     */
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

    /**
     * Triggers on every configuration change, in our case orientation change
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            createLandscapeLayoutProperties();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            createPortraitLayoutProperties();
        }
    }

    /**
     * method responsible for creating landscape layout using constraints
     */
    public void createLandscapeLayoutProperties() {
        orientation = LANDSCAPE;
        if(isRecording) {
            showOrHideElements(false);
        } else {
            showOrHideElements(isLayoutToched);
        }
        changeVideoViewProperties(orientation, RATIO);
        createJoystickLayout(orientation);

         // adding new constraints for joystick -------------------------------------------------------------------------------
        ConstraintSet joystickSet = new ConstraintSet();
        ConstraintLayout joystickMainLayout = findViewById(_getResource("main_layout", "id"));
        joystickSet.clone(joystickMainLayout);
        joystickSet.connect(_getResource("cl_joystick","id"), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0);
        joystickSet.connect(_getResource("cl_joystick","id"), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0);
        joystickSet.connect(_getResource("cl_joystick","id"), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);
        joystickSet.connect(_getResource("cl_joystick","id"), ConstraintSet.END, _getResource("rl_camera_layout","id"), ConstraintSet.END, 0);
        joystickSet.applyTo(joystickMainLayout);

        ConstraintLayout.LayoutParams joystickParams = (ConstraintLayout.LayoutParams) clJoystick.getLayoutParams();
        joystickParams.horizontalBias = 0.98f;
        joystickParams.verticalBias = 0.85f;
        clJoystick.setLayoutParams(joystickParams);


        // adding new constraints for live img -------------------------------------------------------------------------------   
        ConstraintLayout.LayoutParams rlLiveParams = (ConstraintLayout.LayoutParams) rlLive.getLayoutParams();
        rlLiveParams.horizontalBias = 0.18f;
        rlLiveParams.verticalBias = 0.05f;
        rlLive.setLayoutParams(rlLiveParams);

        // adding new constraints to notification -------------------------------------------------------------------------------
        ConstraintSet notificationSet = new ConstraintSet();
        ConstraintLayout notificationLayout = findViewById(_getResource("main_layout", "id"));
        notificationSet.clone(notificationLayout);
        notificationSet.connect(_getResource("rl_recording_saved","id"), ConstraintSet.TOP, _getResource("rl_camera_layout","id"),ConstraintSet.TOP, 0);
        notificationSet.connect(_getResource("rl_recording_saved","id"), ConstraintSet.BOTTOM, _getResource("rl_camera_layout","id"), ConstraintSet.BOTTOM, 0);
        notificationSet.connect(_getResource("rl_recording_saved","id"), ConstraintSet.START, _getResource("rl_camera_layout","id"), ConstraintSet.START, (int) (getDisplayMetrics().widthPixels*0.1));
        notificationSet.connect(_getResource("rl_recording_saved","id"), ConstraintSet.END, _getResource("rl_camera_layout","id"), ConstraintSet.END, (int) (getDisplayMetrics().widthPixels*0.1));
        notificationSet.applyTo(notificationLayout);

        ConstraintLayout.LayoutParams recordSavedParams = (ConstraintLayout.LayoutParams) recordSavedLayout.getLayoutParams();
        recordSavedParams.verticalBias = 0.025f;
        recordSavedLayout.setLayoutParams(recordSavedParams);

        // adding new constraints to close button -------------------------------------------------------------------------------
        ConstraintSet closeBtnSet = new ConstraintSet();
        ConstraintLayout closeBtnMainLayout = findViewById(_getResource("main_layout", "id"));
        closeBtnSet.clone(closeBtnMainLayout);
        closeBtnSet.connect(_getResource("rl_close","id"), ConstraintSet.TOP, _getResource("rl_camera_layout","id"),ConstraintSet.TOP, 0);
        closeBtnSet.connect(_getResource("rl_close","id"), ConstraintSet.BOTTOM, _getResource("rl_camera_layout","id"), ConstraintSet.BOTTOM, 0);
        closeBtnSet.connect(_getResource("rl_close","id"), ConstraintSet.START, _getResource("rl_camera_layout","id"), ConstraintSet.START, 0);
        closeBtnSet.connect(_getResource("rl_close","id"), ConstraintSet.END, _getResource("rl_camera_layout","id"), ConstraintSet.END, 0);
        closeBtnSet.applyTo(closeBtnMainLayout);

        ConstraintLayout.LayoutParams closeParams = (ConstraintLayout.LayoutParams) rlClose.getLayoutParams();
        closeParams.horizontalBias = 0.02f;
        closeParams.verticalBias = 0.035f;
        rlClose.setLayoutParams(closeParams);
        //-----------------------------------------------------------------------------------------------------------------------------
        
        // adding new constraints to record button -------------------------------------------------------------------------------
        ConstraintSet recordBtnSet = new ConstraintSet();
        ConstraintLayout recordBtnMainLayout = findViewById(_getResource("main_layout", "id"));
        recordBtnSet.clone(recordBtnMainLayout);
        recordBtnSet.connect(_getResource("rl_recording_cnt","id"), ConstraintSet.TOP, ConstraintSet.PARENT_ID,ConstraintSet.TOP, 0);
        recordBtnSet.connect(_getResource("rl_recording_cnt","id"), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0);
        recordBtnSet.connect(_getResource("rl_recording_cnt","id"), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);
        recordBtnSet.connect(_getResource("rl_recording_cnt","id"), ConstraintSet.END, _getResource("rl_camera_layout","id"), ConstraintSet.END, 0);
        recordBtnSet.applyTo(recordBtnMainLayout);
    
        ConstraintLayout.LayoutParams recordParams = (ConstraintLayout.LayoutParams) rlRecordingCnt.getLayoutParams();
        recordParams.horizontalBias = 0.98f;
        recordParams.verticalBias = 0.15f;
    
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

    /**
     * method responsible for creating portrait layout using constraints
     */
    public void createPortraitLayoutProperties() {
        orientation = PORTRAIT;
        //reseting hiden elements from landscape mode because in portrait mode all elements must be visible----------------
        showOrHideElements(false);
        //--------------------------------------------------------------------------------------------------------------------
        changeVideoViewProperties(orientation, RATIO);
        createJoystickLayout(orientation);
       
        // adding new constraints for joystick -------------------------------------------------------------------------------
        ConstraintSet joystickSet = new ConstraintSet();
        ConstraintLayout joystickMainLayout = findViewById(_getResource("main_layout", "id"));
        joystickSet.clone(joystickMainLayout);
        joystickSet.connect(_getResource("cl_joystick","id"), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0);
        joystickSet.connect(_getResource("cl_joystick","id"), ConstraintSet.TOP, _getResource("rl_camera_layout","id"), ConstraintSet.BOTTOM, 0);
        joystickSet.connect(_getResource("cl_joystick","id"), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);
        joystickSet.connect(_getResource("cl_joystick","id"), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);
        joystickSet.applyTo(joystickMainLayout);

        
        ConstraintLayout.LayoutParams joystickParams = (ConstraintLayout.LayoutParams) clJoystick.getLayoutParams();
        joystickParams.horizontalBias = 0.5f;
        joystickParams.verticalBias = 0.9f;
        clJoystick.setLayoutParams(joystickParams);


        // adding new constraints for live img -------------------------------------------------------------------------------   
        ConstraintLayout.LayoutParams rlLiveParams = (ConstraintLayout.LayoutParams) rlLive.getLayoutParams();
        rlLiveParams.horizontalBias = 0.05f;
        rlLiveParams.verticalBias = 0.1f;
        rlLive.setLayoutParams(rlLiveParams);


        // adding new constraints to notification -------------------------------------------------------------------------------
        ConstraintSet notificationSet = new ConstraintSet();
        ConstraintLayout notificationLayout = findViewById(_getResource("main_layout", "id"));
        notificationSet.clone(notificationLayout);
        notificationSet.connect(_getResource("rl_recording_saved","id"), ConstraintSet.TOP, ConstraintSet.PARENT_ID,ConstraintSet.TOP, 0);
        notificationSet.connect(_getResource("rl_recording_saved","id"), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0);
        notificationSet.connect(_getResource("rl_recording_saved","id"), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, getPixelsFromDP(15));
        notificationSet.connect(_getResource("rl_recording_saved","id"), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, getPixelsFromDP(15));
        notificationSet.applyTo(notificationLayout);

        ConstraintLayout.LayoutParams recordSavedParams = (ConstraintLayout.LayoutParams) recordSavedLayout.getLayoutParams();
        recordSavedParams.verticalBias = 0.025f;
        recordSavedLayout.setLayoutParams(recordSavedParams);

        // adding new constraints to close button -------------------------------------------------------------------------------
        ConstraintSet closeBtnSet = new ConstraintSet();
        ConstraintLayout closeBtnMainLayout = findViewById(_getResource("main_layout", "id"));
        closeBtnSet.clone(closeBtnMainLayout);
        closeBtnSet.connect(_getResource("rl_close","id"), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0);
        closeBtnSet.connect(_getResource("rl_close","id"), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0);
        closeBtnSet.connect(_getResource("rl_close","id"), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);
        closeBtnSet.connect(_getResource("rl_close","id"), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);
        closeBtnSet.applyTo(closeBtnMainLayout);

        ConstraintLayout.LayoutParams closeParams = (ConstraintLayout.LayoutParams) rlClose.getLayoutParams();
        closeParams.horizontalBias = 0.05f;
        closeParams.verticalBias = 0.05f;
        rlClose.setLayoutParams(closeParams);
        //------------------------------------------------------------------------------------------------------------------------

        // adding new constraints to record button -----------------------------------------------------------------------------
        ConstraintSet recordBtnSet = new ConstraintSet();
        ConstraintLayout recordBtnMainLayout = findViewById(_getResource("main_layout", "id"));
        recordBtnSet.clone(recordBtnMainLayout);
        recordBtnSet.connect(_getResource("rl_recording_cnt","id"), ConstraintSet.TOP,_getResource("rl_camera_layout","id"), ConstraintSet.BOTTOM, 0);
        recordBtnSet.connect(_getResource("rl_recording_cnt","id"), ConstraintSet.BOTTOM,_getResource("cl_joystick","id"), ConstraintSet.TOP, 0);
        recordBtnSet.connect(_getResource("rl_recording_cnt","id"), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);
        recordBtnSet.connect(_getResource("rl_recording_cnt","id"), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);
        recordBtnSet.applyTo(recordBtnMainLayout);

        ConstraintLayout.LayoutParams recordParams = (ConstraintLayout.LayoutParams) rlRecordingCnt.getLayoutParams();
        recordParams.horizontalBias = 0.5f;
        recordParams.verticalBias = 0.5f;
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
        //------------------------------------------------------------------------------------------------------------------------------------------
        
    }

    /**
     * joystick has 2 types of background color, white and black with alpha
     * one, black is for landscape mode, and the other, white is for portrait mode
     */
    public void createJoystickLayout(String orientation) {
        if(orientation.equals(PORTRAIT)) {
            leftJoy.setImageResource(_getResource("ic_joy_left","drawable"));
            leftJoy.setAdjustViewBounds(true);
            rightJoy.setImageResource(_getResource("ic_joy_right","drawable"));
            rightJoy.setAdjustViewBounds(true);
            upJoy.setImageResource(_getResource("ic_joy_up","drawable"));
            rightJoy.setAdjustViewBounds(true);
            downJoy.setImageResource(_getResource("ic_joy_down","drawable"));
            downJoy.setAdjustViewBounds(true);
        } else if (orientation.equals(LANDSCAPE)) {
            leftJoy.setImageResource(_getResource("ic_joy_left_landscape","drawable"));
            leftJoy.setAdjustViewBounds(true);
            rightJoy.setImageResource(_getResource("ic_joy_right_landscape","drawable"));
            rightJoy.setAdjustViewBounds(true);
            upJoy.setImageResource(_getResource("ic_joy_up_landscape","drawable"));
            rightJoy.setAdjustViewBounds(true);
            downJoy.setImageResource(_getResource("ic_joy_down_landscape","drawable"));
            downJoy.setAdjustViewBounds(true);
        } 
        
    }

    /**
     * change joystick size based on device resolution, if the display is smaller then 5 inches, the joystick will be smaller
     * getViewTreeObserver is triggered when the view is created so that we can know exact dimensions
     */
    public void changeJoystickSize() {
            if(getScreenInchSize() <= 5.5) {
                rlRecordingCnt.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        ConstraintLayout.LayoutParams rlRecordingParams = (ConstraintLayout.LayoutParams) rlRecordingCnt.getLayoutParams();
                        rlRecordingCnt.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        if (joystickSize == 0) {
                            rlRecordingParams.height = (int) (rlRecordingCnt.getHeight() * 0.8);
                            rlRecordingParams.width = (int) (rlRecordingCnt.getHeight() * 0.8);
                            rlRecordingCnt.setLayoutParams(rlRecordingParams);
                        }
                    }
                });
    
                clJoystick.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        ConstraintLayout.LayoutParams clJoystickParams = (ConstraintLayout.LayoutParams) clJoystick.getLayoutParams();
                        clJoystick.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        if (joystickSize == 0){
                            joystickSize = (int) (clJoystick.getHeight() * 0.8);
                            clJoystickParams.height = joystickSize;
                            clJoystickParams.width = joystickSize;
                            clJoystick.setLayoutParams(clJoystickParams);
                            setJoystickArrowsSizes(joystickSize);
                        }
                    }
                });
            } else {
                rlRecordingCnt.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        ConstraintLayout.LayoutParams rlRecordingParams = (ConstraintLayout.LayoutParams) rlRecordingCnt.getLayoutParams();
                        rlRecordingCnt.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        if (joystickSize == 0) {
                            rlRecordingParams.height = (int) (rlRecordingCnt.getHeight());
                            rlRecordingParams.width = (int) (rlRecordingCnt.getHeight());
                            rlRecordingCnt.setLayoutParams(rlRecordingParams);
                        }
                    }
                });
    
                clJoystick.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        ConstraintLayout.LayoutParams clJoystickParams = (ConstraintLayout.LayoutParams) clJoystick.getLayoutParams();
                        clJoystick.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        if (joystickSize == 0) {
                            joystickSize = (int) (clJoystick.getHeight());
                            clJoystickParams.height = joystickSize;
                            clJoystickParams.width = joystickSize;
                            clJoystick.setLayoutParams(clJoystickParams);
                        }
                    }
                });
            } 
    }

    public void setJoystickArrowsSizes(int size) {
        ConstraintLayout.LayoutParams upJoystickParams = (ConstraintLayout.LayoutParams) upJoy.getLayoutParams();
        ConstraintLayout.LayoutParams leftJoystickParams = (ConstraintLayout.LayoutParams) leftJoy.getLayoutParams();
        ConstraintLayout.LayoutParams rightJoystickParams = (ConstraintLayout.LayoutParams) rightJoy.getLayoutParams();
        ConstraintLayout.LayoutParams downJoystickParams = (ConstraintLayout.LayoutParams) downJoy.getLayoutParams();

        upJoystickParams.width = (int) (size * 0.7);
        upJoy.setLayoutParams(upJoystickParams);

        leftJoystickParams.height = (int) (size * 0.7);
        leftJoy.setLayoutParams(leftJoystickParams);

        rightJoystickParams.height = (int) (size * 0.7);
        rightJoy.setLayoutParams(rightJoystickParams);

        downJoystickParams.width = (int) (size * 0.7);
        downJoy.setLayoutParams(downJoystickParams);
    }

    /**
     * tranforms dp to pixels
     * dp -> density-independent pixels
     */
    public int getPixelsFromDP(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, activity.getResources().getDisplayMetrics());
    }

    public double getScreenInchSize() {        
        double x = Math.pow(getDisplayMetrics().widthPixels / getDisplayMetrics().xdpi, 2);
        double y = Math.pow(getDisplayMetrics().heightPixels / getDisplayMetrics().ydpi, 2);
        double screenInches = Math.sqrt(x + y);
        Log.d("debug", "Screen inches : " + screenInches);
        return screenInches;
    }

    /**
     * Calculate layout width and height based on ratio and change joystick size if the display is smaller the 5 inch
     * orientation -> screen oritentation 
     * ratio -> ratio of the camera stream video
     */
    public void changeVideoViewProperties(String orientation, float ratio) {
        ConstraintLayout.LayoutParams cameraViewParams = (ConstraintLayout.LayoutParams) rlCameraView.getLayoutParams();
        int height = 0;
        int width = 0;
        if(orientation.equals(PORTRAIT)) {
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            height = (int) (getDisplayMetrics().widthPixels/ratio);
             width = getDisplayMetrics().widthPixels;
            cameraViewParams.height = height;
            cameraViewParams.width = width;
           
        
        } else if(orientation.equals(LANDSCAPE)) {
            activity.getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
            | View.SYSTEM_UI_FLAG_IMMERSIVE);
            height = (int) getDisplayMetrics().heightPixels;
            width = (int) (getDisplayMetrics().heightPixels * ratio);
            cameraViewParams.height = height;
            cameraViewParams.width = width;
        }

        rlCameraView.setLayoutParams(cameraViewParams);

        Log.d("resolutionsCameraHeight",  String.valueOf(cameraViewParams.height));
        Log.d("resolutionsCameraWidth",  String.valueOf(cameraViewParams.width));

        if (vlcVideoLibrary != null)
            vlcVideoLibrary.changeVideoResolution(width, height);

        changeJoystickSize();
    }

    /**
     * gets metrics of the devices screen (width and height)
     */
    public DisplayMetrics getDisplayMetrics() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics;
    }

    /**
     * calculate ratio of the device screen
     */
    public float getAutoRatio() {
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        return ((float)metrics.heightPixels / (float)metrics.widthPixels);
    }
}
