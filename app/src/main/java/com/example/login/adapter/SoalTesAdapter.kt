// File: app/src/main/java/com/example/login/adapter/SoalTesAdapter.kt
package com.example.login.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.login.R
import com.example.login.models.OpsiData
import com.example.login.models.SoalData

class SoalTesAdapter(
    private var soalList: List<SoalData>,
    private val onEditClick: (SoalData) -> Unit,
    private val onDeleteClick: (SoalData) -> Unit
) : RecyclerView.Adapter<SoalTesAdapter.SoalViewHolder>() {

    inner class SoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNomorSoal: TextView = itemView.findViewById(R.id.tvNomorSoal)
        val tvPertanyaan: TextView = itemView.findViewById(R.id.tvPertanyaan)
        val tvOpsiA: TextView = itemView.findViewById(R.id.tvOpsiA)
        val tvOpsiB: TextView = itemView.findViewById(R.id.tvOpsiB)
        val tvOpsiC: TextView = itemView.findViewById(R.id.tvOpsiC)
        val tvOpsiD: TextView = itemView.findViewById(R.id.tvOpsiD)
        val tvOpsiE: TextView = itemView.findViewById(R.id.tvOpsiE)
        val btnEditSoal: TextView = itemView.findViewById(R.id.btnEditSoal)
        val btnHapusSoal: TextView = itemView.findViewById(R.id.btnHapusSoal)
        val garisPemisah: View = itemView.findViewById(R.id.garisPemisah)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SoalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_soal_tes, parent, false)
        return SoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: SoalViewHolder, position: Int) {
        val soal = soalList[position]
        val opsiList = soal.opsi_list

        // Set nomor soal
        holder.tvNomorSoal.text = "${position + 1}."

        // Set pertanyaan
        holder.tvPertanyaan.text = if (soal.pertanyaan.isNullOrEmpty()) {
            "[Pertanyaan tidak tersedia]"
        } else {
            soal.pertanyaan
        }

        // Set opsi jawaban (mendukung hingga 5 opsi)
        setOpsiText(holder.tvOpsiA, "A", opsiList, 0)
        setOpsiText(holder.tvOpsiB, "B", opsiList, 1)
        setOpsiText(holder.tvOpsiC, "C", opsiList, 2)
        setOpsiText(holder.tvOpsiD, "D", opsiList, 3)
        setOpsiText(holder.tvOpsiE, "E", opsiList, 4)

        // Sembunyikan garis pemisah untuk item terakhir
        holder.garisPemisah.visibility = if (position == soalList.size - 1) View.GONE else View.VISIBLE

        // Setup tombol edit
        holder.btnEditSoal.setOnClickListener {
            onEditClick(soal)
        }

        // Setup tombol hapus
        holder.btnHapusSoal.setOnClickListener {
            onDeleteClick(soal)
        }
    }

    override fun getItemCount(): Int = soalList.size

    fun updateData(newSoalList: List<SoalData>) {
        soalList = newSoalList
        notifyDataSetChanged()
    }

    /**
     * Helper function untuk menampilkan teks opsi
     */
    private fun setOpsiText(textView: TextView, label: String, opsiList: List<OpsiData>, index: Int) {
        if (opsiList.size > index) {
            val opsi = opsiList[index]
            val bobot = opsi.bobot
            val teks = getOpsiText(opsi)
            textView.text = "$label. (Bobot $bobot) $teks"
            textView.visibility = View.VISIBLE
        } else {
            textView.text = "$label. (Bobot 0) [Opsi tidak tersedia]"
            textView.visibility = View.VISIBLE
        }
    }

    /**
     * Helper function untuk mendapatkan teks opsi dari berbagai field
     */
    private fun getOpsiText(opsi: OpsiData): String {
        // Cek semua kemungkinan field secara berurutan
        val possibleFields = listOf(
            opsi.opsi_text,
            opsi.opsi,
            opsi.jawaban,
            opsi.teks_opsi
        )

        // Ambil field pertama yang tidak null dan tidak kosong
        for (field in possibleFields) {
            if (!field.isNullOrEmpty()) {
                return field
            }
        }

        // Fallback jika semua kosong
        return "Opsi ${opsi.id_opsi}"
    }
}