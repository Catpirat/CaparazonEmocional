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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDate
import org.threeten.bp.DayOfWeek
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.TextStyle
import java.util.Locale

class AgendaActivity : AppCompatActivity() {

    private lateinit var calendarContainer: LinearLayout
    private lateinit var tvRangoSemana: TextView
    private lateinit var btnSemanaAnterior: ImageButton
    private lateinit var btnSemanaSiguiente: ImageButton

    private val horasDelDia = (8..20).map { String.format("%02d:00:00", it) }
    private var horariosMap = mutableMapOf<String, MutableList<HorarioSlot>>()
    private var citasMap = mutableMapOf<Int, CitaReservada>()

    // Variables para el manejo de fechas
    private var primerDiaSemana: LocalDate = LocalDate.now().with(DayOfWeek.MONDAY)
    private val diasSemana = listOf("Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado")
    private val formateadorFecha = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agenda)

        initViews()
        setupClickListeners()
        actualizarHeaderFechas()
        cargarDatosAgenda()

        val headerScroll = findViewById<HorizontalScrollView>(R.id.headerScrollView)
        val contentScroll = findViewById<HorizontalScrollView>(R.id.contentScrollView)

        contentScroll.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            headerScroll.scrollTo(scrollX, 0)
        }
    }

    override fun onResume() {
        super.onResume()
        cargarDatosAgenda()
    }

    private fun initViews() {
        calendarContainer = findViewById(R.id.calendarContainer)
        tvRangoSemana = findViewById(R.id.tvRangoSemana)
        btnSemanaAnterior = findViewById(R.id.btnSemanaAnterior)
        btnSemanaSiguiente = findViewById(R.id.btnSemanaSiguiente)
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<ImageView>(R.id.btnHome).setOnClickListener {
            startActivity(Intent(this, AgendaActivity::class.java))
        }

        findViewById<ImageView>(R.id.btnReportes).setOnClickListener {
            startActivity(Intent(this, ReportesActivity::class.java))
        }

        findViewById<ImageView>(R.id.btnPacientes).setOnClickListener {
            startActivity(Intent(this, MisPacientesActivity::class.java))
        }

        findViewById<ImageView>(R.id.btnNotificaciones).setOnClickListener {
            startActivity(Intent(this, NotificacionesActivity::class.java))
        }

        findViewById<ImageView>(R.id.btnConfig).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Navegación entre semanas
        btnSemanaAnterior.setOnClickListener {
            primerDiaSemana = primerDiaSemana.minusWeeks(1)
            actualizarHeaderFechas()
            cargarDatosAgenda()
        }

        btnSemanaSiguiente.setOnClickListener {
            primerDiaSemana = primerDiaSemana.plusWeeks(1)
            actualizarHeaderFechas()
            cargarDatosAgenda()
        }
    }

    private fun actualizarHeaderFechas() {
        val ultimoDiaSemana = primerDiaSemana.plusDays(5) // Sábado
        val formatoDisplay = DateTimeFormatter.ofPattern("d MMM", Locale("es", "ES"))

        tvRangoSemana.text = "${primerDiaSemana.format(formatoDisplay)} - ${ultimoDiaSemana.format(formatoDisplay)}"

        // Actualizar los headers de los días con las fechas
        actualizarHeadersDias()
    }

    private fun actualizarHeadersDias() {
        val headerContainer = findViewById<LinearLayout>(R.id.headerDaysContainer)
        headerContainer.removeAllViews()

        for (i in 0..5) {
            val fecha = primerDiaSemana.plusDays(i.toLong())
            val diaNombre = diasSemana[i]
            val diaNumero = fecha.dayOfMonth

            val headerDia = layoutInflater.inflate(R.layout.item_day_header, headerContainer, false)
            headerDia.findViewById<TextView>(R.id.tvDiaNombre).text = diaNombre.substring(0, 3) // Lun, Mar, etc
            headerDia.findViewById<TextView>(R.id.tvDiaNumero).text = diaNumero.toString()

            headerContainer.addView(headerDia)
        }
    }

    private fun cargarDatosAgenda() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ultimoDiaSemana = primerDiaSemana.plusDays(5)

                // Cargar horarios
                val horarios = SupabaseInstance.client.from("horario")
                    .select()
                    .decodeList<HorarioSlot>()

                // Cargar citas de la semana actual solamente
                val citas = SupabaseInstance.client.from("cita")
                    .select() {
                        filter {
                            eq("estado", true)
                            gte("fecha", primerDiaSemana.format(formateadorFecha))
                            lte("fecha", ultimoDiaSemana.format(formateadorFecha))
                        }
                    }
                    .decodeList<CitaReservada>()

                withContext(Dispatchers.Main) {
                    organizarHorarios(horarios)
                    organizarCitas(citas)
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

    private fun organizarCitas(citas: List<CitaReservada>) {
        citasMap.clear()
        Log.d("AgendaActivity", "Total citas cargadas: ${citas.size}")
        citas.forEach { cita ->
            Log.d("AgendaActivity", "Cita ID: ${cita.id_cita}, Fecha: ${cita.fecha}, Horario ID: ${cita.id_horario}")
            citasMap[cita.id_horario] = cita
        }
    }

    private fun organizarHorarios(horarios: List<HorarioSlot>) {
        horariosMap.clear()

        diasSemana.forEach { dia ->
            horariosMap[dia] = mutableListOf()
        }

        horarios.forEach { horario ->
            horariosMap[horario.dia]?.add(horario)
        }

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
                resources.getDimensionPixelSize(R.dimen.time_slot_height)
            )
        }

        val tvHora = TextView(this).apply {
            text = horaInicio.substring(0, 5)
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@AgendaActivity, android.R.color.black))
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.time_column_width),
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            background = ContextCompat.getDrawable(this@AgendaActivity, R.drawable.time_header_background)
        }
        fila.addView(tvHora)

        // Crear slots para cada día de la semana actual
        for (i in 0..5) {
            val fecha = primerDiaSemana.plusDays(i.toLong())
            val diaNombre = diasSemana[i]
            val slot = crearTimeSlot(diaNombre, horaInicio, fecha)
            fila.addView(slot)
        }

        return fila
    }

    private fun crearTimeSlot(dia: String, horaInicio: String, fecha: LocalDate): FrameLayout {
        val horaFin = String.format("%02d:00:00", horaInicio.substring(0, 2).toInt() + 1)

        val horario = horariosMap[dia]?.find {
            it.hora_inicio == horaInicio && it.hora_fin == horaFin
        }

        val slot = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.day_column_width),
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(2, 2, 2, 2)
            }
        }

        configurarAparienciaSlot(slot, horario)

        if (horario != null && horario.estado) {
            val indicador = crearIndicadorOcupacion(horario, fecha)
            slot.addView(indicador)
        }

        slot.setOnClickListener {
            if (horario != null) {
                // Buscar cita en esta fecha específica
                val cita = citasMap.values.find {
                    it.id_horario == horario.id_horario &&
                            it.fecha == fecha.format(formateadorFecha)
                }

                if (cita != null) {
                    abrirInfoCita(cita.id_cita!!)
                } else {
                    mostrarDialogConfiguracion(horario)
                }
            } else {
                Toast.makeText(this, "Horario no encontrado", Toast.LENGTH_SHORT).show()
            }
        }

        return slot
    }

    private fun abrirInfoCita(citaId: Int) {
        val intent = Intent(this, CitaInfoActivity::class.java)
        intent.putExtra("CITA_ID", citaId)
        startActivity(intent)
    }

    private fun configurarAparienciaSlot(slot: FrameLayout, horario: HorarioSlot?) {
        when {
            horario == null || !horario.estado -> {
                slot.background = ContextCompat.getDrawable(this, R.drawable.slot_disabled)
            }
            horario.virtual -> {
                slot.background = ContextCompat.getDrawable(this, R.drawable.slot_virtual)
            }
            else -> {
                slot.background = ContextCompat.getDrawable(this, R.drawable.slot_presencial)
            }
        }
    }

    private fun crearIndicadorOcupacion(horario: HorarioSlot, fecha: LocalDate): View {
        val indicador = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.indicator_size),
                resources.getDimensionPixelSize(R.dimen.indicator_size)
            ).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.END
                setMargins(4, 4, 4, 4)
            }
        }

        // Verificar si hay cita en esta fecha específica
        val estaOcupado = citasMap.values.any {
            it.id_horario == horario.id_horario &&
                    it.fecha == fecha.format(formateadorFecha)
        }

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

        val tvHorarioInfo = dialog.findViewById<TextView>(R.id.tvHorarioInfo)
        val btnToggleEstado = dialog.findViewById<Button>(R.id.btnToggleEstado)
        val btnToggleTipo = dialog.findViewById<Button>(R.id.btnToggleTipo)
        val btnCancelar = dialog.findViewById<Button>(R.id.btnCancelar)
        val btnGuardar = dialog.findViewById<Button>(R.id.btnGuardar)

        tvHorarioInfo.text = "${horario.dia} ${horario.getHoraDisplay()} - ${horario.hora_fin.substring(0, 5)}"

        var nuevoEstado = horario.estado
        var nuevoTipo = horario.virtual

        actualizarBotonesDialog(btnToggleEstado, btnToggleTipo, nuevoEstado, nuevoTipo)

        btnToggleEstado.setOnClickListener {
            nuevoEstado = !nuevoEstado
            actualizarBotonesDialog(btnToggleEstado, btnToggleTipo, nuevoEstado, nuevoTipo)
        }

        btnToggleTipo.setOnClickListener {
            if (nuevoEstado) {
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
                cargarDatosAgenda()
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
        if (estado) {
            btnEstado.text = "Deshabilitar"
            btnEstado.background = ContextCompat.getDrawable(this, R.drawable.btn_disabled)
        } else {
            btnEstado.text = "Habilitar"
            btnEstado.background = ContextCompat.getDrawable(this, R.drawable.btn_habilitar)
        }

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
                SupabaseInstance.client.from("horario").update(
                    mapOf(
                        "estado" to nuevoEstado,
                        "virtual" to nuevoVirtual
                    )
                ) {
                    filter {
                        eq("id_horario", horario.id_horario!!)
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