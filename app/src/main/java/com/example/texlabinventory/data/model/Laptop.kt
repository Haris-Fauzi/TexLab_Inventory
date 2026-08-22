package com.example.texlabinventory.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.io.Serializable

@IgnoreExtraProperties
data class Laptop(
    var inventory_id: String = "",
    var brand: String = "",
    var model: String = "",
    var serial_number: String = "",
    var condition: String = "",
    var status: String = "TERSEDIA",
    var location: String = "",
    var pic_lab: String = "",
    var procurement_year: Long = 0L,
    var charger_status: String = "",
    var charger_condition: String = "",

    // Field asli di Firestore yang menampung data String maupun List
    @get:PropertyName("image_url")
    @set:PropertyName("image_url")
    var rawImageUrl: Any? = null,

    var specs: Specs = Specs()
) : Serializable {

    // @get:Exclude mencegah konflik getter dengan rawImageUrl saat dibaca Firestore
    @get:Exclude
    val image_url: List<String>
        get() {
            return when (val raw = rawImageUrl) {
                is List<*> -> raw.filterIsInstance<String>()
                is String -> if (raw.isNotEmpty()) listOf(raw) else emptyList()
                else -> emptyList()
            }
        }
}