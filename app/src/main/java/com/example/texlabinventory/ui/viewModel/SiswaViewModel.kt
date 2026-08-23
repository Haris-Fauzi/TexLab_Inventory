package com.example.texlabinventory.ui.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.texlabinventory.data.model.Siswa
import com.example.texlabinventory.data.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore

class SiswaViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _siswaState = MutableLiveData<Resource<List<Siswa>>>()
    val siswaState: LiveData<Resource<List<Siswa>>> = _siswaState

    fun fetchSiswa() {
        _siswaState.value = Resource.Loading

        db.collection("siswa")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val list = mutableListOf<Siswa>()
                for (doc in querySnapshot.documents) {
                    val siswa = doc.toObject(Siswa::class.java)?.copy(nis = doc.id)
                    if (siswa != null) {
                        list.add(siswa)
                    }
                }
                _siswaState.value = Resource.Success(list)
            }
            .addOnFailureListener { exception ->
                _siswaState.value = Resource.Error(exception.localizedMessage ?: "Gagal memuat data")
            }
    }
}