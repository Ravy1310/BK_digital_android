package com.example.login.adapter


import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.login.R
import com.example.login.models.Guru

class GuruAdapter(
    private var guruList: List<Guru>,
    private val onItemClick: (Guru) -> Unit
) : RecyclerView.Adapter<GuruAdapter.GuruViewHolder>() {

    // Update data
    fun updateData(newList: List<Guru>) {
        guruList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuruViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_guru, parent, false)
        return GuruViewHolder(view)
    }

    override fun onBindViewHolder(holder: GuruViewHolder, position: Int) {
        val guru = guruList[position]
        holder.bind(guru, position + 1)

        // Set background warna bergantian
        if (position % 2 == 0) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFFFFF"))
        } else {
            holder.itemView.setBackgroundColor(Color.parseColor("#F8F8F8"))
        }

        // Click listener
        holder.itemView.setOnClickListener {
            onItemClick(guru)
        }
    }

    override fun getItemCount(): Int = guruList.size

    inner class GuruViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNo: TextView = itemView.findViewById(R.id.tv_no)
        private val tvNama: TextView = itemView.findViewById(R.id.tv_nama)
        private val tvTelepon: TextView = itemView.findViewById(R.id.tv_telepon)
        private val tvAlamat: TextView = itemView.findViewById(R.id.tv_alamat)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)

        fun bind(guru: Guru, nomor: Int) {
            tvNo.text = nomor.toString()
            tvNama.text = guru.nama
            tvTelepon.text = guru.telepon
            tvAlamat.text = guru.alamat
            tvStatus.text = guru.status

            // Set warna status
            if (guru.status == "Aktif") {
                tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.green))
            } else {
                tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.red))
            }
        }
    }
}