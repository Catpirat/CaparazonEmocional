package com.example.caparazonemocional

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ReportesActivity : AppCompatActivity() {

    private lateinit var tvResumenSemanal: TextView
    private lateinit var tvPorcentajeOcupacion: TextView
    private lateinit var tvDiaSeleccionado: TextView
    private lateinit var tvFechaDia: TextView
    private lateinit var tvDesocupacion: TextView
    private lateinit var tvCitasCanceladas: TextView
    private lateinit var tvCitasAgendadas: TextView
    private lateinit var tvPrimeraCita: TextView
    private lateinit var tvUltimaCita: TextView

    private val diasSemanaViews = mutableMapOf<String, TextView>()
    private var diaSeleccionado = "Miércoles"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reportes)

        initViews()
        setupClickListeners()
        cargarReporteSemanal()
    }

    private fun initViews() {
        tvResumenSemanal = findViewById(R.id.tvResumenSemanal)
        tvPorcentajeOcupacion = findViewById(R.id.tvPorcentajeOcupacion)
        tvDiaSeleccionado = findViewById(R.id.tvDiaSeleccionado)
        tvFechaDia = findViewById(R.id.tvFechaDia)
        tvDesocupacion = findViewById(R.id.tvDesocupacion)
        tvCitasCanceladas = findViewById(R.id.tvCitasCanceladas)
        tvCitasAgendadas = findViewById(R.id.tvCitasAgendadas)
        tvPrimeraCita = findViewById(R.id.tvPrimeraCita)
        tvUltimaCita = findViewById(R.id.tvUltimaCita)

        // Mapear días de la semana con sus TextViews
        diasSemanaViews["Lunes"] = findViewById(R.id.tvLunes)
        diasSemanaViews["Martes"] = findViewById(R.id.tvMartes)
        diasSemanaViews["Miércoles"] = findViewById(R.id.tvMiercoles)
        diasSemanaViews["Jueves"] = findViewById(R.id.tvJueves)
        diasSemanaViews["Viernes"] = findViewById(R.id.tvViernes)
        diasSemanaViews["Sábado"] = findViewById(R.id.tvSabado)
        diasSemanaViews["Domingo"] = findViewById(R.id.tvDomingo)
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnEstadisticas).setOnClickListener {
            Toast.makeText(this, "Estadísticas de meses anteriores - Próximamente", Toast.LENGTH_SHORT).show()
        }

        // Click listeners para cada día
        diasSemanaViews.forEach { (dia, textView) ->
            textView.setOnClickListener {
                diaSeleccionado = dia
                cargarReporteDia(dia)
            }
        }
    }

    private fun cargarReporteSemanal() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obtener rango de fechas de la semana actual
                val calendar = Calendar.getInstance()
                val formatoFecha = SimpleDateFormat("dd MMM", Locale("es", "ES"))

                // Inicio de la semana (lunes)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val inicioSemana = formatoFecha.format(calendar.time)

                // Fin de la semana (domingo)
                calendar.add(Calendar.DAY_OF_WEEK, 6)
                val finSemana = formatoFecha.format(calendar.time)

                // Cargar todos los horarios habilitados
                val horarios = SupabaseInstance.client.from("horario")
                    .select() {
                        filter {
                            eq("estado", true)
                        }
                    }
                    .decodeList<HorarioSlot>()

                // Cargar todas las citas confirmadas de esta semana
                val citas = SupabaseInstance.client.from("cita")
                    .select() {
                        filter {
                            eq("estado", true)
                        }
                    }
                    .decodeList<CitaReservada>()

                // Calcular ocupación por día
                val ocupacionPorDia = calcularOcupacionPorDia(horarios, citas)

                withContext(Dispatchers.Main) {
                    tvResumenSemanal.text = "Resumen semanal: $inicioSemana - $finSemana"
                    mostrarOcupacionDias(ocupacionPorDia)
                    cargarReporteDia(diaSeleccionado)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("ReportesActivity", "Error: ${e.message}")
                    Toast.makeText(this@ReportesActivity, "Error cargando reporte: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun calcularOcupacionPorDia(
        horarios: List<HorarioSlot>,
        citas: List<CitaReservada>
    ): Map<String, Int> {
        val ocupacion = mutableMapOf<String, Int>()
        val dias = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

        dias.forEach { dia ->
            val horariosDelDia = horarios.filter { it.dia == dia }
            val totalHorarios = horariosDelDia.size

            if (totalHorarios > 0) {
                val idsHorarios = horariosDelDia.mapNotNull { it.id_horario }
                val citasDelDia = citas.filter { it.id_horario in idsHorarios }
                val porcentaje = (citasDelDia.size * 100) / totalHorarios
                ocupacion[dia] = porcentaje
            } else {
                ocupacion[dia] = 0
            }
        }

        return ocupacion
    }

    private fun mostrarOcupacionDias(ocupacionPorDia: Map<String, Int>) {
        diasSemanaViews.forEach { (dia, textView) ->
            val ocupacion = ocupacionPorDia[dia] ?: 0

            // Cambiar color según ocupación
            val backgroundColor = when {
                ocupacion >= 75 -> ContextCompat.getColor(this, R.color.ocupacion_alta)
                ocupacion >= 50 -> ContextCompat.getColor(this, R.color.ocupacion_media)
                ocupacion >= 25 -> ContextCompat.getColor(this, R.color.ocupacion_baja)
                else -> ContextCompat.getColor(this, R.color.ocupacion_ninguna)
            }

            textView.setBackgroundColor(backgroundColor)
        }
    }

    private fun cargarReporteDia(dia: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Cargar horarios del día
                val horarios = SupabaseInstance.client.from("horario")
                    .select() {
                        filter {
                            eq("dia", dia)
                            eq("estado", true)
                        }
                    }
                    .decodeList<HorarioSlot>()

                val idsHorarios = horarios.mapNotNull { it.id_horario }

                // Cargar citas del día (confirmadas)
                val citasConfirmadas = SupabaseInstance.client.from("cita")
                    .select() {
                        filter {
                            eq("estado", true)
                        }
                    }
                    .decodeList<CitaReservada>()
                    .filter { it.id_horario in idsHorarios }

                // Cargar citas canceladas del día
                val citasCanceladas = SupabaseInstance.client.from("cita")
                    .select() {
                        filter {
                            eq("estado", false)
                        }
                    }
                    .decodeList<CitaReservada>()
                    .filter { it.id_horario in idsHorarios }

                // Calcular estadísticas
                val totalHorarios = horarios.size
                val porcentajeOcupacion = if (totalHorarios > 0) {
                    (citasConfirmadas.size * 100) / totalHorarios
                } else 0

                // Obtener primera y última cita
                val horariosConCita = citasConfirmadas.mapNotNull { cita ->
                    horarios.find { it.id_horario == cita.id_horario }
                }.sortedBy { it.hora_inicio }

                val primeraCita = horariosConCita.firstOrNull()?.hora_inicio?.substring(0, 5) ?: "--:--"
                val ultimaCita = horariosConCita.lastOrNull()?.hora_inicio?.substring(0, 5) ?: "--:--"

                withContext(Dispatchers.Main) {
                    tvDiaSeleccionado.text = dia
                    tvFechaDia.text = obtenerFechaActual()
                    tvPorcentajeOcupacion.text = "$porcentajeOcupacion%"
                    tvDesocupacion.text = "${100 - porcentajeOcupacion}% Desocupación"
                    tvCitasCanceladas.text = citasCanceladas.size.toString()
                    tvCitasAgendadas.text = citasConfirmadas.size.toString()
                    tvPrimeraCita.text = primeraCita
                    tvUltimaCita.text = ultimaCita
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("ReportesActivity", "Error cargando día: ${e.message}")
                    Toast.makeText(this@ReportesActivity, "Error cargando datos del día", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun obtenerFechaActual(): String {
        val formato = SimpleDateFormat("dd MMMM", Locale("es", "ES"))
        return formato.format(Date())
    }
}