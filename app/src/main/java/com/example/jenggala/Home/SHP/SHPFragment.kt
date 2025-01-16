package com.example.jenggala.Home.SHP

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jenggala.API.Responden
import com.example.jenggala.API.RetrofitInstance
import com.example.jenggala.Home.RespondenService.RespondenAdapter
import com.example.jenggala.R
import com.example.jenggala.databinding.FragmentSHPBinding
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SHPFragment : Fragment() {

    private lateinit var binding: FragmentSHPBinding
    private lateinit var respondenAdapter: RespondenAdapter
    private var originalRespondenList: List<Responden> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSHPBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Tambahkan menu info responden
        setHasOptionsMenu(true)

        // Mengatur Toolbar sebagai ActionBar dan mengaktifkan tombol kembali
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Mengatur tindakan onClick untuk tombol kembali di toolbar
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Setup RecyclerView
        respondenAdapter = RespondenAdapter(fragmentOrigin = "SHPFragment")
        binding.recyclerViewShp.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = respondenAdapter
        }

        // Ambil kode_petugas dan panggil data responden
        val kode_petugas = getKodePetugas()
        Log.d("check kode petugas", kode_petugas)
        if (kode_petugas.isNotEmpty()) {
            fetchRespondenData(kode_petugas)
        } else {
            Toast.makeText(activity, "Kode Petugas tidak ditemukan", Toast.LENGTH_SHORT).show()
        }

        // Mengatur filter responden
        setupFilterButtons()
    }

    private fun getKodePetugas(): String {
        val prefs = activity?.getSharedPreferences("myNewPrefs", MODE_PRIVATE)
        return prefs?.getString("kode_petugas", "") ?: ""
    }

    private fun fetchRespondenData(kode_petugas: String) {
        // Tampilkan ProgressBar
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewShp.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = RetrofitInstance.api.getPlottingDetails(kode_petugas)
                val responseBody = result.body()

                withContext(Dispatchers.Main) {
                    if (result.isSuccessful && responseBody != null) {
                        val plottingResponse = responseBody.plotting
                        val respondenList = mutableListOf<Responden>()

                        // Filter hanya survei SHP
                        plottingResponse?.data_survei?.get("1001")?.responden?.let { responden ->
                            responden.forEach { it.kode_kegiatan = "1001" }
                            val sortedResponden = responden.sortedBy { it.nama_perusahaan?.lowercase() }
                            respondenList.addAll(sortedResponden)
                        }
                        if (respondenList.isEmpty()) {
                            Toast.makeText(activity, "Tidak ada data responden", Toast.LENGTH_SHORT).show()
                        } else {
                            displayRespondenList(respondenList)
                        }

                    } else {
                        Toast.makeText(activity, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                        Log.d("check data responden", result.message())
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "Gagal menghubungkan ke server", Toast.LENGTH_SHORT).show()
                    Log.d("check data api", e.message.toString())
                }
            } finally {
                withContext(Dispatchers.Main) {
                    // Sembunyikan ProgressBar setelah selesai
                    binding.progressBar.visibility = View.GONE
                    binding.recyclerViewShp.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun displayRespondenList(respondenList: List<Responden>) {
        // Simpan daftar responden asli
        originalRespondenList = respondenList
        // Tampilkan daftar responden di adapter
        respondenAdapter.submitList(originalRespondenList)

        // Hitung jumlah responden berdasarkan status
        val jumlahResponden = originalRespondenList.size
        val jumlahBerhasil = originalRespondenList.count { it.kode_status == 1 }
        val jumlahMenolak = originalRespondenList.count { it.kode_status == 2 }
        val jumlahReschedule = originalRespondenList.count { it.kode_status == 3 }
        val jumlahMenunggu = originalRespondenList.count { it.kode_status == 4 }

        // Tampilkan jumlah responden berdasarkan status
        binding.valueBebanCacah.text = "$jumlahResponden"
        binding.statusBerhasil.text = "Berhasil: $jumlahBerhasil"
        binding.statusMenolak.text = "Menolak: $jumlahMenolak"
        binding.statusReschedule.text = "Reschedule: $jumlahReschedule"
        binding.statusMenunggu.text = "Menunggu: $jumlahMenunggu"
    }

    private fun filterRespondenList(filterStatus: String) {
        if (originalRespondenList.isNotEmpty()) {
            val filteredList = when (filterStatus) {
                "ALL" -> originalRespondenList
                "BERHASIL" -> originalRespondenList.filter { it.kode_status == 1 }
                "MENOLAK" -> originalRespondenList.filter { it.kode_status == 2 }
                "RESCHEDULE" -> originalRespondenList.filter { it.kode_status == 3 }
                "MENUNGGU" -> originalRespondenList.filter { it.kode_status == 4 }
                else -> originalRespondenList
            }
            respondenAdapter.submitList(filteredList)
        }
    }

    private var activeFilterButton: MaterialButton? = null

    private fun setActiveFilter(selectedButton: MaterialButton, filterStatus: String) {
        // Reset warna tombol sebelumnya
        activeFilterButton?.apply {
            setBackgroundColor(resources.getColor(R.color.brown_light, null))
            setTextColor(resources.getColor(R.color.white, null))
        }

        // Ubah warna tombol yang aktif
        selectedButton.apply {
            setBackgroundColor(resources.getColor(R.color.brown_base, null))
            setTextColor(resources.getColor(android.R.color.white, null))
        }

        // Atur tombol aktif saat ini
        activeFilterButton = selectedButton

        // Lakukan filter berdasarkan status
        filterRespondenList(filterStatus)
    }

    private fun setupFilterButtons() {
        // Atur filter default sebagai "Semua"
        setActiveFilter(binding.filterAll, "ALL")

        // Set OnClickListener untuk setiap tombol filter
        binding.filterAll.setOnClickListener {
            setActiveFilter(binding.filterAll, "ALL")
        }
        binding.filterBerhasil.setOnClickListener {
            setActiveFilter(binding.filterBerhasil, "BERHASIL")
        }
        binding.filterMenolak.setOnClickListener {
            setActiveFilter(binding.filterMenolak, "MENOLAK")
        }
        binding.filterReschedule.setOnClickListener {
            setActiveFilter(binding.filterReschedule, "RESCHEDULE")
        }
        binding.filterMenunggu.setOnClickListener {
            setActiveFilter(binding.filterMenunggu, "MENUNGGU")
        }
    }

    // Tambahkan menu info responden
//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        inflater.inflate(R.menu.add_responden, menu)
//        super.onCreateOptionsMenu(menu, inflater)
//    }

    // Tambahkan menu info responden
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add_responden -> {
                val dialog = AddRespondenSHPFragment()
                dialog.show(parentFragmentManager, "AddRespondenSHPFragment")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}