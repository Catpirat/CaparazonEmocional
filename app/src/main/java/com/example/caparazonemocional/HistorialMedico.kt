package com.example.caparazonemocional

import kotlinx.serialization.Serializable

@Serializable
data class HistorialMedico(
    val id_historial: Int? = null,
    val id_paciente: Int,
    val fecha_registro: String
)