package com.example.caparazonemocional

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AgendaActivity : AppCompatActivity() {

    private lateinit var calendarContainer: LinearLayout
    private val diasSemana = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
    private val horasDelDia = (8..20).map { String.format("%02d:00:00", it) } // 08:00 a 20:00
    private var horariosMap = mutableMapOf<String, MutableList<HorarioSlot>>()
    private var citasMap = mutableMapOf<Int, CitaReservada>() // id_horario -> cita

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agenda)

        initViews()
        setupClickListeners()
        cargarDatosAgenda()
    }

    private fun initViews() {
        calendarContainer = findViewById(R.id.calendarContainer)
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<ImageView>(R.id.btnHome).setOnClickListener {
            // Navegar a MainActivity
            startActivity(Intent(this, MainActivity::class.java))
        }

        findViewById<ImageView>(R.id.btnReportes).setOnClickListener {
            // Navegar a ReportesActivity
            Toast.makeText(this, "Reportes - Próximamente", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.btnNotificaciones).setOnClickListener {
            // Navegar a NotificacionesActivity
            Toast.makeText(this, "Notificaciones - Próximamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarDatosAgenda() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Cargar horarios
                val horarios = SupabaseInstance.client.from("horario")
                    .select(Columns.list("id_horario", "dia", "hora_inicio", "hora_fin", "estado", "virtual"))
                    .decodeList<HorarioSlot>()

                // Cargar citas (simulado por ahora, después implementaremos la tabla real)
                // val citas = SupabaseInstance.client.from("cita").select().decodeList<CitaReservada>()

                withContext(Dispatchers.Main) {
                    organizarHorarios(horarios)
                    construirCalendario()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("AgendaActivity", "Error cargando datos: ${e.message}")
                    Toast.makeText(this@AgendaActivity, "Error cargando agenda: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun organizarHorarios(horarios: List<HorarioSlot>) {
        horariosMap.clear()

        // Inicializar todos los días
        diasSemana.forEach { dia ->
            horariosMap[dia] = mutableListOf()
        }

        // Agrupar horarios por día
        horarios.forEach { horario ->
            horariosMap[horario.dia]?.add(horario)
        }

        // Ordenar por hora dentro de cada día
        horariosMap.values.forEach { listaHorarios ->
            listaHorarios.sortBy { it.hora_inicio }
        }
    }

    private fun construirCalendario() {
        calendarContainer.removeAllViews()

        horasDelDia.forEach { hora ->
            val filaHora = crearFilaHora(hora)
            calendarContainer.addView(filaHora)
        }
    }

    private fun crearFilaHora(horaInicio: String): LinearLayout {
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.time_slot_height) // Define en dimens.xml
            )
        }

        // Columna de hora
        val tvHora = TextView(this).apply {
            text = horaInicio.substring(0, 5) // "08:00:00" -> "08:00"
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@AgendaActivity, android.R.color.black))
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.time_column_width), // Define en dimens.xml
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            background = ContextCompat.getDrawable(this@AgendaActivity, R.drawable.time_header_background)
        }
        fila.addView(tvHora)

        // Crear slots para cada día
        diasSemana.forEach { dia ->
            val slot = crearTimeSlot(dia, horaInicio)
            fila.addView(slot)
        }

        return fila
    }

    private fun crearTimeSlot(dia: String, horaInicio: String): FrameLayout {
        val horaFin = String.format("%02d:00:00", horaInicio.substring(0, 2).toInt() + 1)

        // Buscar el horario correspondiente
        val horario = horariosMap[dia]?.find {
            it.hora_inicio == horaInicio && it.hora_fin == horaFin
        }

        val slot = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            ).apply {
                setMargins(2, 2, 2, 2)
            }
        }

        // Configurar apariencia según el estado del horario
        configurarAparienciaSlot(slot, horario)

        // Agregar indicador de ocupación si es necesario
        if (horario != null && horario.estado) {
            val indicador = crearIndicadorOcupacion(horario)
            slot.addView(indicador)
        }

        // Click listener para configuración
        slot.setOnClickListener {
            if (horario != null) {
                mostrarDialogConfiguracion(horario)
            } else {
                Toast.makeText(this, "Horario no encontrado en la base de datos", Toast.LENGTH_SHORT).show()
            }
        }

        return slot
    }

    private fun configurarAparienciaSlot(slot: FrameLayout, horario: HorarioSlot?) {
        when {
            horario == null || !horario.estado -> {
                // No disponible - blanco
                slot.background = ContextCompat.getDrawable(this, R.drawable.slot_disabled)
            }
            horario.virtual -> {
                // Virtual - azul
                slot.background = ContextCompat.getDrawable(this, R.drawable.slot_virtual)
            }
            else -> {
                // Presencial - verde
                slot.background = ContextCompat.getDrawable(this, R.drawable.slot_presencial)
            }
        }
    }

    private fun crearIndicadorOcupacion(horario: HorarioSlot): View {
        val indicador = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.indicator_size), // Define en dimens.xml
                resources.getDimensionPixelSize(R.dimen.indicator_size)
            ).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.END
                setMargins(4, 4, 4, 4)
            }
        }

        // Verificar si está ocupado (por ahora simulamos que algunos están libres)
        val estaOcupado = citasMap.containsKey(horario.id) // Cambiar lógica según tus datos

        if (estaOcupado) {
            indicador.background = ContextCompat.getDrawable(this, R.drawable.indicator_ocupado)
        } else {
            indicador.background = ContextCompat.getDrawable(this, R.drawable.indicator_libre)
        }

        return indicador
    }

    private fun mostrarDialogConfiguracion(horario: HorarioSlot) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_config_horario)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Referencias a las vistas del dialog
        val tvHorarioInfo = dialog.findViewById<TextView>(R.id.tvHorarioInfo)
        val btnToggleEstado = dialog.findViewById<Button>(R.id.btnToggleEstado)
        val btnToggleTipo = dialog.findViewById<Button>(R.id.btnToggleTipo)
        val btnCancelar = dialog.findViewById<Button>(R.id.btnCancelar)
        val btnGuardar = dialog.findViewById<Button>(R.id.btnGuardar)

        // Configurar información del horario
        tvHorarioInfo.text = "${horario.dia} ${horario.getHoraDisplay()} - ${horario.hora_fin.substring(0, 5)}"

        // Estado actual de los botones
        var nuevoEstado = horario.estado
        var nuevoTipo = horario.virtual

        actualizarBotonesDialog(btnToggleEstado, btnToggleTipo, nuevoEstado, nuevoTipo)

        // Click listeners
        btnToggleEstado.setOnClickListener {
            nuevoEstado = !nuevoEstado
            actualizarBotonesDialog(btnToggleEstado, btnToggleTipo, nuevoEstado, nuevoTipo)
        }

        btnToggleTipo.setOnClickListener {
            if (nuevoEstado) { // Solo permitir cambio si está habilitado
                nuevoTipo = !nuevoTipo
                actualizarBotonesDialog(btnToggleEstado, btnToggleTipo, nuevoEstado, nuevoTipo)
            }
        }

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        btnGuardar.setOnClickListener {
            actualizarHorario(horario, nuevoEstado, nuevoTipo) {
                dialog.dismiss()
                cargarDatosAgenda() // Recargar para mostrar cambios
            }
        }

        dialog.show()
    }

    private fun actualizarBotonesDialog(
        btnEstado: Button,
        btnTipo: Button,
        estado: Boolean,
        virtual: Boolean
    ) {
        // Actualizar botón de estado
        if (estado) {
            btnEstado.text = "Deshabilitar"
            btnEstado.background = ContextCompat.getDrawable(this, R.drawable.btn_disabled)
        } else {
            btnEstado.text = "Habilitar"
            btnEstado.background = ContextCompat.getDrawable(this, R.drawable.btn_habilitar)
        }

        // Actualizar botón de tipo
        if (estado) {
            btnTipo.isEnabled = true
            btnTipo.alpha = 1.0f
            if (virtual) {
                btnTipo.text = "Presencial"
                btnTipo.background = ContextCompat.getDrawable(this, R.drawable.btn_presencial)
            } else {
                btnTipo.text = "Virtual"
                btnTipo.background = ContextCompat.getDrawable(this, R.drawable.btn_virtual)
            }
        } else {
            btnTipo.isEnabled = false
            btnTipo.alpha = 0.5f
            btnTipo.text = "No disponible"
            btnTipo.background = ContextCompat.getDrawable(this, R.drawable.btn_disabled)
        }
    }

    private fun actualizarHorario(
        horario: HorarioSlot,
        nuevoEstado: Boolean,
        nuevoVirtual: Boolean,
        onSuccess: () -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SupabaseInstance.client.from("horario").update({
                    set("estado", nuevoEstado)
                    set("virtual", nuevoVirtual)
                }) {
                    filter {
                        eq("id", horario.id!!)
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AgendaActivity, "Horario actualizado", Toast.LENGTH_SHORT).show()
                    onSuccess()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("AgendaActivity", "Error actualizando horario: ${e.message}")
                    Toast.makeText(this@AgendaActivity, "Error actualizando: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}