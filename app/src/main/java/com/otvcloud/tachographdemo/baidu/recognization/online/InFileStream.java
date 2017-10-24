package com.otvcloud.tachographdemo.baidu.recognization.online;

import android.app.Activity;

import com.otvcloud.tachographdemo.baidu.util.Logger;

import java.io.IOException;
import java.io.InputStream;
/**
 * Created by fujiayi on 2017/6/20.
 */

public class InFileStream {

    private static Activity context;

    private static final String TAG = "InFileStream";
    public static void setContext(Activity context){
        InFileStream.context = context;
    }

    public static InputStream create16kStream(){
        InputStream is = null;
        Logger.info(TAG,"cmethod call");
        try {
            is = context.getAssets().open("outfile.pcm");
            Logger.info(TAG,"create input stream ok" + is.available());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return is;
    }
}