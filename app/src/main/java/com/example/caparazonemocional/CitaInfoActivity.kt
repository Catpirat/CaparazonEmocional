package com.example.caparazonemocional

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CitaInfoActivity : AppCompatActivity() {

    private lateinit var tvPatientName: TextView
    private lateinit var tvPatientEmail: TextView
    private lateinit var tvTherapyType: TextView
    private lateinit var tvModality: TextView
    private lateinit var tvSchedule: TextView
    private lateinit var btnCancelarCita: Button
    private lateinit var btnReprogramarCita: Button
    private lateinit var btnNotas: Button

    private var citaId: Int? = null
    private var pacienteId: Int? = null
    private var horarioActual: HorarioSlot? = null
    private var horariosDisponibles = mutableListOf<HorarioSlot>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cita_info)

        initViews()
        setupClickListeners()

        // Recibir ID de la cita desde el Intent
        citaId = intent.getIntExtra("CITA_ID", -1)
        if (citaId == -1) {
            Toast.makeText(this, "Error: Cita no encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        cargarDatosCita()
    }

    private fun initViews() {
        tvPatientName = findViewById(R.id.tvPatientName)
        tvPatientEmail = findViewById(R.id.tvPatientEmail)
        tvTherapyType = findViewById(R.id.tvTherapyType)
        tvModality = findViewById(R.id.tvModality)
        tvSchedule = findViewById(R.id.tvSchedule)
        btnCancelarCita = findViewById(R.id.btnCancelarCita)
        btnReprogramarCita = findViewById(R.id.btnReprogramarCita)
        btnNotas = findViewById(R.id.btnNotas)
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnCancelarCita.setOnClickListener {
            mostrarDialogCancelar()
        }

        btnReprogramarCita.setOnClickListener {
            mostrarDialogReprogramar()
        }

        btnNotas.setOnClickListener {
            if (pacienteId != null) {
                val intent = Intent(this, NotasPacienteActivity::class.java)
                intent.putExtra("PACIENTE_ID", pacienteId)
                intent.putExtra("PACIENTE_NOMBRE", tvPatientName.text.toString())
                startActivity(intent)
            } else {
                Toast.makeText(this, "Error: Paciente no encontrado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarDatosCita() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Cargar información de la cita
                val citas = SupabaseInstance.client.from("cita")
                    .select(Columns.list(
                        "id_cita", "id_paciente", "id_horario",
                        "fecha", "modalidad", "motivo", "estado"
                    )) {
                        filter {
                            eq("id_cita", citaId!!)
                        }
                    }
                    .decodeList<CitaReservada>()

                if (citas.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CitaInfoActivity, "Cita no encontrada", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    return@launch
                }

                val cita = citas[0]
                pacienteId = cita.id_paciente  // Guardar el ID del paciente aquí

                // Cargar información del paciente
                val pacientes = SupabaseInstance.client.from("paciente")
                    .select() {
                        filter {
                            eq("id_paciente", cita.id_paciente)
                        }
                    }
                    .decodeList<Paciente>()

                // Cargar información del horario
                val horarios = SupabaseInstance.client.from("horario")
                    .select() {
                        filter {
                            eq("id_horario", cita.id_horario)
                        }
                    }
                    .decodeList<HorarioSlot>()

                withContext(Dispatchers.Main) {
                    if (pacientes.isNotEmpty() && horarios.isNotEmpty()) {
                        mostrarDatos(cita, pacientes[0], horarios[0])
                    } else {
                        Toast.makeText(this@CitaInfoActivity, "Error cargando datos", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("CitaInfoActivity", "Error: ${e.message}")
                    Toast.makeText(this@CitaInfoActivity, "Error cargando cita: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun mostrarDatos(cita: CitaReservada, paciente: Paciente, horario: HorarioSlot) {
        tvPatientName.text = paciente.nombre
        tvPatientEmail.text = paciente.correo ?: "Sin correo"
        tvTherapyType.text = cita.motivo
        tvModality.text = if (cita.modalidad == "presencial") "Presencial" else "Virtual"
        tvSchedule.text = "${horario.hora_inicio.substring(0, 5)} - ${horario.hora_fin.substring(0, 5)}"

        horarioActual = horario  // Guardar horario actual
    }

    private fun mostrarDialogCancelar() {
        AlertDialog.Builder(this)
            .setTitle("Cancelar Cita")
            .setMessage("¿Estás segura de que deseas cancelar esta cita?")
            .setPositiveButton("Sí") { _, _ ->
                cancelarCita()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelarCita() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SupabaseInstance.client.from("cita").update(
                    mapOf("estado" to false)  // Cambiar a false en lugar de "cancelada"
                ) {
                    filter {
                        eq("id_cita", citaId!!)
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CitaInfoActivity, "Cita cancelada", Toast.LENGTH_SHORT).show()
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("CitaInfoActivity", "Error cancelando: ${e.message}")
                    Toast.makeText(this@CitaInfoActivity, "Error al cancelar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun mostrarDialogReprogramar() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_reprogramar_cita)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val spinnerDia = dialog.findViewById<Spinner>(R.id.spinnerDia)
        val spinnerHorario = dialog.findViewById<Spinner>(R.id.spinnerHorario)
        val btnCancelar = dialog.findViewById<Button>(R.id.btnCancelarReprogramar)
        val btnConfirmar = dialog.findViewById<Button>(R.id.btnConfirmarReprogramar)

        // Configurar spinner de días
        val dias = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
        val diaAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, dias)
        diaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDia.adapter = diaAdapter

        // Listener para cargar horarios cuando se selecciona un día
        spinnerDia.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val diaSeleccionado = dias[position]
                cargarHorariosDisponibles(diaSeleccionado, spinnerHorario)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirmar.setOnClickListener {
            if (horariosDisponibles.isEmpty()) {
                Toast.makeText(this, "No hay horarios disponibles", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val horarioSeleccionado = horariosDisponibles[spinnerHorario.selectedItemPosition]
            reprogramarCita(horarioSeleccionado.id_horario!!)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun cargarHorariosDisponibles(dia: String, spinnerHorario: Spinner) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Cargar todos los horarios del día seleccionado que estén habilitados
                val horarios = SupabaseInstance.client.from("horario")
                    .select() {
                        filter {
                            eq("dia", dia)
                            eq("estado", true)
                        }
                    }
                    .decodeList<HorarioSlot>()

                // Cargar citas confirmadas para ese día
                val citasDelDia = SupabaseInstance.client.from("cita")
                    .select() {
                        filter {
                            eq("estado", true)
                        }
                    }
                    .decodeList<CitaReservada>()

                // Filtrar solo horarios disponibles (sin cita)
                val horariosOcupados = citasDelDia.map { it.id_horario }.toSet()
                horariosDisponibles = horarios.filter { it.id_horario !in horariosOcupados }.toMutableList()

                withContext(Dispatchers.Main) {
                    if (horariosDisponibles.isEmpty()) {
                        val mensaje = listOf("No hay horarios disponibles")
                        val adapter = ArrayAdapter(this@CitaInfoActivity, android.R.layout.simple_spinner_item, mensaje)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerHorario.adapter = adapter
                    } else {
                        val horariosTexto = horariosDisponibles.map {
                            "${it.hora_inicio.substring(0, 5)} - ${it.hora_fin.substring(0, 5)} (${if (it.virtual) "Virtual" else "Presencial"})"
                        }
                        val adapter = ArrayAdapter(this@CitaInfoActivity, android.R.layout.simple_spinner_item, horariosTexto)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerHorario.adapter = adapter
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("CitaInfoActivity", "Error cargando horarios: ${e.message}")
                    Toast.makeText(this@CitaInfoActivity, "Error cargando horarios", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun reprogramarCita(nuevoIdHorario: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SupabaseInstance.client.from("cita").update(
                    mapOf("id_horario" to nuevoIdHorario)
                ) {
                    filter {
                        eq("id_cita", citaId!!)
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CitaInfoActivity, "Cita reprogramada exitosamente", Toast.LENGTH_SHORT).show()
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("CitaInfoActivity", "Error reprogramando: ${e.message}")
                    Toast.makeText(this@CitaInfoActivity, "Error al reprogramar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}