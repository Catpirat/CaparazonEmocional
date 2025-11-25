# Caparazón Emocional — Aplicación Móvil (Android)

**Aplicación móvil oficial del sistema Caparazón Emocional**, una plataforma integral diseñada para optimizar la gestión administrativa de consultorios psicológicos.

Este módulo corresponde exclusivamente a la app para psicólogos, desarrollada en **Android Studio (Kotlin)**, y conectada con un backend en **Supabase**.

---

## Descripción General

La aplicación móvil permite a los psicólogos gestionar de manera sencilla y ordenada todas las tareas esenciales de su consultorio, incluyendo:
- Administración de horarios disponibles.
- Visualización y control de citas (virtuales y presenciales).
- Gestión de pacientes.
- Recepción de notificaciones sobre nuevas citas, cancelaciones o cambios.
- Acceso a reportes e historial clínico.
- Recordatorios sobre cambios, cancelaciónes y citas proximas.
- Recuperación segura de contraseña mediante **deep links** personalizados integrados con Supabase.

La app funciona como uno de los tres módulos principales del sistema Caparazón Emocional, junto con:
- El chatbot automatizado (gestión de citas mediante lenguaje natural).
- La página web (plataforma para psicólogos y pacientes).

---

##  Tecnologías Utilizadas

| Componente         | Tecnología                     |
|--------------------|--------------------------------|
| Lenguaje           | Kotlin                         |
| IDE                | Android Studio                 |
| Backend            | Supabase (Auth, PostgreSQL, API REST) |
| HTTP Client        | Ktor                           |
| Navegación         | Deep Links personalizados      |
| Control de versiones| Git & GitHub                  |

---

##  Características de la aplicación

✅ **Autenticación**
- Registro e inicio de sesión mediante Supabase Auth.
- Recuperación de contraseña vía email y deep link seguro.
- Manejo de tokens JWT y refresh tokens.

⚙️​ **Agenda Interactiva**
- Activar/desactivar horas de atención (ej. 10:00 a.m., 1:00 p.m., etc.).
- Clasificación de citas como virtuales o presenciales.

✅ **Gestión de Pacientes**
- Lista de pacientes activos.
- Información relevante para consulta.

✅ **Notificaciones**
- Avisos automáticos cuando:
  - Se agenda una cita.
  - Un paciente cancela.
  - Se reprograma una sesión.

⚙️​ **Reportes**
- Visión semanal o mensual de:
  - Total de citas.
  - Modalidades.
  - Tendencias de cancelaciones.

---

## ⚙️​ Seguridad

La app utiliza:
- **Row Level Security (RLS)** en Supabase.
- Reglas estrictas para garantizar que cada psicólogo solo acceda a sus propios pacientes y horarios.
- Tokens firmados (JWT).
- Deep links seguros para recuperación de contraseña.
- HTTPS obligatorio para todas las conexiones.

---

## Estado del proyecto
En desarrollo activo
