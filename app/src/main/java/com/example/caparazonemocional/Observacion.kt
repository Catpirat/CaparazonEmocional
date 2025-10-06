package com.example.caparazonemocional

import kotlinx.serialization.Serializable

@Serializable
data class Observacion(
    val id_observaciones: Int? = null,
    val id_historial: Int,
    val mensaje: String,
    val fecha_hora: String
)