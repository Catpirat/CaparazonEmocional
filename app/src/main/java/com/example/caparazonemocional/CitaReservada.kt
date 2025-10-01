package com.example.caparazonemocional

import kotlinx.serialization.Serializable

@Serializable
data class CitaReservada(
    val id_cita: Int? = null,
    val id_horario: Int,
    val id_paciente: Int,
    val fecha: String,
    val modalidad: String,
    val motivo: String,
    val estado: Boolean  // Cambiar de String a Boolean
) {
    fun isConfirmada(): Boolean = estado
}