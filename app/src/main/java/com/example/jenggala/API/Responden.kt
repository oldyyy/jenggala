package com.example.jenggala.API

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Responden(
    val id: Int,
    var kode_kegiatan: String? = null,
    val kode_responden: String,
    val nama_perusahaan: String,
    val alamat_perusahaan: String,
    val kode_prov: String,
    val kode_kabkot: String,
    val kode_kecamatan: String,
    val kode_keldes: String,
    val no_telepon: String,
    val kode_status: Int,
    val longitude: Double,
    val latitude: Double
) : Parcelable