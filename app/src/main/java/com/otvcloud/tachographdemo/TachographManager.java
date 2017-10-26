package com.otvcloud.tachographdemo;

import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.coremedia.iso.boxes.Container;
import com.google.gson.Gson;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.otvcloud.tachographdemo.bean.Tachograph;
import com.otvcloud.tachographdemo.bean.UploadInfo;
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
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by android_jy on 2017/10/20.
 */
public class TachographManager {
    private static final String TAG = TachographManager.class.getSimpleName();
    private static TachographManager manager;
    private static String LOCATION_FILE_PATH = "tachographFile/file";
    private static String LOCATION_CACHE_PATH = "tachographFile/cache";
    private static double MAX_SIZE = 30; //最大空间为M
    private final int DEFAULT_TIMEOUT = 10 * 1000;

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
        String path = getLocationFilePath(LOCATION_FILE_PATH);
        if (path != null) {
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            path = dir + "/" + getDate() + ".mp4";
        }
        return path;
    }

    /**
     * 检测当前记录的空间，自动进行清理
     */
    public void checkRecorderSpace() {
        double fileOrFilesSize = FileSizeUtil.getFileOrFilesSize(getLocationFilePath(LOCATION_FILE_PATH), FileSizeUtil.SIZETYPE_MB);
        if (fileOrFilesSize > MAX_SIZE) {
            //需要删除的信息获取
            List<Tachograph> deleteTachographs = TachographDao.getInstance().findAllTachograph(3, "asc");
            if (deleteTachographs == null) {
                FileSizeUtil.deleteDirectory(getLocationFilePath(LOCATION_FILE_PATH));
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
     * @param location 路径
     * @return
     */
    public String getLocationFilePath(String location) {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(android.os.Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();// 获取跟目录
            File file = new File(sdDir.toString() + "/" + location);
            if (!file.exists()) {
                file.mkdirs();
            }
            return sdDir.toString() + "/" + location;
        }
        return null;
    }

    /**
     * 语音识别成功之后，进行视频截取
     *
     * @param msg
     */
    public void checkRecognizer(String msg) {
        Log.e(TAG, "语音识别结束---checkRecognizer= " + msg);
        if (checkMsg(msg)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String filePath = getLocationFilePath(LOCATION_CACHE_PATH) + "/" + getDate() + ".mp4";
                    List<Tachograph> tachographs = TachographDao.getInstance().findAllTachograph(3, "desc");
                    if (tachographs == null || tachographs.size() <= 1) {
                        return;
                    }

                    //测试文件大小
//                uploadFile(tachographs.get(0).getFilePath());

                    List<String> fileList = new ArrayList<String>();
                    List<Movie> moviesList = new LinkedList<Movie>();
                    for (int i = tachographs.size() - 1; i >= 0; i--) {
                        fileList.add(tachographs.get(i).getFilePath());
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
                        FileChannel fc = new RandomAccessFile(filePath, "rw").getChannel();
                        out.writeContainer(fc);
                        fc.close();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    moviesList.clear();
                    fileList.clear();
                    uploadFile(filePath);
                }
            }).start();
        }
    }

    /**
     * 检测当前文字是否包含，拍，截图，拍视频，上传等
     *
     * @param msg
     * @return
     */
    private boolean checkMsg(String msg) {
        if (msg == null || "".equals(msg)) {
            return false;
        }
        if (msg.contains("拍") || msg.contains("违章")) {
            return true;
        }
        return false;
    }

    /**
     * 视频上传处理
     *
     * @param filePath
     */
    private void uploadFile(final String filePath) {
        Log.e(TAG, "uploadFile----start=" + filePath);
        String uploadHost = "http://ceshi-219.otvcloud.com:5052/cms-api-web/wx/api/";
        String mVideoName = "交通道路违章";
        final File file = new File(filePath);
//        Retrofit retrofit = new Retrofit
//                .Builder()
//                .baseUrl(uploadHost)
//                .build();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        Retrofit retrofit = new Retrofit
                .Builder()
                .baseUrl(uploadHost)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        TestService testService = retrofit.create(TestService.class);
        // 获取文件真实的内容类型
        final RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
        String descriptionString = "This is a description";
        RequestBody description = RequestBody.create(MediaType.parse("multipart/form-data"), descriptionString);
        Call<ResponseBody> call = null;
        try {
            call = testService.uploadVideoFile(description, body, "o0IUwwXqXeQd6nKnHKcktIpXfvP0", "用户输入的名称",
                    mVideoName == null || mVideoName.equals("") ? file.getName() : mVideoName + "233", "1");
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (null == response || response.body() == null) {
                        return;
                    }
                    Gson gson = new Gson();
                    UploadInfo uplod = new UploadInfo();
                    try {
                        uplod = gson.fromJson(response.body().string(), UploadInfo.class);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    File file1 = new File(filePath);
                    if (uplod != null && uplod.code == 0 && uplod.data != null && uplod.data.code == 0) {
                        if (RecorderActivity.getInstance() != null) {
                            Toast.makeText(RecorderActivity.getInstance(), "上传成功", Toast.LENGTH_SHORT).show();
                        }
                        Log.e(TAG, "uploadFile-----------------上传成功");
                        file1.delete();
                    } else {
                        Log.e(TAG, "uploadFile-----------------上传失败");
//                        Toast.makeText(MainActivity.this, "上传失败，请稍后再试!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
//                    Toast.makeText(MainActivity.this, "上传失败，请稍后再试!", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "uploadFile-----------------上传失败e=");
                    t.printStackTrace();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "uploadFile-----------------文件路径解析失败，请选择其他目录下的文件!");
//            Toast.makeText(MainActivity.this, "文件路径解析失败，请选择其他目录下的文件!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
