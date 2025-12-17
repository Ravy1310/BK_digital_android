// File: app/src/main/java/com/example/login/adapter/KelolaSoalAdapter.kt
package com.example.login.adapter

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.login.R
import com.example.login.api.ApiClient
import com.example.login.models.HapusTesRequest
import com.example.login.models.TesDetail
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KelolaSoalAdapter(
    private val tesList: List<TesDetail>,
    private val onItemClick: (TesDetail) -> Unit,
    private val onEditClick: (TesDetail) -> Unit
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
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(currentList[position])
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
        setupToggleButton(holder, tes, position)

        // Setup edit button
        holder.btnEditTes.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val currentTes = currentList[currentPosition]
                onEditClick(currentTes)
            }
        }

        // Setup hapus button
        holder.btnHapusTes.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val tesToDelete = currentList[currentPosition]
                showDeleteConfirmation(holder.itemView.context, tesToDelete, currentPosition)
            }
        }
    }

    override fun getItemCount(): Int = currentList.size

    private fun setupToggleButton(holder: TesViewHolder, tes: TesDetail, position: Int) {
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
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val newAction = if (tes.status == "aktif") "nonaktif" else "aktif"
                updateTesStatus(holder, tes.idTes, newAction, currentPosition)
            }
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

    // Konfirmasi hapus
    private fun showDeleteConfirmation(context: Context, tes: TesDetail, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Konfirmasi Hapus")
            .setMessage("Apakah Anda yakin ingin menghapus tes \"${tes.kategoriTes}\"?")
            .setPositiveButton("Hapus") { dialog, _ ->
                deleteTes(context, tes.idTes, position)
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Hapus tes
    private fun deleteTes(context: Context, idTes: Int, position: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Gunakan model HapusTesRequest yang konkret
                val request = HapusTesRequest(
                    id_tes = idTes
                )

                Log.d(TAG, "Mengirim request hapus tes: $request")

                val response = ApiClient.apiService.hapusTes(request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        Log.d(TAG, "Response hapus tes: $result")

                        if (result != null && result.success) {
                            // Hapus dari list lokal
                            currentList.removeAt(position)
                            notifyItemRemoved(position)
                            notifyItemRangeChanged(position, currentList.size - position)

                            Toast.makeText(context,
                                "✅ ${result.message}",
                                Toast.LENGTH_SHORT).show()
                        } else {
                            val errorMsg = result?.message ?: "Gagal menghapus tes"
                            Toast.makeText(context, "❌ $errorMsg", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val errorCode = response.code()
                        val errorBody = response.errorBody()?.string() ?: "No error body"
                        Log.e(TAG, "Error hapus tes: $errorCode - $errorBody")

                        val errorMessage = when (errorCode) {
                            400 -> "Bad Request: Data tidak valid"
                            404 -> "Tes tidak ditemukan"
                            500 -> "Server error"
                            else -> "Error $errorCode: $errorBody"
                        }

                        Toast.makeText(context,
                            "❌ $errorMessage",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception hapus tes: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context,
                        "❌ Error: ${e.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}