package com.example.login.ui.dashboard

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.login.R
import com.example.login.databinding.ActivityDashboardBinding
import com.example.login.viewmodel.DashboardViewModel

class DashboardFragment : Fragment(R.layout.activity_dashboard) {

    private var _binding: ActivityDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DashboardViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = ActivityDashboardBinding.bind(view)

        // Inisialisasi ViewModel dengan ViewModelProvider
        viewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)

        setupUI()
        setupObservers()

        // Load initial data
        viewModel.fetchDashboardData()
    }

    private fun setupUI() {
        // Swipe to refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.fetchDashboardData()
        }

        // Lihat Semua button
        binding.tvLihatSemua.setOnClickListener {
            // Handle click
        }

        // Retry button
        binding.btnRetry.setOnClickListener {
            viewModel.fetchDashboardData()
        }
    }

    private fun setupObservers() {
        viewModel.dashboardData.observe(viewLifecycleOwner) { data ->
            binding.swipeRefresh.isRefreshing = false
            binding.progressBar.visibility = View.GONE
            binding.errorLayout.visibility = View.GONE

            if (data != null) {
                showData(data)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading && !binding.swipeRefresh.isRefreshing) {
                binding.progressBar.visibility = View.VISIBLE
                binding.errorLayout.visibility = View.GONE
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            binding.swipeRefresh.isRefreshing = false
            if (errorMessage != null) {
                binding.progressBar.visibility = View.GONE
                binding.errorLayout.visibility = View.VISIBLE
                binding.tvErrorMessage.text = errorMessage
            }
        }
    }

    private fun showData(data: com.example.login.models.DashboardData) {
        // Update statistics
        binding.tvJumlahSiswa.text = data.jumlahSiswa.toString()
        binding.tvJumlahGuru.text = data.jumlahGuru.toString()
        binding.tvJumlahTes.text = data.jumlahTes.toString()

        // Update popular tests
        updateTesTerpopuler(data.tesTerpopuler)
    }

    private fun updateTesTerpopuler(tesList: List<com.example.login.models.TesTerpopuler>) {
        val container = binding.containerTesTerpopuler
        container.removeAllViews()

        if (tesList.isEmpty()) {
            val textView = TextView(requireContext()).apply {
                text = "Belum ada tes yang dikerjakan"
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
                textSize = 14f
                setPadding(0, dpToPx(32), 0, 0)
            }
            container.addView(textView)
            return
        }

        tesList.forEach { tes ->
            val cardView = layoutInflater.inflate(
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
            layoutParams.bottomMargin = dpToPx(10)
            cardView.layoutParams = layoutParams
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}