package com.example.login.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.login.models.DashboardData
import com.example.login.models.TesTerpopuler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class DashboardViewModel : ViewModel() {

    private val TAG = "DashboardViewModel"

    private val _dashboardData = MutableLiveData<DashboardData?>()
    val dashboardData: LiveData<DashboardData?> = _dashboardData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // OkHttpClient instance
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun fetchDashboardData() {
        Log.d(TAG, "fetchDashboardData() called")

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting API call on background thread...")

                // Pindah ke background thread untuk network call
                val apiData = withContext(Dispatchers.IO) {
                    fetchDataFromAPI()
                }

                if (apiData != null) {
                    Log.d(TAG, "✓ API data received successfully")
                    Log.d(TAG, "  - Siswa: ${apiData.jumlahSiswa}")
                    Log.d(TAG, "  - Guru: ${apiData.jumlahGuru}")
                    Log.d(TAG, "  - Tes: ${apiData.jumlahTes}")
                    Log.d(TAG, "  - Tes Terpopuler: ${apiData.tesTerpopuler.size} items")

                    // Debug detail tes terpopuler
                    apiData.tesTerpopuler.forEachIndexed { index, tes ->
                        Log.d(TAG, "    ${index + 1}. ${tes.namaTes} - ${tes.jumlahSiswa} siswa")
                    }

                    _dashboardData.value = apiData
                } else {
                    Log.w(TAG, "⚠ API returned null, using dummy data")
                    // Fallback ke data dummy dengan tes terpopuler lebih banyak
                    val fallbackData = createDummyData()
                    Log.d(TAG, "Using dummy data with ${fallbackData.tesTerpopuler.size} tes items")
                    _dashboardData.value = fallbackData
                }
                _isLoading.value = false

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in fetchDashboardData: ${e.message}", e)
                _errorMessage.value = "Error: ${e.message}"
                _isLoading.value = false

                // Fallback ke data dummy jika error
                val fallbackData = createDummyData()
                Log.d(TAG, "Using fallback dummy data due to error")
                _dashboardData.value = fallbackData
            }
        }
    }

    /**
     * Fungsi untuk membuat data dummy dengan tes terpopuler
     */
    private fun createDummyData(): DashboardData {
        return DashboardData(
            jumlahSiswa = 150,
            jumlahGuru = 25,
            jumlahTes = 10,
            tesTerpopuler = listOf(
                TesTerpopuler("Tes Minat Belajar", 120),
                TesTerpopuler("Tes Personalitas", 95),
                TesTerpopuler("Tes Penjurusan", 80),
                TesTerpopuler("Tes Kematangan Emosi", 65),
                TesTerpopuler("Tes Bakat & Minat", 50)
            )
        )
    }

    private suspend fun fetchDataFromAPI(): DashboardData? {
        return try {
            val url = "https://bksmaliska.com/api/api_dashboard.php"
            Log.d(TAG, "🌐 Fetching from URL: $url")

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()

            Log.d(TAG, "📊 Response code: ${response.code}")
            Log.d(TAG, "📊 Response message: ${response.message}")

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d(TAG, "📦 API Response: $responseBody")

                if (!responseBody.isNullOrEmpty()) {
                    val parsedData = parseDashboardResponse(responseBody)
                    Log.d(TAG, "✅ Successfully parsed data")
                    parsedData
                } else {
                    Log.w(TAG, "⚠ Response body is empty or null")
                    null
                }
            } else {
                Log.w(TAG, "❌ Request failed with code: ${response.code}")
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in fetchDataFromAPI: ${e.message}", e)
            null
        }
    }

    private fun parseDashboardResponse(jsonString: String): DashboardData {
        return try {
            Log.d(TAG, "🔍 Parsing JSON response")

            val jsonObject = JSONObject(jsonString)
            Log.d(TAG, "📄 Full JSON keys: ${jsonObject.keys().asSequence().toList()}")

            // Coba parsing format yang berbeda
            var jumlahSiswa = 0
            var jumlahGuru = 0
            var jumlahTes = 0

            // Format 1: Langsung di root
            if (jsonObject.has("jumlah_siswa")) {
                jumlahSiswa = jsonObject.optInt("jumlah_siswa", 0)
                jumlahGuru = jsonObject.optInt("jumlah_guru", 0)
                jumlahTes = jsonObject.optInt("jumlah_tes", 0)
                Log.d(TAG, "📊 Format 1 - Direct in root")
            }
            // Format 2: Dalam object "data"
            else if (jsonObject.has("data")) {
                val dataObject = jsonObject.optJSONObject("data")
                if (dataObject != null) {
                    jumlahSiswa = dataObject.optInt("jumlah_siswa", 0)
                    jumlahGuru = dataObject.optInt("jumlah_guru", 0)
                    jumlahTes = dataObject.optInt("jumlah_tes", 0)
                    Log.d(TAG, "📊 Format 2 - Inside 'data' object")
                    Log.d(TAG, "📊 Data object keys: ${dataObject.keys().asSequence().toList()}")
                }
            }
            // Format 3: Coba key lain yang mungkin
            else {
                // Coba berbagai kemungkinan key
                jumlahSiswa = jsonObject.optInt("siswa", 0)
                    .takeIf { it > 0 }
                    ?: jsonObject.optInt("total_siswa", 0)
                            ?: 0

                jumlahGuru = jsonObject.optInt("guru", 0)
                    .takeIf { it > 0 }
                    ?: jsonObject.optInt("total_guru", 0)
                            ?: 0

                jumlahTes = jsonObject.optInt("tes", 0)
                    .takeIf { it > 0 }
                    ?: jsonObject.optInt("total_tes", 0)
                            ?: 0

                Log.d(TAG, "📊 Format 3 - Alternative keys")
            }

            Log.d(TAG, "✅ Parsed counts - Siswa: $jumlahSiswa, Guru: $jumlahGuru, Tes: $jumlahTes")

            // Parse tes terpopuler
            val tesTerpopulerList = mutableListOf<TesTerpopuler>()

            // Coba berbagai format untuk tes terpopuler
            var tesArray = jsonObject.optJSONArray("tes_terpopuler")

            if (tesArray == null) {
                tesArray = jsonObject.optJSONObject("data")?.optJSONArray("tes_terpopuler")
            }

            if (tesArray == null) {
                tesArray = jsonObject.optJSONArray("tesTerpopuler")
            }

            if (tesArray == null) {
                tesArray = jsonObject.optJSONObject("data")?.optJSONArray("tesTerpopuler")
            }

            if (tesArray == null) {
                tesArray = jsonObject.optJSONArray("popular_tests")
            }

            if (tesArray == null) {
                tesArray = jsonObject.optJSONArray("tests")
            }

            if (tesArray == null) {
                tesArray = jsonObject.optJSONObject("data")?.optJSONArray("tests")
            }

            if (tesArray != null) {
                Log.d(TAG, "✅ Found tes array with ${tesArray.length()} items")

                for (i in 0 until tesArray.length()) {
                    try {
                        val item = tesArray.get(i)

                        // Handle jika item adalah JSONObject
                        if (item is JSONObject) {
                            val tesObject = item

                            // Coba berbagai format field name untuk nama tes
                            val namaTes = tesObject.optString("nama_tes", "")
                                .takeIf { it.isNotEmpty() }
                                ?: tesObject.optString("namaTes", "")
                                ?: tesObject.optString("test_name", "")
                                ?: tesObject.optString("name", "")
                                ?: tesObject.optString("nama", "")
                                ?: "Tes ${i + 1}"

                            // Coba berbagai format field name untuk jumlah siswa
                            val jumlahSiswaTes = tesObject.optInt("jumlah_siswa", 0)
                                .takeIf { it > 0 }
                                ?: tesObject.optInt("jumlahSiswa", 0)
                                ?: tesObject.optInt("student_count", 0)
                                ?: tesObject.optInt("count", 0)
                                ?: tesObject.optInt("jumlah", 0)
                                ?: (100 - i * 15) // Default value untuk testing

                            if (namaTes.isNotEmpty()) {
                                tesTerpopulerList.add(TesTerpopuler(namaTes, jumlahSiswaTes))
                                Log.d(TAG, "  ✅ Added tes: $namaTes ($jumlahSiswaTes siswa)")
                            }
                        }
                        // Handle jika item adalah String (hanya nama tes)
                        else if (item is String) {
                            tesTerpopulerList.add(TesTerpopuler(item, 50 + i * 10))
                            Log.d(TAG, "  ✅ Added tes (string): $item")
                        }

                    } catch (e: Exception) {
                        Log.w(TAG, "⚠ Error parsing tes item $i: ${e.message}")
                    }
                }
            } else {
                Log.w(TAG, "⚠ No tes array found in response, checking for other formats")

                // Coba cari tes dalam format lain
                val tesObject = jsonObject.optJSONObject("tes_terpopuler")
                    ?: jsonObject.optJSONObject("data")?.optJSONObject("tes_terpopuler")

                if (tesObject != null) {
                    Log.d(TAG, "✅ Found tes as object instead of array")
                    // Jika tes adalah object, coba ekstrak data
                    val keys = tesObject.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val value = tesObject.optInt(key, 0)
                        if (value > 0) {
                            tesTerpopulerList.add(TesTerpopuler(key, value))
                            Log.d(TAG, "  ✅ Added tes from object: $key ($value siswa)")
                        }
                    }
                }
            }

            // Jika tidak ada tes yang ditemukan, tambahkan data dummy
            if (tesTerpopulerList.isEmpty()) {
                Log.w(TAG, "⚠ No tes data found in API response, using dummy data")
                tesTerpopulerList.addAll(createDummyData().tesTerpopuler)
            }

            Log.d(TAG, "✅ Final tes count: ${tesTerpopulerList.size}")

            DashboardData(
                jumlahSiswa = jumlahSiswa,
                jumlahGuru = jumlahGuru,
                jumlahTes = jumlahTes,
                tesTerpopuler = tesTerpopulerList
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing JSON: ${e.message}", e)

            // Return data dummy jika parsing gagal
            val dummyData = createDummyData()
            Log.d(TAG, "⚠ Using dummy data due to parsing error")
            dummyData
        }
    }
}