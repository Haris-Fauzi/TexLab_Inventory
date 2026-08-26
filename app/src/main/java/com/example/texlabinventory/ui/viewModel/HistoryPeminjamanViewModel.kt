package com.example.texlabinventory.ui.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.texlabinventory.data.model.Peminjaman
import com.example.texlabinventory.data.utils.Resource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistoryPeminjamanViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _historyState = MutableLiveData<Resource<List<Peminjaman>>>()
    val historyState: LiveData<Resource<List<Peminjaman>>> = _historyState

    private val _actionState = MutableLiveData<Resource<String>>()
    val actionState: LiveData<Resource<String>> = _actionState

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

    // Fungsi untuk memproses pengembalian barang
    fun kembalikanBarang(peminjaman: Peminjaman) {
        _actionState.value = Resource.Loading

        // 1. Cari item di katalog "laptops" atau "items" berdasarkan itemId
        db.collection("items")
            .whereEqualTo("inventory_id", peminjaman.itemId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()

                // Update status transaksi peminjaman di History
                val peminjamanRef = db.collection("peminjaman").document(peminjaman.id)
                batch.update(
                    peminjamanRef, mapOf(
                        "status" to "DIKEMBALIKAN",
                        "waktuKembali" to Timestamp.now()
                    )
                )

                // Update status barang di Katalog menjadi TERSEDIA
                if (!querySnapshot.isEmpty) {
                    val itemDocRef = querySnapshot.documents[0].reference
                    batch.update(itemDocRef, "status", "TERSEDIA")
                }

                // Jalankan Batch
                batch.commit()
                    .addOnSuccessListener {
                        _actionState.value = Resource.Success("Barang berhasil dikembalikan")
                    }
                    .addOnFailureListener { e ->
                        _actionState.value = Resource.Error(e.localizedMessage ?: "Gagal memproses pengembalian")
                    }
            }
            .addOnFailureListener { e ->
                _actionState.value = Resource.Error(e.localizedMessage ?: "Gagal menemukan barang di inventaris")
            }
    }
}