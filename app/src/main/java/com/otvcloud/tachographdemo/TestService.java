package com.otvcloud.tachographdemo;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

/**
 * Created by otvcloud on 2017/10/24.
 */

public interface TestService {

    @Multipart
    @POST("importVideo")
    Call<ResponseBody> uploadVideoFile(@Part("description") RequestBody description, @Part MultipartBody.Part file,
                                       @Query("openid") String openid, @Query("name") String name,
                                       @Query("fileName") String fileName, @Query("sign") String sign);
}
