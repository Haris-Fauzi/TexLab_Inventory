// GuruViewModel.kt
package com.example.texlabinventory.ui.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.texlabinventory.data.model.Guru
import com.example.texlabinventory.data.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore

class GuruViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _guruState = MutableLiveData<Resource<List<Guru>>>()
    val guruState: LiveData<Resource<List<Guru>>> = _guruState

    fun fetchGuru() {
        _guruState.value = Resource.Loading

        db.collection("guru")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val list = mutableListOf<Guru>()
                for (doc in querySnapshot.documents) {
                    val guru = doc.toObject(Guru::class.java)?.copy(id = doc.id)
                    if (guru != null) {
                        list.add(guru)
                    }
                }
                _guruState.value = Resource.Success(list)
            }
            .addOnFailureListener { exception ->
                _guruState.value = Resource.Error(exception.localizedMessage ?: "Gagal memuat data guru")
            }
    }
}