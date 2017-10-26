package com.otvcloud.tachographdemo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;

import com.baidu.speech.asr.SpeechConstant;
import com.otvcloud.tachographdemo.baidu.control.MyRecognizer;
import com.otvcloud.tachographdemo.baidu.control.MyWakeup;
import com.otvcloud.tachographdemo.baidu.recognization.IStatus;
import com.otvcloud.tachographdemo.baidu.recognization.MessageStatusRecogListener;
import com.otvcloud.tachographdemo.baidu.recognization.PidBuilder;
import com.otvcloud.tachographdemo.baidu.recognization.StatusRecogListener;
import com.otvcloud.tachographdemo.baidu.wakeup.IWakeupListener;
import com.otvcloud.tachographdemo.baidu.wakeup.RecogWakeupListener;
import com.otvcloud.tachographdemo.baidu.wakeup.WakeupParams;
import com.otvcloud.tachographdemo.bean.dao.TachographDao;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by android_jy on 2017/10/19.
 */

public class RecorderActivity extends Activity implements SurfaceHolder.Callback, IStatus {

    public final static int VIDEO_WIDTH = 1280;
    public final static int VIDEO_HEIGHT = 720;
    private static final String TAG = RecorderActivity.class.getSimpleName();
    private TextView textView;
    private int textTime = 0;
    private MediaRecorder mRecorder;
    private Camera mCamera;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private int cutTime = 1 * 10;
    private int mCameraType = 0; //当前设置头类型,0:后置/1:前置
    private String mRecorderFilePath = "";
    private static RecorderActivity mRecorderActivity;

    private static final int TIME_SQ = 100;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TIME_SQ:
                    textTime++;
                    //如果当前时间大于切割时间时
                    if (textTime > cutTime) {
                        textTime = 0;
                        //结束的时候要进行数据库操作
                        stopRecorder();
                        startRecorder();
                        sendEmptyMessage(msg.what);
                    } else {
                        removeMessages(msg.what);
                        sendEmptyMessageDelayed(msg.what, 1000);
                    }
                    textView.setText(textTime + "");
                    break;
            }
        }
    };

    /**
     * 识别控制器，使用MyRecognizer控制识别的流程
     */
    protected MyRecognizer myRecognizer;
    protected MyWakeup myWakeup;
    /**
     * 0: 方案1， 唤醒词说完后，直接接句子，中间没有停顿。
     * >0 : 方案2： 唤醒词说完后，中间有停顿，然后接句子。推荐4个字 1500ms
     * <p>
     * backTrackInMs 最大 15000，即15s
     */
    private int backTrackInMs = 1500;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == STATUS_WAKEUP_SUCCESS) {
                // 此处 开始正常识别流程
                Map<String, Object> params = new LinkedHashMap<>();
                params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
                params.put(SpeechConstant.VAD, SpeechConstant.VAD_DNN);
                int pid = PidBuilder.create().model(PidBuilder.INPUT).toPId(); //如识别短句，不需要需要逗号，将PidBuilder.INPUT改为搜索模型PidBuilder.SEARCH
                params.put(SpeechConstant.PID, pid);
                if (backTrackInMs > 0) { // 方案1， 唤醒词说完后，直接接句子，中间没有停顿。
                    params.put(SpeechConstant.AUDIO_MILLS, System.currentTimeMillis() - backTrackInMs);
                }
                myRecognizer.cancel();
                myRecognizer.start(params);
            } else if (msg.what == STATUS_FINISHED) {
                TachographManager.getInstance().checkRecognizer((String) msg.obj);
            }
        }
    };

    public static RecorderActivity getInstance(){
        return mRecorderActivity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setStrictMode();
        setContentView(R.layout.activity_recorder);
        mRecorderActivity = this;
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        textView = (TextView) findViewById(R.id.text);
        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(this);
        // setType必须设置，要不出错.
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        initPermission();
        initRecog();
    }

    /**
     * 初始化语音和唤醒服务
     */
    protected void initRecog() {
        // 初始化识别引擎
        StatusRecogListener recogListener = new MessageStatusRecogListener(handler);
        myRecognizer = new MyRecognizer(this, recogListener);

        IWakeupListener listener = new RecogWakeupListener(handler);
        myWakeup = new MyWakeup(this, listener);
        start();
    }

    /**
     * 开始语音
     */
    private void start() {
        WakeupParams wakeupParams = new WakeupParams(this);
        Map<String, Object> params = wakeupParams.fetch();
        myWakeup.start(params);
    }


    /**
     * 停止语音
     */
    protected void stop() {
        myWakeup.stop();
        myRecognizer.stop();
    }


    /**
     * 开始记录
     */
    private void startRecorder() {
        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
        }
        initCamera(mCameraType);
        try {
            mRecorder.setCamera(mCamera);
            // 这两项需要放在setOutputFormat之前

//            mRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            // Set output file format
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            //必须在setEncoder之前  视频的帧率和视频大小是需要硬件支持的，如果设置的帧率和视频大小,如果硬件不支持就会出现错误。
            // mRecorder.setVideoFrameRate(15);  //帧数  一分钟帧，15帧就够了 华为手机不支持，故注释

            //setVideoSize需要权衡的因素较多，主要包括三方面：MediaRecorder支持的录制尺寸、视频文件的大小以及兼容不同Android机型。这里采用640 * 480（微信小视频的尺寸是320*240），并且市面上99%以上机型支持此录制尺寸。
//            mRecorder.setVideoSize(VIDEO_WIDTH, VIDEO_HEIGHT);

            // 这两项需要放在setOutputFormat之后
//            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//            mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);

//            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

            mRecorder.setVideoSize(640, 480);
            mRecorder.setVideoFrameRate(30);
            mRecorder.setVideoEncodingBitRate(3 * 1024 * 1024);
            mRecorder.setOrientationHint(90);
            //设置记录会话的最大持续时间（毫秒）
            mRecorder.setMaxDuration(30 * 1000);
            mRecorderFilePath = TachographManager.getInstance().getRecorderPath();
            mRecorder.setOutputFile(mRecorderFilePath);

            mRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
            mRecorder.prepare();
            mRecorder.start();
            mHandler.sendEmptyMessage(TIME_SQ);
        } catch (Exception e) {
            e.printStackTrace();
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
        }
    }

    private void stopRecorder() {
        TachographManager.getInstance().checkRecorderSpace();
        TachographDao.getInstance().save(mRecorderFilePath, System.currentTimeMillis() + "");
        mHandler.removeMessages(TIME_SQ);
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
        }
    }

    /**
     * 初始化相机
     *
     * @param type 前后的类型
     */
    private void initCamera(int type) {
        if (mCamera != null) {
            //如果已经初始化过，就先释放
            releaseCamera();
        }

        try {
            mCamera = Camera.open(type);
            if (mCamera == null) {
                return;
            }
            mCamera.lock();

            Camera.Parameters parameters = mCamera.getParameters();
            if (type == 0) {
                //基本是都支持这个比例
                parameters.setPreviewSize(VIDEO_WIDTH, VIDEO_HEIGHT);
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//连续对焦
                mCamera.cancelAutoFocus();// 如果要实现连续的自动对焦，这一句必须加上
            }
            mCamera.setParameters(parameters);
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
            mCamera.unlock();
        } catch (Exception e) {
            e.printStackTrace();
            releaseCamera();
        }
    }

    /**
     * 释放相机资源
     */
    private void releaseCamera() {
        try {
            if (mCamera != null) {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.lock();
                mCamera.release();
                mCamera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
        startRecorder();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mSurfaceHolder = holder;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopRecorder();
        releaseCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        try {
            stopRecorder();
            if (mCamera != null && mCameraType == 0) {
                mCamera.lock();
                mCamera.unlock();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecorder();
        releaseCamera();

        myWakeup.release();
        myRecognizer.release();
    }

    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String permissions[] = {Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                //进入到这里代表没有权限.

            }
        }
        String tmpList[] = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 此处为android 6.0以上动态授权的回调，用户自行实现。
    }

    private void setStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build());

    }
}
