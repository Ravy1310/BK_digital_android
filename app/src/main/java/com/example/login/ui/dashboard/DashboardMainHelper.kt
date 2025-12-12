package com.example.login.ui.dashboard

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import com.example.login.R
import com.example.login.viewmodel.DashboardViewModel

object DashboardMainHelper {

    fun setupDashboardContent(activity: DashboardActivity, dashboardView: View) {
        // Initialize ViewModel
        val viewModel = ViewModelProvider(activity).get(DashboardViewModel::class.java)

        // Find views - SESUAIKAN DENGAN ID DI LAYOUT ANDA
        val tvJumlahSiswa = dashboardView.findViewById<TextView>(R.id.tv_jumlah_siswa)
        val tvJumlahGuru = dashboardView.findViewById<TextView>(R.id.tv_jumlah_guru)
        val tvJumlahTes = dashboardView.findViewById<TextView>(R.id.tv_jumlah_tes)
        val tvLihatSemua = dashboardView.findViewById<TextView>(R.id.tv_lihat_semua)
        val containerTesTerpopuler = dashboardView.findViewById<ViewGroup>(R.id.container_tes_terpopuler)
        val swipeRefresh = dashboardView.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh) // Perhatikan: swipeRefresh bukan swipe_refresh
        val progressBar = dashboardView.findViewById<View>(R.id.progressBar) // Perhatikan: progressBar bukan progress_bar
        val errorLayout = dashboardView.findViewById<View>(R.id.errorLayout) // Perhatikan: errorLayout bukan error_layout
        val tvErrorMessage = dashboardView.findViewById<TextView>(R.id.tv_error_message)
        val btnRetry = dashboardView.findViewById<Button>(R.id.btn_retry) // Ini Button, bukan TextView

        // Setup listeners
        tvLihatSemua.setOnClickListener {
            android.widget.Toast.makeText(activity, "Membuka semua tes", android.widget.Toast.LENGTH_SHORT).show()
        }

        swipeRefresh.setOnRefreshListener {
            viewModel.fetchDashboardData()
        }

        btnRetry.setOnClickListener {
            viewModel.fetchDashboardData()
        }

        // Setup observers
        viewModel.dashboardData.observe(activity) { data ->
            swipeRefresh.isRefreshing = false
            progressBar.visibility = View.GONE
            errorLayout.visibility = View.GONE

            if (data != null) {
                // Update UI
                tvJumlahSiswa.text = data.jumlahSiswa.toString()
                tvJumlahGuru.text = data.jumlahGuru.toString()
                tvJumlahTes.text = data.jumlahTes.toString()

                // Update tes terpopuler
                updateTesTerpopuler(activity, containerTesTerpopuler, data.tesTerpopuler)
            }
        }

        viewModel.isLoading.observe(activity) { isLoading ->
            if (isLoading && !swipeRefresh.isRefreshing) {
                progressBar.visibility = View.VISIBLE
                errorLayout.visibility = View.GONE
            }
        }

        viewModel.errorMessage.observe(activity) { errorMessage ->
            swipeRefresh.isRefreshing = false
            if (errorMessage != null) {
                progressBar.visibility = View.GONE
                errorLayout.visibility = View.VISIBLE
                tvErrorMessage.text = errorMessage
            }
        }

        // Load initial data
        viewModel.fetchDashboardData()
    }

    private fun updateTesTerpopuler(activity: DashboardActivity, container: ViewGroup, tesList: List<com.example.login.models.TesTerpopuler>) {
        container.removeAllViews()

        if (tesList.isEmpty()) {
            val textView = TextView(activity).apply {
                text = "Belum ada tes yang dikerjakan"
                setTextColor(activity.resources.getColor(android.R.color.darker_gray, null))
                textSize = 14f
                setPadding(0, dpToPx(activity, 32), 0, 0)
            }
            container.addView(textView)
            return
        }

        tesList.forEach { tes ->
            val cardView = activity.layoutInflater.inflate(
                R.layout.item_tes_terpopuler,
                container,
                false
            ) as CardView

            val tvNamaTes = cardView.findViewById<TextView>(R.id.tv_nama_tes)
            val tvJumlahSiswa = cardView.findViewById<TextView>(R.id.tv_jumlah_siswa)

            tvNamaTes.text = tes.namaTes
            tvJumlahSiswa.text = "Dikerjakan oleh ${tes.jumlahSiswa} siswa"

            container.addView(cardView)

            // Add margin bottom
            val layoutParams = cardView.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.bottomMargin = dpToPx(activity, 10)
            cardView.layoutParams = layoutParams
        }
    }

    private fun dpToPx(activity: DashboardActivity, dp: Int): Int {
        return (dp * activity.resources.displayMetrics.density).toInt()
    }
}