package com.example.caparazonemocional.auth

import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.caparazonemocional.R
import com.example.caparazonemocional.SupabaseInstance
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val emailInput = findViewById<EditText>(R.id.etForgotEmail)
        val resetBtn = findViewById<Button>(R.id.btnResetPassword)
        val backToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        // Texto clickeable "Regresar a inicio de sesión"
        val text = "Regresar a Inicio de sesión"
        val spannable = SpannableString(text)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(this@ForgotPasswordActivity, LoginActivity::class.java))
                finish()
            }
        }
        spannable.setSpan(clickableSpan, 11, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        backToLogin.text = spannable
        backToLogin.movementMethod = LinkMovementMethod.getInstance()

        resetBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Ingresa un correo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Lanzamos en IO
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Llamada simple: Supabase usará el "Site URL" configurado en el dashboard
                    SupabaseInstance.client.auth.resetPasswordForEmail(email)

                    // Mensaje en Main
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(
                            this@ForgotPasswordActivity,
                            "Revisa tu correo para restablecer tu contraseña",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(
                            this@ForgotPasswordActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
}
