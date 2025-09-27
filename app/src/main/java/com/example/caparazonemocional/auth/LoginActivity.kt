package com.example.caparazonemocional.auth

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.caparazonemocional.AgendaActivity
import com.example.caparazonemocional.R
import com.example.caparazonemocional.SupabaseInstance
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Revisar si recordar sesión
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val remember = prefs.getBoolean("remember_me", false)

        CoroutineScope(Dispatchers.Main).launch {
            val session = SupabaseInstance.client.auth.currentSessionOrNull()
            if (remember && session != null) {
                //startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                //finish()
                startActivity(Intent(this@LoginActivity, AgendaActivity::class.java))
                finish()
            }
        }

        val emailInput = findViewById<EditText>(R.id.etEmailLogin)
        val passwordInput = findViewById<EditText>(R.id.etPasswordLogin)
        val loginBtn = findViewById<Button>(R.id.btnLogin)
        val createAccount = findViewById<TextView>(R.id.tvCreateAccount)
        val rememberMe = findViewById<CheckBox>(R.id.cbRememberMe)
        val forgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        forgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        loginBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    SupabaseInstance.client.auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }

                    // Guardar preferencia
                    prefs.edit()
                        .putBoolean("remember_me", rememberMe.isChecked)
                        .apply()

                    Toast.makeText(this@LoginActivity, "Inicio de sesión exitoso", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@LoginActivity, AgendaActivity::class.java))
                    finish()

                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        createAccount.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
