package com.example.texlabinventory.ui.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.texlabinventory.data.model.Ruang
import com.example.texlabinventory.data.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore

class RuangViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _ruangList = MutableLiveData<Resource<List<Ruang>>>()
    val ruangState: LiveData<Resource<List<Ruang>>> get() = _ruangList

    fun fetchRuang() {
        _ruangList.value = Resource.Loading

        db.collection("ruang")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val listRuang = querySnapshot.documents.mapNotNull { doc ->
                    val nama = doc.getString("nama_ruang") ?: ""
                    Ruang(id = doc.id, nama_ruang = nama)
                }
                _ruangList.value = Resource.Success(listRuang)
            }
            .addOnFailureListener { exception ->
                _ruangList.value = Resource.Error(
                    exception.localizedMessage ?: "Gagal memuat data ruangan"
                )
            }
    }
}