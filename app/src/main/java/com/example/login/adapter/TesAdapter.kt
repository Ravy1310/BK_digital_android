// File: app/src/main/java/com/example/login/adapter/TesAdapter.kt
package com.example.login.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.login.R
import com.example.login.models.DetailTes

class TesAdapter(
    private val tesList: List<DetailTes>,
    private val onItemClick: (DetailTes) -> Unit
) : RecyclerView.Adapter<TesAdapter.TesViewHolder>() {

    class TesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNamaTes: TextView = itemView.findViewById(R.id.tvNamaTes)
        val tvDeskripsi: TextView = itemView.findViewById(R.id.tvDeskripsi)
        val tvJumlahSoal: TextView = itemView.findViewById(R.id.tvJumlahSoal)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tes, parent, false)
        return TesViewHolder(view)
    }

    override fun onBindViewHolder(holder: TesViewHolder, position: Int) {
        val tes = tesList[position]

        holder.tvNamaTes.text = tes.namaTes
        holder.tvDeskripsi.text = tes.deskripsi
        holder.tvJumlahSoal.text = "${tes.jumlahSoal} soal"

        // Set status
        if (tes.status == "aktif") {
            holder.tvStatus.text = "Aktif"
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_aktif)
            holder.tvStatus.setTextColor(Color.WHITE)
        } else {
            holder.tvStatus.text = "Nonaktif"
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_nonaktif)
            holder.tvStatus.setTextColor(Color.GRAY)
        }

        // Set click listener
        holder.itemView.setOnClickListener {
            onItemClick(tes)
        }
    }

    override fun getItemCount(): Int = tesList.size
}