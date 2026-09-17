package com.example.texlabinventory.data.model

import com.google.firebase.firestore.PropertyName

data class Guru(
    val id: String = "",
    val nama_guru: String = "",
    val kode: String = "",
    @get:PropertyName("ket.guru")
    @set:PropertyName("ket.guru")
    var ket_guru: String = ""
)