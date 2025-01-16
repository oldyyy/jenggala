package com.example.jenggala.API

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiInterface {

    @FormUrlEncoded
    @POST("login")
    suspend fun loginUser(@Field("username") username: String, @Field("password") password: String): Response<GlobalResponse>

    @FormUrlEncoded
    @POST("user-info")
    suspend fun getUserDetails(@Field("username") username: String, @Field("auth_token") auth_token: String): Response<GlobalResponse>

    @FormUrlEncoded
    @POST("plotting")
    suspend fun getPlottingDetails(@Field("kode_petugas") kode_petugas: String): Response<GlobalResponse>

    @FormUrlEncoded
    @POST("tracking")
    suspend fun insertTracking(
        @Field("kode_responden") kode_responden: String,
        @Field("kode_petugas") kode_petugas: String,
        @Field("kode_kegiatan") kode_kegiatan: String,
        @Field("jam_mulai") jam_mulai: String,
        @Field("jam_selesai") jam_selesai: String,
        @Field("total_waktu") total_waktu: String,
        @Field("latitude_start") latitude_start: Double,
        @Field("longitude_start") longitude_start: Double,
        @Field("latitude_stop") latitude_stop: Double,
        @Field("longitude_stop") longitude_stop: Double,
        @Field("kode_status") kode_status: Int,
        @Field("keterangan") keterangan: String?
    ): Response<GlobalResponse>

    @GET("kabkot")
    suspend fun getKabkot(): Response<GlobalResponse>

    @FormUrlEncoded
    @POST("kecamatan")
    suspend fun getKecamatan(@Field("kode_kabkot") kode_kabkot: String): Response<GlobalResponse>

    @FormUrlEncoded
    @POST("keldes")
    suspend fun getKeldes(@Field("kode_kecamatan") kode_kecamatan: String): Response<GlobalResponse>
}