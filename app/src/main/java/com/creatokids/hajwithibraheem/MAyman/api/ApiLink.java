package com.creatokids.hajwithibraheem.MAyman.api;


import com.creatokids.hajwithibraheem.MAyman.model.RequirdData;
import com.creatokids.hajwithibraheem.MAyman.model.ResponseModule;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Created by MahmoudAyman on 12/06/2018.
 */

public interface ApiLink {
    @POST("RequestCall")
    Call<ResponseModule> getData(@Body RequirdData sentData);

}
