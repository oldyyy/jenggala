package com.example.jenggala.Home.RespondenService

import android.graphics.Color
import android.net.Uri
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.findNavController
import com.example.jenggala.API.Responden
import com.example.jenggala.R
import com.example.jenggala.Home.SHP.SHPFragmentDirections
import com.example.jenggala.Home.SHPB.SHPBFragmentDirections
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.text.Spanned

class RespondenAdapter(
    private val fragmentOrigin: String
) : ListAdapter<Responden, RespondenAdapter.RespondenViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RespondenViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card_responden, parent, false)
        return RespondenViewHolder(view)
    }

    override fun onBindViewHolder(holder: RespondenViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RespondenViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val namaPerusahaan = itemView.findViewById<TextView>(R.id.nama_perusahaan)
        private val alamatPerusahaan = itemView.findViewById<TextView>(R.id.alamat_perusahaan)
        private val detailAlamat = itemView.findViewById<TextView>(R.id.detail_alamat_perusahaan)
        private val teleponPerusahaan = itemView.findViewById<TextView>(R.id.telepon_perusahaan)
        private val longlatPerusahaan = itemView.findViewById<TextView>(R.id.longlat_perusahaan)
        private val statusResponden = itemView.findViewById<TextView>(R.id.status_responden)
        private val expandedLayout = itemView.findViewById<LinearLayout>(R.id.expanded_layout)
        private val expandButton = itemView.findViewById<Button>(R.id.expand_button)
        private val gotoTrackingFragment = itemView.findViewById<ImageButton>(R.id.goto_tracking_btn)

        fun bind(responden: Responden) {
            namaPerusahaan.text = responden.nama_perusahaan
            alamatPerusahaan.text = responden.alamat_perusahaan
            detailAlamat.text = "${responden.kode_keldes}, ${responden.kode_kecamatan}, ${responden.kode_kabkot}, ${responden.kode_prov}"
            teleponPerusahaan.text = "Telepon: ${responden.no_telepon}"

            val fullText = "\uD83D\uDCCDLihat Lokasi"
            val spannableString = SpannableString(fullText)
            val startIndex = fullText.indexOf("Lihat Lokasi")
            val endIndex = startIndex + "Lihat Lokasi".length
            spannableString.setSpan(UnderlineSpan(), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannableString.setSpan(ForegroundColorSpan(Color.BLUE), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            longlatPerusahaan.text = spannableString
            longlatPerusahaan.setOnClickListener {
                val mapsUrl = "https://www.google.com/maps/search/?api=1&query=${responden.longitude},${responden.latitude}"
                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl))
                itemView.context.startActivity(mapIntent)
            }

            statusResponden.text = when (responden.kode_status) {
                1 -> "BERHASIL"
                2 -> "MENOLAK"
                3 -> "RESCHEDULE"
                4 -> "MENUNGGU"
                else -> "Status: Tidak Diketahui"
            }

            expandedLayout.isVisible = false
            expandButton.setOnClickListener {
                expandedLayout.isVisible = !expandedLayout.isVisible
                expandButton.text = if (expandedLayout.isVisible) "Sembunyikan" else "Lihat Informasi"
            }

            // Navigasi ke TrackingFragment
            gotoTrackingFragment.apply {
                if (responden.kode_status == 1 || responden.kode_status == 2) {
                    visibility = View.GONE
                } else {
                    visibility = View.VISIBLE
                }

                setOnClickListener {
                    val action = when (fragmentOrigin) {
                        "SHPFragment" -> SHPFragmentDirections.actionSHPFragmentToTrackingFragment(responden, fragmentOrigin)
                        "SHPBFragment" -> SHPBFragmentDirections.actionSHPBFragmentToTrackingFragment(responden, fragmentOrigin)
                        else -> null
                    }
                    action?.let { navAction ->
                        itemView.findNavController().navigate(navAction)
                    }
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Responden>() {
            override fun areItemsTheSame(oldItem: Responden, newItem: Responden) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Responden, newItem: Responden) = oldItem == newItem
        }
    }
}