package com.example.caparazonemocional

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class NotificacionesActivity : AppCompatActivity() {

    private lateinit var btnTabTodas: Button
    private lateinit var btnTabRecordatorio: Button
    private lateinit var btnTabCancelacion: Button
    private lateinit var rvNotificaciones: RecyclerView
    private lateinit var tvEmptyNotificaciones: TextView
    private lateinit var notificacionesAdapter: NotificacionesAdapter

    private val todasLasNotificaciones = mutableListOf<Notificacion>()
    private var filtroActual: TipoNotificacion? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notificaciones)

        initViews()
        setupClickListeners()
        cargarNotificaciones()
    }

    private fun initViews() {
        btnTabTodas = findViewById(R.id.btnTabTodas)
        btnTabRecordatorio = findViewById(R.id.btnTabRecordatorio)
        btnTabCancelacion = findViewById(R.id.btnTabCancelacion)
        rvNotificaciones = findViewById(R.id.rvNotificaciones)
        tvEmptyNotificaciones = findViewById(R.id.tvEmptyNotificaciones)

        // Cambiar esta línea - quitar la lista del constructor
        notificacionesAdapter = NotificacionesAdapter { notificacion ->
            abrirDetalleNotificacion(notificacion)
        }

        rvNotificaciones.layoutManager = LinearLayoutManager(this)
        rvNotificaciones.adapter = notificacionesAdapter
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnTabTodas.setOnClickListener {
            seleccionarTab(btnTabTodas)
            filtroActual = null
            filtrarNotificaciones()
        }

        btnTabRecordatorio.setOnClickListener {
            seleccionarTab(btnTabRecordatorio)
            filtroActual = TipoNotificacion.RECORDATORIO
            filtrarNotificaciones()
        }

        btnTabCancelacion.setOnClickListener {
            seleccionarTab(btnTabCancelacion)
            filtroActual = TipoNotificacion.CANCELACION
            filtrarNotificaciones()
        }

        findViewById<Button>(R.id.btnSilenciar).setOnClickListener {
            Toast.makeText(this, "Notificaciones silenciadas", Toast.LENGTH_SHORT).show()
            // TODO: Implementar lógica de silenciar notificaciones
        }
    }

    private fun seleccionarTab(tabSeleccionado: Button) {
        // Resetear todos los tabs
        btnTabTodas.background = ContextCompat.getDrawable(this, R.drawable.tab_unselected)
        btnTabTodas.setTextColor(ContextCompat.getColor(this, android.R.color.black))

        btnTabRecordatorio.background = ContextCompat.getDrawable(this, R.drawable.tab_unselected)
        btnTabRecordatorio.setTextColor(ContextCompat.getColor(this, android.R.color.black))

        btnTabCancelacion.background = ContextCompat.getDrawable(this, R.drawable.tab_unselected)
        btnTabCancelacion.setTextColor(ContextCompat.getColor(this, android.R.color.black))

        // Seleccionar el tab clickeado
        tabSeleccionado.background = ContextCompat.getDrawable(this, R.drawable.tab_selected)
        tabSeleccionado.setTextColor(ContextCompat.getColor(this, android.R.color.white))
    }

    private fun cargarNotificaciones() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notificaciones = mutableListOf<Notificacion>()

                // Obtener fecha actual
                val horaActual = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

                // 1. Cargar TODAS las citas confirmadas (sin filtro de fecha por ahora)
                val citasConfirmadas = SupabaseInstance.client.from("cita")
                    .select() {
                        filter {
                            eq("estado", true)
                        }
                    }
                    .decodeList<CitaReservada>()

                Log.d("Notificaciones", "Citas confirmadas encontradas: ${citasConfirmadas.size}")

                // Crear notificaciones de recordatorio para TODAS las citas confirmadas
                citasConfirmadas.forEach { cita ->
                    try {
                        val horario = SupabaseInstance.client.from("horario")
                            .select() {
                                filter {
                                    eq("id_horario", cita.id_horario)
                                }
                            }
                            .decodeSingle<HorarioSlot>()

                        val paciente = SupabaseInstance.client.from("paciente")
                            .select() {
                                filter {
                                    eq("id_paciente", cita.id_paciente)
                                }
                            }
                            .decodeSingle<Paciente>()

                        val modalidad = if (cita.modalidad == "presencial") "presencial" else "virtual"
                        val mensaje = "Próxima cita es a las ${horario.hora_inicio.substring(0, 5)} del día ${horario.dia} (${cita.fecha}), en modalidad $modalidad con ${paciente.nombre}."

                        notificaciones.add(
                            Notificacion(
                                id = "rec_${cita.id_cita}",
                                tipo = TipoNotificacion.RECORDATORIO,
                                mensaje = mensaje,
                                hora = horario.hora_inicio.substring(0, 5),
                                citaId = cita.id_cita
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("Notificaciones", "Error procesando cita ${cita.id_cita}: ${e.message}")
                    }
                }

                // 2. Cargar citas canceladas recientemente
                val citasCanceladas = SupabaseInstance.client.from("cita")
                    .select() {
                        filter {
                            eq("estado", false)
                        }
                    }
                    .decodeList<CitaReservada>()

                Log.d("Notificaciones", "Citas canceladas encontradas: ${citasCanceladas.size}")

                // Crear notificaciones de cancelación (solo las más recientes)
                citasCanceladas.take(10).forEach { cita ->
                    try {
                        val horario = SupabaseInstance.client.from("horario")
                            .select() {
                                filter {
                                    eq("id_horario", cita.id_horario)
                                }
                            }
                            .decodeSingle<HorarioSlot>()

                        val paciente = SupabaseInstance.client.from("paciente")
                            .select() {
                                filter {
                                    eq("id_paciente", cita.id_paciente)
                                }
                            }
                            .decodeSingle<Paciente>()

                        val mensaje = "La cita de las ${horario.hora_inicio.substring(0, 5)} del día ${cita.fecha} ha sido cancelada (${paciente.nombre})."

                        notificaciones.add(
                            Notificacion(
                                id = "cancel_${cita.id_cita}",
                                tipo = TipoNotificacion.CANCELACION,
                                mensaje = mensaje,
                                hora = horaActual,
                                citaId = cita.id_cita
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("Notificaciones", "Error cargando cita cancelada: ${e.message}")
                    }
                }

                Log.d("Notificaciones", "Total de notificaciones generadas: ${notificaciones.size}")

                withContext(Dispatchers.Main) {
                    todasLasNotificaciones.clear()
                    todasLasNotificaciones.addAll(notificaciones.sortedByDescending { it.hora })
                    filtrarNotificaciones()

                    Log.d("Notificaciones", "Notificaciones mostradas: ${todasLasNotificaciones.size}")
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("NotificacionesActivity", "Error: ${e.message}")
                    Toast.makeText(this@NotificacionesActivity, "Error cargando notificaciones: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun filtrarNotificaciones() {
        val notificacionesFiltradas = if (filtroActual == null) {
            todasLasNotificaciones
        } else {
            todasLasNotificaciones.filter { it.tipo == filtroActual }
        }

        Log.d("Notificaciones", "Filtrando: filtro=$filtroActual, total=${todasLasNotificaciones.size}, filtradas=${notificacionesFiltradas.size}")

        notificacionesAdapter.actualizarLista(notificacionesFiltradas)

        if (notificacionesFiltradas.isEmpty()) {
            rvNotificaciones.visibility = View.GONE
            tvEmptyNotificaciones.visibility = View.VISIBLE
        } else {
            rvNotificaciones.visibility = View.VISIBLE
            tvEmptyNotificaciones.visibility = View.GONE
        }
    }

    private fun abrirDetalleNotificacion(notificacion: Notificacion) {
        // Si la notificación tiene una cita asociada, abrir CitaInfoActivity
        notificacion.citaId?.let { citaId ->
            val intent = Intent(this, CitaInfoActivity::class.java)
            intent.putExtra("CITA_ID", citaId)
            startActivity(intent)
        }
    }
}