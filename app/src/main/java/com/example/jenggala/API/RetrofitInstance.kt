package com.example.jenggala.API

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import com.google.gson.GsonBuilder

object RetrofitInstance {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    val gson = GsonBuilder().setLenient().create()

    val api: ApiInterface by lazy {
        Retrofit.Builder()
            .baseUrl("https://jenggala.cathajatim.id/api/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiInterface::class.java)
    }

}
