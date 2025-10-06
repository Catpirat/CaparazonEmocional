package com.example.caparazonemocional

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class NotasPacienteActivity : AppCompatActivity() {

    private lateinit var tvPatientName: TextView
    private lateinit var rvNotas: RecyclerView
    private lateinit var tvEmptyNotas: TextView
    private lateinit var notasAdapter: NotasAdapter

    private var pacienteId: Int? = null
    private var historialId: Int? = null
    private val notas = mutableListOf<Observacion>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notas_paciente)

        initViews()
        setupClickListeners()

        // Recibir datos del paciente
        pacienteId = intent.getIntExtra("PACIENTE_ID", -1)
        val pacienteNombre = intent.getStringExtra("PACIENTE_NOMBRE")

        if (pacienteId == -1) {
            Toast.makeText(this, "Error: Paciente no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvPatientName.text = pacienteNombre

        cargarHistorialYNotas()
    }

    private fun initViews() {
        tvPatientName = findViewById(R.id.tvPatientName)
        rvNotas = findViewById(R.id.rvNotas)
        tvEmptyNotas = findViewById(R.id.tvEmptyNotas)

        notasAdapter = NotasAdapter(
            notas,
            onEditClick = { nota -> mostrarDialogEditarNota(nota) },  // Agregar callback
            onDeleteClick = { nota -> mostrarDialogEliminar(nota) }
        )

        rvNotas.layoutManager = LinearLayoutManager(this)
        rvNotas.adapter = notasAdapter
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<ImageView>(R.id.btnAddNota).setOnClickListener {
            mostrarDialogAgregarNota()
        }
    }

    private fun cargarHistorialYNotas() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Buscar o crear historial médico
                val historiales = SupabaseInstance.client.from("historial_medico")
                    .select() {
                        filter {
                            eq("id_paciente", pacienteId!!)
                        }
                    }
                    .decodeList<HistorialMedico>()

                historialId = if (historiales.isEmpty()) {
                    // Crear nuevo historial
                    val fechaActual = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                    val nuevoHistorial = HistorialMedico(
                        id_historial = null,
                        id_paciente = pacienteId!!,
                        fecha_registro = fechaActual
                    )

                    val resultado = SupabaseInstance.client.from("historial_medico")
                        .insert(nuevoHistorial) {
                            select()
                        }
                        .decodeSingle<HistorialMedico>()

                    resultado.id_historial
                } else {
                    historiales[0].id_historial
                }

                // Cargar observaciones
                cargarNotas()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("NotasPaciente", "Error: ${e.message}")
                    Toast.makeText(this@NotasPacienteActivity, "Error cargando datos: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun cargarNotas() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val observaciones = SupabaseInstance.client.from("observaciones")
                    .select() {
                        filter {
                            eq("id_historial", historialId!!)
                        }
                    }
                    .decodeList<Observacion>()

                withContext(Dispatchers.Main) {
                    notas.clear()
                    notas.addAll(observaciones.sortedByDescending { it.fecha_hora })
                    notasAdapter.notifyDataSetChanged()

                    if (notas.isEmpty()) {
                        rvNotas.visibility = android.view.View.GONE
                        tvEmptyNotas.visibility = android.view.View.VISIBLE
                    } else {
                        rvNotas.visibility = android.view.View.VISIBLE
                        tvEmptyNotas.visibility = android.view.View.GONE
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("NotasPaciente", "Error cargando notas: ${e.message}")
                    Toast.makeText(this@NotasPacienteActivity, "Error cargando notas", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mostrarDialogAgregarNota() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_nota)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val etNota = dialog.findViewById<EditText>(R.id.etNota)
        val btnCancelar = dialog.findViewById<Button>(R.id.btnCancelarNota)
        val btnGuardar = dialog.findViewById<Button>(R.id.btnGuardarNota)

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        btnGuardar.setOnClickListener {
            val mensaje = etNota.text.toString().trim()
            if (mensaje.isEmpty()) {
                Toast.makeText(this, "Escribe una nota", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            guardarNota(mensaje)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun guardarNota(mensaje: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fechaHora = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

                // Cambiar la sintaxis del insert
                val nuevaNota = Observacion(
                    id_observaciones = null,
                    id_historial = historialId!!,
                    mensaje = mensaje,
                    fecha_hora = fechaHora
                )

                SupabaseInstance.client.from("observaciones")
                    .insert(nuevaNota)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@NotasPacienteActivity, "Nota guardada", Toast.LENGTH_SHORT).show()
                    cargarNotas()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("NotasPaciente", "Error guardando: ${e.message}")
                    Toast.makeText(this@NotasPacienteActivity, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun mostrarDialogEditarNota(nota: Observacion) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_nota)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val etNota = dialog.findViewById<EditText>(R.id.etNota)
        val btnCancelar = dialog.findViewById<Button>(R.id.btnCancelarNota)
        val btnGuardar = dialog.findViewById<Button>(R.id.btnGuardarNota)

        // Pre-llenar con el texto actual
        etNota.setText(nota.mensaje)
        etNota.setSelection(nota.mensaje.length) // Colocar cursor al final

        // Cambiar texto del botón
        btnGuardar.text = "Actualizar"

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        btnGuardar.setOnClickListener {
            val mensaje = etNota.text.toString().trim()
            if (mensaje.isEmpty()) {
                Toast.makeText(this, "Escribe una nota", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            editarNota(nota, mensaje)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun editarNota(nota: Observacion, nuevoMensaje: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SupabaseInstance.client.from("observaciones")
                    .update(mapOf("mensaje" to nuevoMensaje)) {
                        filter {
                            eq("id_observaciones", nota.id_observaciones!!)
                        }
                    }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@NotasPacienteActivity, "Nota actualizada", Toast.LENGTH_SHORT).show()
                    // Actualizar la nota en la lista local
                    val notaActualizada = nota.copy(mensaje = nuevoMensaje)
                    notasAdapter.updateItem(notaActualizada)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("NotasPaciente", "Error editando: ${e.message}")
                    Toast.makeText(this@NotasPacienteActivity, "Error al editar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun mostrarDialogEliminar(nota: Observacion) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Nota")
            .setMessage("¿Estás segura de que deseas eliminar esta nota?")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarNota(nota)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarNota(nota: Observacion) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SupabaseInstance.client.from("observaciones")
                    .delete {
                        filter {
                            eq("id_observaciones", nota.id_observaciones!!)
                        }
                    }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@NotasPacienteActivity, "Nota eliminada", Toast.LENGTH_SHORT).show()
                    notasAdapter.removeItem(nota)

                    if (notas.isEmpty()) {
                        rvNotas.visibility = android.view.View.GONE
                        tvEmptyNotas.visibility = android.view.View.VISIBLE
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("NotasPaciente", "Error eliminando: ${e.message}")
                    Toast.makeText(this@NotasPacienteActivity, "Error al eliminar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}