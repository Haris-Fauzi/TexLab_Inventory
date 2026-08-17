package com.example.texlabinventory.ui.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.texlabinventory.data.model.Laptop
import com.example.texlabinventory.data.repository.LaptopRepository
import com.example.texlabinventory.data.utils.Resource
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
        _laptopsState.value = Resource.Loading
        viewModelScope.launch {
            val result = repository.getLaptops()
            _laptopsState.value = result
        }
    }
}