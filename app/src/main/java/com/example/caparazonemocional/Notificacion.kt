package com.example.caparazonemocional

data class Notificacion(
    val id: String,
    val tipo: TipoNotificacion,
    val mensaje: String,
    val hora: String,
    val citaId: Int? = null,
    val leida: Boolean = false
)

enum class TipoNotificacion {
    RECORDATORIO,
    CANCELACION,
    CONFIRMACION
}