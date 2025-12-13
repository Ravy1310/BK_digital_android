package com.example.login.viewmodel
// File: app/src/main/java/com/example/login/viewmodel/KelolaTesViewModel.kt


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.login.api.ApiClient
import com.example.login.models.KelolaTesResponse
import kotlinx.coroutines.launch

class KelolaTesViewModel : ViewModel() {

    private val _kelolaTesData = MutableLiveData<KelolaTesResponse>()
    val kelolaTesData: LiveData<KelolaTesResponse> = _kelolaTesData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun fetchKelolaTesData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val response = ApiClient.apiService.getKelolaTesData()

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    if (result.success) {
                        _kelolaTesData.value = result
                    } else {
                        _errorMessage.value = result.message ?: "Gagal memuat data"
                    }
                } else {
                    _errorMessage.value = "Gagal menghubungi server (${response.code()})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}