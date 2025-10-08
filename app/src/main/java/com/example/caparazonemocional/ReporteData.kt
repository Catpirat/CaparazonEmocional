package com.example.caparazonemocional

data class ReporteData(
    val diaSeleccionado: String = "Miércoles",
    val fechaDia: String = "27 Noviembre",
    val porcentajeOcupacion: Int = 0,
    val citasCanceladas: Int = 0,
    val citasAgendadas: Int = 0,
    val primeraCita: String = "--:--",
    val ultimaCita: String = "--:--",
    val ocupacionPorDia: Map<String, Int> = mapOf()
)