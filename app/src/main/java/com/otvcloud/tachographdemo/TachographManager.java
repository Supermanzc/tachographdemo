package com.otvcloud.tachographdemo;

import android.os.Environment;
import android.util.Log;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.otvcloud.tachographdemo.bean.Tachograph;
import com.otvcloud.tachographdemo.bean.dao.TachographDao;
import com.otvcloud.tachographdemo.util.FileSizeUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by android_jy on 2017/10/20.
 */
public class TachographManager {
    private static final String TAG = TachographManager.class.getSimpleName();
    private static TachographManager manager;
    private static String LOCATION_FILE_PATH = "tachographFile";
    private static double MAX_SIZE = 30; //最大空间为M

    private TachographManager() {
    }

    public static TachographManager getInstance() {
        if (manager == null) {
            manager = new TachographManager();
        }
        return manager;
    }

    /**
     * 根据当前时间获取记录路径
     *
     * @return
     */
    public String getRecorderPath() {
        String path = getLocationFilePath();
        if (path != null) {
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdir();
            }
            path = dir + "/" + getDate() + ".mp4";
        }
        return path;
    }

    /**
     * 检测当前记录的空间，自动进行清理
     */
    public void checkRecorderSpace() {
        double fileOrFilesSize = FileSizeUtil.getFileOrFilesSize(getLocationFilePath(), FileSizeUtil.SIZETYPE_MB);
        if (fileOrFilesSize > MAX_SIZE) {
            //需要删除的信息获取
            List<Tachograph> deleteTachographs = TachographDao.getInstance().findAllTachograph(3, "asc");
            if (deleteTachographs == null) {
                FileSizeUtil.deleteDirectory(getLocationFilePath());
            } else {
                for (Tachograph tachograph : deleteTachographs) {
                    FileSizeUtil.deleteFile(tachograph.getFilePath());
                    tachograph.delete();
                }
            }
        }
    }

    /**
     * 获取系统时间
     *
     * @return
     */
    public String getDate() {
        Calendar ca = Calendar.getInstance();
        int year = ca.get(Calendar.YEAR);           // 获取年份
        int month = ca.get(Calendar.MONTH);         // 获取月份
        int day = ca.get(Calendar.DATE);            // 获取日
        int minute = ca.get(Calendar.MINUTE);       // 分
        int hour = ca.get(Calendar.HOUR);           // 小时
        int second = ca.get(Calendar.SECOND);       // 秒

        String date = "" + year + (month + 1) + day + hour + minute + second;
        Log.d(TAG, "date:" + date);
        return date;
    }

    /**
     * 获取当前的文件路径
     *
     * @return
     */
    public String getLocationFilePath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(android.os.Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();// 获取跟目录
            return sdDir.toString() + "/" + LOCATION_FILE_PATH;
        }
        return null;
    }

    /**
     * 语音识别成功之后，进行视频截取
     *
     * @param msg
     */
    public void checkRecognizer(String msg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Tachograph> tachographs = TachographDao.getInstance().findAllTachograph(3, "desc");
                List<String> fileList = new ArrayList<String>();
                List<Movie> moviesList = new LinkedList<Movie>();
                for (Tachograph tachograph : tachographs) {
                    fileList.add(tachograph.getFilePath());
                }
                try {
                    for (String file : fileList) {
                        moviesList.add(MovieCreator.build(file));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                List<Track> videoTracks = new LinkedList<Track>();
                List<Track> audioTracks = new LinkedList<Track>();
                for (Movie m : moviesList) {
                    for (Track t : m.getTracks()) {
                        if (t.getHandler().equals("soun")) {
                            audioTracks.add(t);
                        }
                        if (t.getHandler().equals("vide")) {
                            videoTracks.add(t);
                        }
                    }
                }
                Movie result = new Movie();
                try {
                    if (audioTracks.size() > 0) {
                        result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
                    }
                    if (videoTracks.size() > 0) {
                        result.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Container out = new DefaultMp4Builder().build(result);
                try {
                    FileChannel fc = new RandomAccessFile(getLocationFilePath() + "/output.mp4", "rw").getChannel();
                    out.writeContainer(fc);
                    fc.close();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                moviesList.clear();
                fileList.clear();
            }
        }).start();
    }
}
