package com.bvantage.ipfication.demoapp.network;

import com.bvantage.ipfication.demoapp.request.OauthRequest;
import com.bvantage.ipfication.demoapp.response.OauthResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface Service {
    @Headers("Cache-Control: no-cache")
    @POST("/api/v1/oauth/token")
    Call<OauthResponse> generateAccessToken(@Body OauthRequest request);
}
