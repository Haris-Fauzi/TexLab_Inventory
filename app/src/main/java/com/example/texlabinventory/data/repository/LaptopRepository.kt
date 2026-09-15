package com.example.texlabinventory.data.repository

import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.texlabinventory.data.model.Laptop
import com.example.texlabinventory.data.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

class LaptopRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // 1. Mengambil seluruh daftar laptop (Menggunakan Flow & SnapshotListener untuk Realtime/Offline Support)
    fun getLaptopsRealtime(): Flow<Resource<List<Laptop>>> = callbackFlow {
        trySend(Resource.Loading)

        val listener = firestore.collection("items")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Gagal mengambil data dari Firestore"))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val laptopList = snapshot.toObjects(Laptop::class.java)
                    trySend(Resource.Success(laptopList))
                }
            }

        awaitClose { listener.remove() }
    }

    // 1b. Alternative Direct Get (Direct Query dengan penanganan Exception lengkap)
    suspend fun getLaptops(): Resource<List<Laptop>> {
        return try {
            val snapshot = firestore.collection("items").get().await()
            val laptopList = snapshot.toObjects(Laptop::class.java)
            Resource.Success(laptopList)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Terjadi kesalahan koneksi saat mengambil data")
        }
    }

    // 2. Upload foto ke Cloudinary
    suspend fun uploadImageToCloudinary(imageUri: Uri): Resource<String> {
        return suspendCancellableCoroutine { continuation ->
            try {
                MediaManager.get().upload(imageUri)
                    .unsigned("TexLab_Inventory")
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {}
                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            val imageUrl = resultData["secure_url"] as? String ?: ""
                            if (continuation.isActive) {
                                continuation.resume(Resource.Success(imageUrl))
                            }
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            if (continuation.isActive) {
                                continuation.resume(Resource.Error(error.description ?: "Gagal mengunggah gambar"))
                            }
                        }

                        override fun onReschedule(requestId: String, error: ErrorInfo) {}
                    }).dispatch()
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(Resource.Error(e.localizedMessage ?: "Terjadi kesalahan saat menginisialisasi unggahan"))
                }
            }
        }
    }

    // 3. Menambah data laptop
    suspend fun addLaptop(laptop: Laptop): Resource<Boolean> {
        return try {
            firestore.collection("items")
                .document(laptop.inventory_id)
                .set(laptop)
                .await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Gagal menyimpan data laptop")
        }
    }

    // 4. Menghapus data laptop
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

    // 5. Memperbarui data laptop
    suspend fun updateLaptop(laptop: Laptop): Resource<Boolean> {
        return try {
            firestore.collection("items")
                .document(laptop.inventory_id)
                .set(laptop)
                .await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Gagal memperbarui data laptop")
        }
    }

    // 6. Mengambil laptop berdasarkan ID
    suspend fun getLaptopById(inventoryId: String): Resource<Laptop?> {
        return try {
            val snapshot = firestore.collection("items")
                .document(inventoryId)
                .get()
                .await()

            if (snapshot.exists()) {
                val laptop = snapshot.toObject(Laptop::class.java)
                Resource.Success(laptop)
            } else {
                Resource.Success(null)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Gagal mengambil data laptop")
        }
    }
}