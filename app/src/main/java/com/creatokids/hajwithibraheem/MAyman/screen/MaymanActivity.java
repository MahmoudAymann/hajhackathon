package com.creatokids.hajwithibraheem.MAyman.screen;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.creatokids.hajwithibraheem.MAyman.api.ApiLink;
import com.creatokids.hajwithibraheem.MAyman.model.RequirdData;
import com.creatokids.hajwithibraheem.MAyman.model.ResponseModule;
import com.creatokids.hajwithibraheem.R;

import java.util.Calendar;
import java.util.Date;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MaymanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setData("1", "1236","msg to send", "21.616862500000003","39.15633203125001","02-08-2018");

    }
    private void setData(String calleId, String userQrCode, String msg, String lat, String longX, String dateTime) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        Retrofit retrofit =
                new Retrofit.Builder().baseUrl("http://rashedcallrequestapi.gear.host/RashedAssistant/Call/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .client(client)
                        .build();
        ApiLink apiLink = retrofit.create(ApiLink.class);

        RequirdData data = new RequirdData();
        data.setCalleeId(calleId);
        data.setUserId(userQrCode);
        data.setMessage(msg);
        data.setLatitude(lat);
        data.setLongitude(longX);
        data.setTime(dateTime);

        Call<ResponseModule> callModel = apiLink.getData(data);
        callModel.enqueue(new Callback<ResponseModule>() {
            @Override
            public void onResponse(@NonNull Call<ResponseModule> call, @NonNull Response<ResponseModule> response) {

                Toast.makeText(MaymanActivity.this, ""+response.body().getStatus().getMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(@NonNull Call<ResponseModule> call, @NonNull Throwable t) {

            }
        });

    }
    //if u need it
    private Date getCurrentDate() {
        return Calendar.getInstance().getTime();
    }

}

