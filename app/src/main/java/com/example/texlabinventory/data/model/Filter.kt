package com.example.texlabinventory.data.model

class Filter (
    val status: String? = null,          // null = Semua Status, "BORROWED", "RETURNED", "LATE", dll.
    val startDate: Long? = null,         // Timestamp awal (ms)
    val endDate: Long? = null            // Timestamp akhir (ms)
)