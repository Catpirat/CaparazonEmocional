package com.example.caparazonemocional

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class NotasAdapter(
    private val notas: MutableList<Observacion>,
    private val onEditClick: (Observacion) -> Unit,  // Agregar callback de editar
    private val onDeleteClick: (Observacion) -> Unit
) : RecyclerView.Adapter<NotasAdapter.NotaViewHolder>() {

    class NotaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFechaNota: TextView = view.findViewById(R.id.tvFechaNota)
        val tvMensajeNota: TextView = view.findViewById(R.id.tvMensajeNota)
        val btnEditNota: ImageView = view.findViewById(R.id.btnEditNota)  // Agregar botón editar
        val btnDeleteNota: ImageView = view.findViewById(R.id.btnDeleteNota)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nota, parent, false)
        return NotaViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotaViewHolder, position: Int) {
        val nota = notas[position]

        // Formatear fecha
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val date = inputFormat.parse(nota.fecha_hora.substring(0, 19))
            holder.tvFechaNota.text = date?.let { outputFormat.format(it) } ?: nota.fecha_hora
        } catch (e: Exception) {
            holder.tvFechaNota.text = nota.fecha_hora
        }

        holder.tvMensajeNota.text = nota.mensaje

        holder.btnEditNota.setOnClickListener {
            onEditClick(nota)
        }

        holder.btnDeleteNota.setOnClickListener {
            onDeleteClick(nota)
        }
    }

    override fun getItemCount() = notas.size

    fun removeItem(nota: Observacion) {
        val position = notas.indexOf(nota)
        if (position != -1) {
            notas.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateItem(nota: Observacion) {
        val position = notas.indexOfFirst { it.id_observaciones == nota.id_observaciones }
        if (position != -1) {
            notas[position] = nota
            notifyItemChanged(position)
        }
    }
}