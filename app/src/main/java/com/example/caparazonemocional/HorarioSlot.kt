package com.example.caparazonemocional

import kotlinx.serialization.Serializable

@Serializable
data class HorarioSlot(
    val id_horario: Int? = null,
    val dia: String,
    val hora_inicio: String,
    val hora_fin: String,
    val estado: Boolean,
    val virtual: Boolean
) {
    fun getHoraDisplay(): String {
        return hora_inicio.substring(0, 5)
    }
}