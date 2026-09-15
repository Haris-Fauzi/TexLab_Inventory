package com.example.texlabinventory.ui.viewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.texlabinventory.data.model.Laptop
import com.example.texlabinventory.data.repository.LaptopRepository
import com.example.texlabinventory.data.utils.Resource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LaptopViewModel(
    private val repository: LaptopRepository = LaptopRepository()
) : ViewModel() {

    private val _laptopsState = MutableLiveData<Resource<List<Laptop>>>()
    val laptopsState: LiveData<Resource<List<Laptop>>> = _laptopsState

    init {
        fetchLaptops()
    }

    fun fetchLaptops() {
        viewModelScope.launch {
            repository.getLaptopsRealtime().collectLatest { result ->
                _laptopsState.value = result
            }
        }
    }

    fun uploadImagesAndSave(
        uris: List<Uri>,
        existingUrls: List<String>,
        onSuccess: (List<String>) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val uploadedUrls = mutableListOf<String>()

            for (uri in uris) {
                when (val result = repository.uploadImageToCloudinary(uri)) {
                    is Resource.Success -> {
                        result.data?.let { uploadedUrls.add(it) }
                    }
                    is Resource.Error -> {
                        onError(result.message ?: "Gagal mengunggah salah satu gambar")
                        return@launch
                    }
                    else -> {}
                }
            }

            val finalUrls = existingUrls + uploadedUrls
            onSuccess(finalUrls)
        }
    }

    fun addLaptop(laptop: Laptop, onResult: (Resource<Boolean>) -> Unit) {
        viewModelScope.launch {
            onResult(Resource.Loading)
            val result = repository.addLaptop(laptop)
            onResult(result)
        }
    }

    fun deleteLaptop(inventoryId: String, onResult: (Resource<Boolean>) -> Unit) {
        viewModelScope.launch {
            onResult(Resource.Loading)
            val result = repository.deleteLaptop(inventoryId)
            onResult(result)
        }
    }

    fun updateLaptop(laptop: Laptop, onResult: (Resource<Boolean>) -> Unit) {
        viewModelScope.launch {
            onResult(Resource.Loading)
            val result = repository.updateLaptop(laptop)
            onResult(result)
        }
    }

    fun getLaptopById(inventoryId: String, onResult: (Resource<Laptop?>) -> Unit) {
        viewModelScope.launch {
            onResult(Resource.Loading)
            try {
                val result = repository.getLaptopById(inventoryId)
                onResult(result)
            } catch (e: Exception) {
                onResult(Resource.Error(e.message ?: "Terjadi kesalahan"))
            }
        }
    }
}