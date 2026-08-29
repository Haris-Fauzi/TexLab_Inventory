package com.example.texlabinventory.data.repository

import com.example.texlabinventory.data.utils.Resource
import com.example.texlabinventory.data.model.Laptop
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

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

    // 1. Upload foto ke Cloudinary
    fun uploadImageToCloudinary(imageUri: Uri, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        MediaManager.get().upload(imageUri)
            .unsigned("TexLab_Inventory") // Ganti dengan Unsigned Upload Preset Cloudinary Anda
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val imageUrl = resultData["secure_url"] as? String ?: ""
                    onSuccess(imageUrl)
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    onError(error.description)
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            }).dispatch()
    }

    suspend fun addLaptop(laptop: Laptop): Resource<Boolean> {
        return try {
            firestore.collection("items")
                .document(laptop.inventory_id) // Menggunakan ID Inventaris sebagai Document ID
                .set(laptop)
                .await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Gagal menyimpan data laptop")
        }
    }

    suspend fun deleteLaptop(inventoryId: String): Resource<Boolean> {
        return try {
            firestore.collection("items")
                .document(inventoryId)
                .delete()
                .await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Gagal menghapus data laptop")
        }
    }

    suspend fun updateLaptop(laptop: Laptop): Resource<Boolean> {
        return try {
            // .set(laptop) dengan ID dokumen yang sama akan menimpa/memperbarui data lama secara penuh
            firestore.collection("items")
                .document(laptop.inventory_id)
                .set(laptop)
                .await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Gagal memperbarui data laptop")
        }
    }

    //scanner
    // Tambahkan fungsi ini di dalam class LaptopRepository
    suspend fun getLaptopById(inventoryId: String): Resource<Laptop?> {
        return try {
            val snapshot = firestore.collection("items")
                .whereEqualTo("inventory_id", inventoryId)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val laptop = snapshot.documents[0].toObject(Laptop::class.java)
                Resource.Success(laptop)
            } else {
                Resource.Success(null) // Laptop tidak ditemukan
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Gagal mengambil data laptop")
        }
    }

}