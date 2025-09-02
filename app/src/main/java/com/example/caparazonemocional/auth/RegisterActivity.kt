package com.example.caparazonemocional.auth

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.caparazonemocional.R
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.caparazonemocional.SupabaseInstance
import io.github.jan.supabase.auth.auth

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import android.widget.*

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val nameInput = findViewById<EditText>(R.id.etName)
        val emailInput = findViewById<EditText>(R.id.etEmail)
        val passwordInput = findViewById<EditText>(R.id.etPassword)
        val registerBtn = findViewById<Button>(R.id.btnRegister)
        val backToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        // ----- Texto con parte clickeable -----
        val text = "Regresar a Inicio de sesión"
        val spannable = SpannableString(text)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                finish() // volver al LoginActivity
            }
        }

        // "Inicio de sesión" será clickeable
        spannable.setSpan(clickableSpan, 11, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        backToLogin.text = spannable
        backToLogin.movementMethod = LinkMovementMethod.getInstance()


        registerBtn.setOnClickListener {
            val nameValue = nameInput.text.toString().trim()
            val emailValue = emailInput.text.toString().trim()
            val passwordValue = passwordInput.text.toString().trim()

            if (nameValue.isEmpty() || emailValue.isEmpty() || passwordValue.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Registrar con Supabase
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    SupabaseInstance.client.auth.signUpWith(Email) {
                        email = emailValue
                        password = passwordValue
                        data = buildJsonObject {
                            put("full_name", nameValue)
                        }
                    }

                    Toast.makeText(
                        this@RegisterActivity,
                        "Registro exitoso. Revisa tu correo.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish() // Regresa al LoginActivity
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.util.Log.e("RegisterActivity", "Error al registrar", e)

                    Toast.makeText(
                        this@RegisterActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
