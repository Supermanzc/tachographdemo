package com.otvcloud.tachographdemo;

import android.app.Activity;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;

import com.otvcloud.tachographdemo.bean.dao.TachographDao;

/**
 * Created by android_jy on 2017/10/19.
 */

public class RecorderActivity extends Activity implements SurfaceHolder.Callback {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorder);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        textView = (TextView) findViewById(R.id.text);
        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(this);
        // setType必须设置，要不出错.
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
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
            mRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
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
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

            mRecorder.setVideoSize(640, 480);
            mRecorder.setVideoFrameRate(30);
            mRecorder.setVideoEncodingBitRate(3 * 1024 * 1024);
            mRecorder.setOrientationHint(90);
            //设置记录会话的最大持续时间（毫秒）
            mRecorder.setMaxDuration(30 * 1000);
            mRecorderFilePath = TachographManager.getRecorderPath();
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
        TachographManager.checkRecorderSpace();
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
//                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//连续对焦
//                mCamera.cancelAutoFocus();// 如果要实现连续的自动对焦，这一句必须加上
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
    }
}
