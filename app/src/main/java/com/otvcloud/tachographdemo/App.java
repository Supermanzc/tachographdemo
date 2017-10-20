package com.otvcloud.tachographdemo;

import com.orm.SugarApp;
import com.orm.SugarContext;

/**
 * Created by android_jy on 2017/10/20.
 */

public class App extends SugarApp {

    @Override
    public void onCreate() {
        super.onCreate();
        SugarContext.init(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
