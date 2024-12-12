package com.example.jenggala.Home.SHP

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.jenggala.API.ApiInterface
import com.example.jenggala.API.RetrofitInstance
import com.example.jenggala.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddRespondenSHPFragment : DialogFragment() {

    private lateinit var spinnerKabkota: Spinner
    private lateinit var spinnerKecamatan: Spinner
    private lateinit var spinnerKelDesa: Spinner
    private lateinit var btnCancel: Button

    private val apiService: ApiInterface = RetrofitInstance.api

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_responden_s_h_p, container, false)

        // Inisialisasi views
        spinnerKabkota = view.findViewById(R.id.spinnerKabKota)
        spinnerKecamatan = view.findViewById(R.id.spinnerKecamatan)
        spinnerKelDesa = view.findViewById(R.id.spinnerKelDesa)
        btnCancel = view.findViewById(R.id.btnCancel)

        // Awalnya sembunyikan spinner kecamatan dan kelurahan/desa
        spinnerKecamatan.visibility = View.GONE
        spinnerKelDesa.visibility = View.GONE

        // Ambil data kabupaten/kota
        fetchKabKota()

        // Tombol batal
        btnCancel.setOnClickListener {
            dismiss() // Tutup dialog
        }

        return view
    }

    private fun fetchKabKota() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val kabkotaList = apiService.getKabkot()
                if (kabkotaList.isSuccessful) {
                    val kabKotaNames = mutableListOf("Pilih Kabupaten/Kota")
                    kabKotaNames.addAll(kabkotaList.body()?.wilayah?.map { it.nama } ?: emptyList())

                    withContext(Dispatchers.Main) {
                        val adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            kabKotaNames
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerKabkota.adapter = adapter

                        spinnerKabkota.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                                if (position == 0) {
                                    // Jika placeholder dipilih, sembunyikan spinner lainnya
                                    spinnerKecamatan.visibility = View.GONE
                                    spinnerKelDesa.visibility = View.GONE
                                } else {
                                    // Tampilkan spinner kecamatan jika kabkot dipilih
                                    spinnerKecamatan.visibility = View.VISIBLE
                                    val selectedKabkot = kabkotaList.body()?.wilayah?.get(position - 1)
                                    selectedKabkot?.let {
                                        fetchKecamatan(it.kode)
                                    }
                                }
                            }

                            override fun onNothingSelected(parent: AdapterView<*>) {}
                        }
                    }
                } else {
                    Log.e("AddRespondenSHPFragment", "Failed to fetch kabkot: ${kabkotaList.errorBody()}")
                }
            } catch (e: Exception) {
                Log.e("AddRespondenSHPFragment", "Error fetching kabkot: ${e.message}")
            }
        }
    }

    private fun fetchKecamatan(kodeKabkot: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val kecamatanList = apiService.getKecamatan(kodeKabkot)
                if (kecamatanList.isSuccessful) {
                    val kecamatanNames = mutableListOf("Pilih Kecamatan")
                    kecamatanNames.addAll(kecamatanList.body()?.wilayah?.map { it.nama } ?: emptyList())

                    withContext(Dispatchers.Main) {
                        val adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            kecamatanNames
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerKecamatan.adapter = adapter

                        spinnerKecamatan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                                if (position == 0) {
                                    // Jika placeholder dipilih, sembunyikan spinner kelurahan/desa
                                    spinnerKelDesa.visibility = View.GONE
                                } else {
                                    // Tampilkan spinner kelurahan/desa jika kecamatan dipilih
                                    spinnerKelDesa.visibility = View.VISIBLE
                                    val selectedKecamatan = kecamatanList.body()?.wilayah?.get(position - 1)
                                    selectedKecamatan?.let {
                                        fetchKelDesa(it.kode)
                                    }
                                }
                            }

                            override fun onNothingSelected(parent: AdapterView<*>) {}
                        }
                    }
                } else {
                    Log.e("AddRespondenSHPFragment", "Failed to fetch kecamatan: ${kecamatanList.errorBody()}")
                }
            } catch (e: Exception) {
                Log.e("AddRespondenSHPFragment", "Error fetching kecamatan: ${e.message}")
            }
        }
    }

    private fun fetchKelDesa(kodeKecamatan: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val kelDesaList = apiService.getKeldes(kodeKecamatan)
                if (kelDesaList.isSuccessful) {
                    val kelDesaNames = mutableListOf("Pilih Kelurahan/Desa")
                    kelDesaNames.addAll(kelDesaList.body()?.wilayah?.map { it.nama } ?: emptyList())

                    withContext(Dispatchers.Main) {
                        val adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            kelDesaNames
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerKelDesa.adapter = adapter
                    }
                } else {
                    Log.e("AddRespondenSHPFragment", "Failed to fetch kelurahan/desa: ${kelDesaList.errorBody()}")
                }
            } catch (e: Exception) {
                Log.e("AddRespondenSHPFragment", "Error fetching kelurahan/desa: ${e.message}")
            }
        }
    }
}