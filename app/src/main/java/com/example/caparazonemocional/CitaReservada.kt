package com.example.caparazonemocional

import kotlinx.serialization.Serializable

@Serializable
data class CitaReservada(
    val id: Int? = null,
    val id_horario: Int,
    val id_paciente: Int,
    val fecha: String,
    val modalidad: String,
    val motivo: String,
    val estado: String
) {
    fun isConfirmada(): Boolean = estado == "confirmada"
}