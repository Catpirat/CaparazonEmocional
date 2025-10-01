package com.example.caparazonemocional

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
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
    private lateinit var btnNotas: Button

    private var citaId: Int? = null
    private var pacienteId: Int? = null

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
        btnNotas = findViewById(R.id.btnNotas)
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnCancelarCita.setOnClickListener {
            mostrarDialogCancelar()
        }

        btnNotas.setOnClickListener {
            // TODO: Abrir pantalla de notas del paciente
            Toast.makeText(this, "Notas - Próximamente", Toast.LENGTH_SHORT).show()
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
                pacienteId = cita.id_paciente

                // Cargar información del paciente
                val pacientes = SupabaseInstance.client.from("paciente")
                    .select() {  // Traer todos los campos
                        filter {
                            eq("id_paciente", cita.id_paciente)
                        }
                    }
                    .decodeList<Paciente>()

                // Cargar información del horario
                val horarios = SupabaseInstance.client.from("horario")
                    .select() {  // Cambiar a select() sin parámetros para traer todos los campos
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
}