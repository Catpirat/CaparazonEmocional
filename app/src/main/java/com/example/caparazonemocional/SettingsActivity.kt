package com.example.caparazonemocional

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.caparazonemocional.auth.LoginActivity
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var switchNotificaciones: SwitchCompat
    private lateinit var switchRecordatorios: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initViews()
        setupClickListeners()
        cargarDatosUsuario()
    }

    private fun initViews() {
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        switchNotificaciones = findViewById(R.id.switchNotificaciones)
        switchRecordatorios = findViewById(R.id.switchRecordatorios)
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<LinearLayout>(R.id.btnEditarPerfil).setOnClickListener {
            Toast.makeText(this, "Editar perfil - Próximamente", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.btnCambiarContrasena).setOnClickListener {
            mostrarDialogCambiarContrasena()
        }

        switchNotificaciones.setOnCheckedChangeListener { _, isChecked ->
            guardarPreferencia("notificaciones", isChecked)
        }

        switchRecordatorios.setOnCheckedChangeListener { _, isChecked ->
            guardarPreferencia("recordatorios", isChecked)
        }

        findViewById<LinearLayout>(R.id.btnAcercaDe).setOnClickListener {
            mostrarDialogAcercaDe()
        }

        findViewById<LinearLayout>(R.id.btnAyuda).setOnClickListener {
            Toast.makeText(this, "Ayuda y soporte - Próximamente", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            mostrarDialogCerrarSesion()
        }
    }

    private fun cargarDatosUsuario() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val session = SupabaseInstance.client.auth.currentSessionOrNull()

                withContext(Dispatchers.Main) {
                    if (session != null) {
                        tvUserEmail.text = session.user?.email ?: "usuario@ejemplo.com"
                        // Aquí podrías cargar el nombre del usuario desde una tabla de perfil
                        tvUserName.text = session.user?.userMetadata?.get("full_name")?.toString()
                            ?: "Usuario"
                    }
                }

                // Cargar preferencias guardadas
                val prefs = getSharedPreferences("settings_prefs", MODE_PRIVATE)
                withContext(Dispatchers.Main) {
                    switchNotificaciones.isChecked = prefs.getBoolean("notificaciones", true)
                    switchRecordatorios.isChecked = prefs.getBoolean("recordatorios", true)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "Error cargando datos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun guardarPreferencia(key: String, value: Boolean) {
        val prefs = getSharedPreferences("settings_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean(key, value).apply()
        Toast.makeText(this, "Preferencia guardada", Toast.LENGTH_SHORT).show()
    }

    private fun mostrarDialogCambiarContrasena() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_cambiar_contrasena, null)

        val etContrasenaActual = dialogView.findViewById<EditText>(R.id.etContrasenaActual)
        val etContrasenaNueva = dialogView.findViewById<EditText>(R.id.etContrasenaNueva)
        val etConfirmarContrasena = dialogView.findViewById<EditText>(R.id.etConfirmarContrasena)

        builder.setView(dialogView)
            .setTitle("Cambiar Contraseña")
            .setPositiveButton("Cambiar") { _, _ ->
                val nuevaContrasena = etContrasenaNueva.text.toString()
                val confirmar = etConfirmarContrasena.text.toString()

                if (nuevaContrasena.length < 6) {
                    Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (nuevaContrasena != confirmar) {
                    Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                cambiarContrasena(nuevaContrasena)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cambiarContrasena(nuevaContrasena: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SupabaseInstance.client.auth.updateUser {
                    password = nuevaContrasena
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "Contraseña actualizada correctamente", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun mostrarDialogAcercaDe() {
        AlertDialog.Builder(this)
            .setTitle("Acerca de Caparazón Emocional")
            .setMessage("Versión 1.0.0\n\nApp de gestión de agenda para psicólogos.\n\n© 2025 Caparazón Emocional")
            .setPositiveButton("Aceptar", null)
            .show()
    }

    private fun mostrarDialogCerrarSesion() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás segura de que deseas cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                cerrarSesion()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cerrarSesion() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SupabaseInstance.client.auth.signOut()

                // Limpiar preferencias
                val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                prefs.edit().clear().apply()

                withContext(Dispatchers.Main) {
                    val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "Error cerrando sesión: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}