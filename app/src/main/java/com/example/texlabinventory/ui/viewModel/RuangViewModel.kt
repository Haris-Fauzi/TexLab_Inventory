package com.example.texlabinventory.ui.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.texlabinventory.data.model.Ruang
import com.example.texlabinventory.data.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore

class RuangViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _ruangState = MutableLiveData<Resource<List<Ruang>>>()
    val ruangState: LiveData<Resource<List<Ruang>>> = _ruangState

    fun fetchRuang() {
        _ruangState.value = Resource.Loading

        db.collection("ruang")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val list = mutableListOf<Ruang>()
                for (doc in querySnapshot.documents) {
                    val ruang = doc.toObject(Ruang::class.java)?.copy(id = doc.id)
                    if (ruang != null) {
                        list.add(ruang)
                    }
                }
                _ruangState.value = Resource.Success(list)
            }
            .addOnFailureListener { exception ->
                _ruangState.value = Resource.Error(exception.localizedMessage ?: "Gagal memuat data ruang")
            }
    }
}