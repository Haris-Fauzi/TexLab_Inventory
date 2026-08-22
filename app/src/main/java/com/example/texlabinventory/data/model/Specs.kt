package com.example.texlabinventory.data.model

import java.io.Serializable

data class Specs(
    val processor: String = "",
    val ram: String = "",
    val storage: String = ""
) : Serializable
