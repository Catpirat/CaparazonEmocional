package com.example.caparazonemocional

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class MisPacientesActivity : AppCompatActivity() {

    private lateinit var tvTitulo: TextView
    private lateinit var etBuscar: EditText
    private lateinit var spinnerFiltro: Spinner
    private lateinit var rvPacientes: RecyclerView
    private lateinit var tvEmptyPacientes: TextView
    private lateinit var pacientesAdapter: PacientesAdapter

    private val todosPacientes = mutableListOf<Paciente>()
    private val pacientesFiltrados = mutableListOf<Paciente>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_pacientes)

        initViews()
        setupClickListeners()
        setupSpinner()
        setupBusqueda()
        cargarPacientes()
    }

    private fun initViews() {
        tvTitulo = findViewById(R.id.tvTitulo)
        etBuscar = findViewById(R.id.etBuscar)
        spinnerFiltro = findViewById(R.id.spinnerFiltro)
        rvPacientes = findViewById(R.id.rvPacientes)
        tvEmptyPacientes = findViewById(R.id.tvEmptyPacientes)

        pacientesAdapter = PacientesAdapter(
            pacientesFiltrados,
            onEditarClick = { paciente -> mostrarDialogEditarPaciente(paciente) },
            onEliminarClick = { paciente -> mostrarDialogEliminar(paciente) }
        )

        rvPacientes.layoutManager = LinearLayoutManager(this)
        rvPacientes.adapter = pacientesAdapter
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<ImageView>(R.id.btnReportes).setOnClickListener {
            startActivity(Intent(this, ReportesActivity::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.btnHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.btnNotificaciones).setOnClickListener {
            startActivity(Intent(this, NotificacionesActivity::class.java))
            finish()
        }

        findViewById<Button>(R.id.btnAgregarPaciente).setOnClickListener {
            mostrarDialogAgregarPaciente()
        }
    }

    private fun setupSpinner() {
        val filtros = arrayOf("Todos", "Activos", "Inactivos")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filtros)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFiltro.adapter = adapter

        spinnerFiltro.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filtrarPacientes()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupBusqueda() {
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filtrarPacientes()
            }
        })
    }

    private fun cargarPacientes() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pacientes = SupabaseInstance.client.from("paciente")
                    .select()
                    .decodeList<Paciente>()

                withContext(Dispatchers.Main) {
                    todosPacientes.clear()
                    todosPacientes.addAll(pacientes)
                    filtrarPacientes()
                    actualizarTitulo()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("MisPacientes", "Error: ${e.message}")
                    Toast.makeText(this@MisPacientesActivity, "Error cargando pacientes: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun filtrarPacientes() {
        val query = etBuscar.text.toString().lowercase()
        val filtroEstado = when (spinnerFiltro.selectedItemPosition) {
            1 -> true  // Activos
            2 -> false // Inactivos
            else -> null // Todos
        }

        pacientesFiltrados.clear()
        pacientesFiltrados.addAll(
            todosPacientes.filter { paciente ->
                val coincideBusqueda = query.isEmpty() ||
                        paciente.nombre.lowercase().contains(query) ||
                        paciente.correo?.lowercase()?.contains(query) == true

                val coincideEstado = filtroEstado == null || paciente.estado == filtroEstado

                coincideBusqueda && coincideEstado
            }
        )

        pacientesAdapter.notifyDataSetChanged()

        if (pacientesFiltrados.isEmpty()) {
            rvPacientes.visibility = View.GONE
            tvEmptyPacientes.visibility = View.VISIBLE
        } else {
            rvPacientes.visibility = View.VISIBLE
            tvEmptyPacientes.visibility = View.GONE
        }
    }

    private fun actualizarTitulo() {
        tvTitulo.text = "Gestión de Pacientes (${todosPacientes.size})"
    }

    private fun mostrarDialogAgregarPaciente() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_paciente)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvTitulo = dialog.findViewById<TextView>(R.id.tvTituloDialog)
        val etNombre = dialog.findViewById<EditText>(R.id.etNombrePaciente)
        val etEdad = dialog.findViewById<EditText>(R.id.etEdadPaciente)
        val etTelefono = dialog.findViewById<EditText>(R.id.etTelefonoPaciente)
        val etCorreo = dialog.findViewById<EditText>(R.id.etCorreoPaciente)
        val btnCancelar = dialog.findViewById<Button>(R.id.btnCancelarPaciente)
        val btnGuardar = dialog.findViewById<Button>(R.id.btnGuardarPaciente)

        tvTitulo.text = "Agregar Paciente"

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            if (nombre.isEmpty()) {
                Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val edad = etEdad.text.toString().toIntOrNull()
            val telefono = etTelefono.text.toString().trim().ifEmpty { null }
            val correo = etCorreo.text.toString().trim().ifEmpty { null }

            agregarPaciente(nombre, edad, telefono, correo)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun agregarPaciente(nombre: String, edad: Int?, telefono: String?, correo: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {

                val jsonData = kotlinx.serialization.json.buildJsonObject {
                    put("nombre", nombre)
                    put("estado", true)
                    if (edad != null) put("edad", edad)
                    if (telefono != null) put("telefono", telefono)
                    if (correo != null) put("correo", correo)
                }

                SupabaseInstance.client.from("paciente").insert(jsonData)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MisPacientesActivity, "Paciente agregado", Toast.LENGTH_SHORT).show()
                    cargarPacientes()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("MisPacientes", "Error: ${e.message}")
                    Toast.makeText(this@MisPacientesActivity, "Error agregando: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun mostrarDialogEditarPaciente(paciente: Paciente) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_paciente)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvTitulo = dialog.findViewById<TextView>(R.id.tvTituloDialog)
        val etNombre = dialog.findViewById<EditText>(R.id.etNombrePaciente)
        val etEdad = dialog.findViewById<EditText>(R.id.etEdadPaciente)
        val etTelefono = dialog.findViewById<EditText>(R.id.etTelefonoPaciente)
        val etCorreo = dialog.findViewById<EditText>(R.id.etCorreoPaciente)
        val btnCancelar = dialog.findViewById<Button>(R.id.btnCancelarPaciente)
        val btnGuardar = dialog.findViewById<Button>(R.id.btnGuardarPaciente)

        tvTitulo.text = "Editar Paciente"
        etNombre.setText(paciente.nombre)
        etEdad.setText(paciente.edad?.toString() ?: "")
        etTelefono.setText(paciente.telefono ?: "")
        etCorreo.setText(paciente.correo ?: "")

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            if (nombre.isEmpty()) {
                Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val edad = etEdad.text.toString().toIntOrNull()
            val telefono = etTelefono.text.toString().trim().ifEmpty { null }
            val correo = etCorreo.text.toString().trim().ifEmpty { null }

            editarPaciente(paciente.id_paciente!!, nombre, edad, telefono, correo)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun editarPaciente(id: Int, nombre: String, edad: Int?, telefono: String?, correo: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {

                val jsonData = kotlinx.serialization.json.buildJsonObject {
                    put("nombre", nombre)
                    put("edad", edad)
                    put("telefono", telefono)
                    put("correo", correo)
                }

                SupabaseInstance.client.from("paciente").update(jsonData) {
                    filter {
                        eq("id_paciente", id)
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MisPacientesActivity, "Paciente actualizado", Toast.LENGTH_SHORT).show()
                    cargarPacientes()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("MisPacientes", "Error: ${e.message}")
                    Toast.makeText(this@MisPacientesActivity, "Error actualizando: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun mostrarDialogEliminar(paciente: Paciente) {
        AlertDialog.Builder(this)
            .setTitle("Dar de baja paciente")
            .setMessage("¿Deseas marcar a ${paciente.nombre} como inactivo?")
            .setPositiveButton("Sí") { _, _ ->
                darDeBajaPaciente(paciente.id_paciente!!)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun darDeBajaPaciente(id: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SupabaseInstance.client.from("paciente").update(
                    mapOf("estado" to false)
                ) {
                    filter {
                        eq("id_paciente", id)
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MisPacientesActivity, "Paciente dado de baja", Toast.LENGTH_SHORT).show()
                    cargarPacientes()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("MisPacientes", "Error: ${e.message}")
                    Toast.makeText(this@MisPacientesActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}