// File: app/src/main/java/com/example/login/adapter/KelolaSoalAdapter.kt
package com.example.login.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.login.R
import com.example.login.api.ApiClient
import com.example.login.models.TesDetail
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KelolaSoalAdapter(
    private val tesList: List<TesDetail>, // Gunakan TesDetail bukan KelolaSoalResponse
    private val onItemClick: (TesDetail) -> Unit // Callback dengan parameter TesDetail
) : RecyclerView.Adapter<KelolaSoalAdapter.TesViewHolder>() {

    private val TAG = "KelolaSoalAdapter"
    private var currentList = tesList.toMutableList()

    inner class TesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvKategoriTes: TextView = itemView.findViewById(R.id.tvKategoriTes)
        val tvDeskripsiTes: TextView = itemView.findViewById(R.id.tvDeskripsiTes)
        val tvStatusTes: TextView = itemView.findViewById(R.id.tvStatusTes)
        val tvJumlahSoal: TextView = itemView.findViewById(R.id.tvJumlahSoal)
        val btnToggleStatus: MaterialButton = itemView.findViewById(R.id.btnToggleStatus)
        val btnEditTes: MaterialButton = itemView.findViewById(R.id.btnEditTes)
        val btnHapusTes: MaterialButton = itemView.findViewById(R.id.btnHapusTes)

        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(currentList[adapterPosition])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_soal, parent, false)
        return TesViewHolder(view)
    }

    override fun onBindViewHolder(holder: TesViewHolder, position: Int) {
        val tes = currentList[position]

        holder.tvKategoriTes.text = tes.kategoriTes
        holder.tvDeskripsiTes.text = tes.deskripsiTes
        holder.tvJumlahSoal.text = "Jumlah soal: ${tes.jumlahSoal}"

        // Set status
        holder.tvStatusTes.text = tes.statusText
        if (tes.status == "aktif") {
            holder.tvStatusTes.setBackgroundResource(R.drawable.bg_status_aktif)
            holder.tvStatusTes.setTextColor(Color.parseColor("#155724"))
        } else {
            holder.tvStatusTes.setBackgroundResource(R.drawable.bg_status_nonaktif)
            holder.tvStatusTes.setTextColor(Color.parseColor("#721c24"))
        }

        // Setup toggle status button
        setupToggleButton(holder, tes)

        // Setup edit button
        holder.btnEditTes.setOnClickListener {
            Toast.makeText(holder.itemView.context,
                "Edit tes: ${tes.kategoriTes}",
                Toast.LENGTH_SHORT).show()
        }

        // Setup hapus button
        holder.btnHapusTes.setOnClickListener {
            Toast.makeText(holder.itemView.context,
                "Hapus tes: ${tes.kategoriTes}",
                Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = currentList.size

    private fun setupToggleButton(holder: TesViewHolder, tes: TesDetail) {
        if (tes.status == "aktif") {
            holder.btnToggleStatus.text = "Nonaktifkan"
            holder.btnToggleStatus.setIconResource(R.drawable.ic_pause)
            holder.btnToggleStatus.setBackgroundColor(Color.parseColor("#FFC107"))
            holder.btnToggleStatus.setTextColor(Color.parseColor("#000000"))
        } else {
            holder.btnToggleStatus.text = "Aktifkan"
            holder.btnToggleStatus.setIconResource(R.drawable.ic_play)
            holder.btnToggleStatus.setBackgroundColor(Color.parseColor("#4CAF50"))
            holder.btnToggleStatus.setTextColor(Color.parseColor("#FFFFFF"))
        }

        holder.btnToggleStatus.setOnClickListener {
            val newAction = if (tes.status == "aktif") "nonaktif" else "aktif"
            updateTesStatus(holder, tes.idTes, newAction, holder.adapterPosition)
        }
    }

    private fun updateTesStatus(holder: TesViewHolder, idTes: Int, action: String, position: Int) {
        val context = holder.itemView.context

        holder.btnToggleStatus.isEnabled = false
        val originalText = holder.btnToggleStatus.text.toString()
        holder.btnToggleStatus.text = if (action == "aktif") "Mengaktifkan..." else "Menonaktifkan..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = com.example.login.models.UpdateStatusRequest(
                    idTes = idTes,
                    action = action
                )

                val response = ApiClient.apiService.updateStatusTes(request)

                withContext(Dispatchers.Main) {
                    holder.btnToggleStatus.isEnabled = true

                    if (response.isSuccessful) {
                        val result = response.body()
                        if (result != null && result.success) {
                            // Update data lokal
                            val updatedTes = currentList[position].copy(
                                status = action,
                                statusText = if (action == "aktif") "Aktif" else "Nonaktif"
                            )
                            currentList[position] = updatedTes

                            // Update UI
                            notifyItemChanged(position)

                            Toast.makeText(context,
                                "✅ ${result.message}",
                                Toast.LENGTH_SHORT).show()
                        } else {
                            val errorMsg = result?.message ?: "Gagal mengupdate status"
                            Toast.makeText(context, "❌ $errorMsg", Toast.LENGTH_SHORT).show()
                            holder.btnToggleStatus.text = originalText
                        }
                    } else {
                        val errorCode = response.code()
                        val errorMessage = when (errorCode) {
                            400 -> "Data tidak valid"
                            404 -> "Endpoint tidak ditemukan"
                            500 -> "Server error"
                            else -> "Error $errorCode"
                        }
                        Toast.makeText(context, "❌ $errorMessage", Toast.LENGTH_SHORT).show()
                        holder.btnToggleStatus.text = originalText
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    holder.btnToggleStatus.isEnabled = true
                    holder.btnToggleStatus.text = originalText
                    Toast.makeText(context,
                        "❌ Error: ${e.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}