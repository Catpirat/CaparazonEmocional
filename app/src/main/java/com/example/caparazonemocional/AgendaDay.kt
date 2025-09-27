package com.example.caparazonemocional

import kotlinx.serialization.Serializable

@Serializable
data class AgendaDay(
    val nombreDia: String,
    val horarios: MutableList<HorarioSlot> = mutableListOf(),
    val citasReservadas: MutableList<CitaReservada> = mutableListOf()
) {
    fun isHorarioOcupado(horarioId: Int): Boolean {
        return citasReservadas.any { it.id_horario == horarioId && it.isConfirmada() }
    }
}
