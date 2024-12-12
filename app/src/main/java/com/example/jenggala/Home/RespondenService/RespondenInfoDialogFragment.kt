package com.example.jenggala.Home.RespondenService

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.jenggala.R

class RespondenInfoDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_responden_info_dialog, container, false)

        val Rnama: TextView = view.findViewById(R.id.tvRespondenName)
        val Ralamat: TextView = view.findViewById(R.id.tvRespondenAddress)
        val RnoHp: TextView = view.findViewById(R.id.tvRespondenPhone)
        val btnClose: Button = view.findViewById(R.id.btnClose)

        // Retrieve Responden data from arguments
        val nama = arguments?.getString("nama") ?: "Unknown"
        val alamat = arguments?.getString("alamat") ?: "Location Unknown"
        val noHp = arguments?.getString("noHp") ?: "08xxxxxxxxxx"

        // Set the data to views
        Rnama.text = nama
        Ralamat.text = "Alamat: $alamat"
        RnoHp.text = "No Hp: $noHp"

        // Close button action
        btnClose.setOnClickListener {
            dismiss() // Close the dialog
        }

        return view
    }
}