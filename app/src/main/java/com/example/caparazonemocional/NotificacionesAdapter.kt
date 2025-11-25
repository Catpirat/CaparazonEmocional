package com.example.caparazonemocional

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotificacionesAdapter(
    private val onNotificacionClick: (Notificacion) -> Unit
) : RecyclerView.Adapter<NotificacionesAdapter.NotificacionViewHolder>() {

    private val notificaciones = mutableListOf<Notificacion>()  // Lista interna del adapter

    class NotificacionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivTipoNotificacion: ImageView = view.findViewById(R.id.ivTipoNotificacion)
        val tvTipoNotificacion: TextView = view.findViewById(R.id.tvTipoNotificacion)
        val tvMensajeNotificacion: TextView = view.findViewById(R.id.tvMensajeNotificacion)
        val tvHoraNotificacion: TextView = view.findViewById(R.id.tvHoraNotificacion)
        val btnOpcionesNotificacion: ImageView = view.findViewById(R.id.btnOpcionesNotificacion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificacionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notificacion, parent, false)
        return NotificacionViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificacionViewHolder, position: Int) {
        val notificacion = notificaciones[position]

        // Configurar icono según tipo
        when (notificacion.tipo) {
            TipoNotificacion.RECORDATORIO -> {
                holder.ivTipoNotificacion.setImageResource(R.drawable.ic_notification_bell)
                holder.tvTipoNotificacion.text = "Recordatorio"
            }
            TipoNotificacion.CANCELACION -> {
                holder.ivTipoNotificacion.setImageResource(R.drawable.ic_notification_cancel)
                holder.tvTipoNotificacion.text = "Cancelación"
            }
            TipoNotificacion.CONFIRMACION -> {
                holder.ivTipoNotificacion.setImageResource(R.drawable.ic_check)
                holder.tvTipoNotificacion.text = "Confirmación"
            }
        }

        holder.tvMensajeNotificacion.text = notificacion.mensaje
        holder.tvHoraNotificacion.text = notificacion.hora

        // Click en la notificación
        holder.itemView.setOnClickListener {
            onNotificacionClick(notificacion)
        }

        holder.btnOpcionesNotificacion.setOnClickListener {
            // TODO: Mostrar opciones (eliminar, marcar como leída, etc.)
        }
    }

    override fun getItemCount() = notificaciones.size

    fun actualizarLista(nuevasNotificaciones: List<Notificacion>) {
        notificaciones.clear()
        notificaciones.addAll(nuevasNotificaciones)
        notifyDataSetChanged()
    }
}