package com.example.texlabinventory.data.repository

import com.example.texlabinventory.data.utils.Resource
import com.example.texlabinventory.data.model.Laptop
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class LaptopRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    // Fungsi untuk mengambil seluruh daftar laptop dari koleksi "laptops" (atau nama collection Anda)
    suspend fun getLaptops(): Resource<List<Laptop>> {
        return try {
            // Ubah "laptops" sesuai dengan nama Collection Anda di Firestore
            val snapshot = firestore.collection("items").get().await()
            val laptopList = snapshot.toObjects(Laptop::class.java)
            Resource.Success(laptopList)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Terjadi kesalahan saat mengambil data")
        }
    }
}