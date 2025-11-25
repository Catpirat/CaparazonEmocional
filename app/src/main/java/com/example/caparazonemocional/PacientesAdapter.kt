package com.example.caparazonemocional

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class PacientesAdapter(
    private val pacientes: MutableList<Paciente>,
    private val onEditarClick: (Paciente) -> Unit,
    private val onEliminarClick: (Paciente) -> Unit
) : RecyclerView.Adapter<PacientesAdapter.PacienteViewHolder>() {

    class PacienteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombrePaciente: TextView = view.findViewById(R.id.tvNombrePaciente)
        val tvEmailPaciente: TextView = view.findViewById(R.id.tvEmailPaciente)
        val tvTelefonoPaciente: TextView = view.findViewById(R.id.tvTelefonoPaciente)
        val tvProximaCita: TextView = view.findViewById(R.id.tvProximaCita)
        val tvEstadoPaciente: TextView = view.findViewById(R.id.tvEstadoPaciente)
        val btnEditar: Button = view.findViewById(R.id.btnEditar)
        val btnEliminar: Button = view.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PacienteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_paciente, parent, false)
        return PacienteViewHolder(view)
    }

    override fun onBindViewHolder(holder: PacienteViewHolder, position: Int) {
        val paciente = pacientes[position]

        holder.tvNombrePaciente.text = paciente.nombre
        holder.tvEmailPaciente.text = paciente.correo ?: "Sin correo"
        holder.tvTelefonoPaciente.text = paciente.telefono ?: "Sin teléfono"

        // Configurar estado
        if (paciente.estado) {
            holder.tvEstadoPaciente.text = "Activo"
            holder.tvEstadoPaciente.background = ContextCompat.getDrawable(
                holder.itemView.context, R.drawable.badge_activo
            )
        } else {
            holder.tvEstadoPaciente.text = "Inactivo"
            holder.tvEstadoPaciente.background = ContextCompat.getDrawable(
                holder.itemView.context, R.drawable.badge_inactivo
            )
        }

        // TODO: Cargar próxima cita del paciente
        holder.tvProximaCita.text = "Próxima cita: --"

        holder.btnEditar.setOnClickListener {
            onEditarClick(paciente)
        }

        holder.btnEliminar.setOnClickListener {
            onEliminarClick(paciente)
        }
    }

    override fun getItemCount() = pacientes.size

    fun filtrar(query: String, estado: String?) {
        // Implementar filtrado
    }

    fun actualizarLista(nuevosPacientes: List<Paciente>) {
        pacientes.clear()
        pacientes.addAll(nuevosPacientes)
        notifyDataSetChanged()
    }

    fun eliminarPaciente(paciente: Paciente) {
        val position = pacientes.indexOf(paciente)
        if (position != -1) {
            pacientes.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}