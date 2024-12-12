package com.example.jenggala.API

data class GlobalResponse(
    val plotting: Plotting? = null,
    val result: Result,
    val user: User? = null,
    val wilayah: List<Wilayah>? = null,
)