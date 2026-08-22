package com.example.texlabinventory.data.utils

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.collections.get
import kotlin.coroutines.resume

object CloudinaryHelper {

    private const val CLOUD_NAME = "aayg63qo" // Ganti dengan Cloud Name Anda
    private const val UPLOAD_PRESET = "TexLab_Inventory" // Ganti dengan Upload Preset Unsigned Anda

    private var isInitialized = false

    // Inisialisasi MediaManager (Panggil sekali saja di Application / MainActivity)
    fun init(context: Context) {
        if (!isInitialized) {
            val config = mapOf(
                "cloud_name" to CLOUD_NAME,
                "secure" to true
            )
            MediaManager.init(context.applicationContext, config)
            isInitialized = true
        }
    }

    // Fungsi suspend untuk mengunggah gambar dan mengembalikan URL
    suspend fun uploadImage(imageUri: Uri): String? = suspendCancellableCoroutine { continuation ->
        MediaManager.get().upload(imageUri)
            .unsigned(UPLOAD_PRESET)
            .option("folder", "laptop_inventory") // Gambar akan tersimpan otomatis di folder ini
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: Map<*, *>) {
                    // Ambil secure URL (https) dari respon Cloudinary
                    val secureUrl = resultData["secure_url"] as? String
                    if (continuation.isActive) {
                        continuation.resume(secureUrl)
                    }
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            })
            .dispatch()
    }
}