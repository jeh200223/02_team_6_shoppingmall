package com.lion.a02_team_6_shoppingmall.repository

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface RetrofitRequestInterface {

    @GET("maps/api/place/nearbysearch/json")
    fun requestPlaceApi(
        @Query("location") location:String,
        @Query("radius") radius:String,
        @Query("language") language:String,
        @Query("type") type:String,
        @Query("key") key:String
    ): Call<PlaceData>
}