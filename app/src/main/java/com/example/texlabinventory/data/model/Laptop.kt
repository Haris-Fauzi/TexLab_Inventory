package com.example.texlabinventory.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

import java.io.Serializable

@IgnoreExtraProperties
data class Laptop(
    var inventory_id: String = "",
    var brand: String = "",
    var model: String = "",
    var serial_number: String = "",
    var condition: String = "",
    var status: String = "",
    var location: String = "",
    var pic_lab: String = "",
    var procurement_year: Long = 0L,
    var charger_condition: String = "",
    var charger_status: String = "",
    var image_url: String = "",
    var specs: Specs = Specs()
) : Serializable
