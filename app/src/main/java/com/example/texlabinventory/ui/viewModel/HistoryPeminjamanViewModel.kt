package com.example.texlabinventory.ui.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.texlabinventory.data.model.Peminjaman
import com.example.texlabinventory.data.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistoryPeminjamanViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _historyState = MutableLiveData<Resource<List<Peminjaman>>>()
    val historyState: LiveData<Resource<List<Peminjaman>>> = _historyState

    fun fetchHistoryPeminjaman() {
        _historyState.value = Resource.Loading

        db.collection("peminjaman")
            .orderBy("waktuPinjam", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _historyState.value = Resource.Error(error.localizedMessage ?: "Gagal memuat history")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val listPeminjaman = snapshot.toObjects(Peminjaman::class.java)
                    _historyState.value = Resource.Success(listPeminjaman)
                }
            }
    }
}