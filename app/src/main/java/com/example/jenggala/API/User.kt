package com.example.jenggala.API

data class User(
    val id: Int,
    val nama: String,
    val username: String,
    val email: String,
    val noHp : String,
    val kabkot : String,
    val alamat : String,
    val pengawas : String,
    val password : String,
    val kode_petugas : String,
    val auth_token: String? = null
)