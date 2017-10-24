package com.otvcloud.tachographdemo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by android_jy on 2017/10/24.
 */

public class RecognizerWakeActivity extends Activity implements IStatus {

    /**
     * 识别控制器，使用MyRecognizer控制识别的流程
     */
    protected MyRecognizer myRecognizer;
    protected MyWakeup myWakeup;
    /**
     *  0: 方案1， 唤醒词说完后，直接接句子，中间没有停顿。
     * >0 : 方案2： 唤醒词说完后，中间有停顿，然后接句子。推荐4个字 1500ms
     *
     *  backTrackInMs 最大 15000，即15s
     */
    private int backTrackInMs = 1500;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == STATUS_WAKEUP_SUCCESS){
                // 此处 开始正常识别流程
                Map<String, Object> params = new LinkedHashMap<String, Object>();
                params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
                params.put(SpeechConstant.VAD,SpeechConstant.VAD_DNN);
                int pid = PidBuilder.create().model(PidBuilder.INPUT).toPId(); //如识别短句，不需要需要逗号，将PidBuilder.INPUT改为搜索模型PidBuilder.SEARCH
                params.put(SpeechConstant.PID,pid);
                if (backTrackInMs > 0) { // 方案1， 唤醒词说完后，直接接句子，中间没有停顿。
                    params.put(SpeechConstant.AUDIO_MILLS, System.currentTimeMillis() - backTrackInMs);
                }
                myRecognizer.cancel();
                myRecognizer.start(params);
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_wake);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
}
