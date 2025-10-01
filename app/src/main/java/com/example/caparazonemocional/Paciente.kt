package com.example.caparazonemocional

import kotlinx.serialization.Serializable

@Serializable
data class Paciente(
    val id_paciente: Int? = null,
    val nombre: String,  // Cambiar "ombre" a "nombre"
    val edad: Int? = null,
    val telefono: String? = null,
    val correo: String? = null,
    val estado: Boolean = true
)