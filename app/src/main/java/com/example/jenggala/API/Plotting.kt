package com.example.jenggala.API

data class Plotting(
    val kode_petugas: String,
    val data_survei: Map<String, SurveiData>
)