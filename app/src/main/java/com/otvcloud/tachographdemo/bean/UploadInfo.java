package com.otvcloud.tachographdemo.bean;

/**
 * Created by otvcloud on 2017/10/24.
 */

public class UploadInfo {

    public int code;
    public String message;
    public upload data;

    public static class upload{
        public int code;
        public String message;
        public int flag;
        public String fileId;
    }
}
