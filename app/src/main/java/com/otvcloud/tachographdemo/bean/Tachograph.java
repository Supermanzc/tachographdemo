package com.otvcloud.tachographdemo.bean;

import com.orm.SugarRecord;

import java.io.Serializable;

/**
 * Created by android_jy on 2017/10/20.
 */

public class Tachograph extends SugarRecord implements Serializable {
    private String filePath;
    private String createTime;

    public String getFilePath() {
        return filePath;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}
