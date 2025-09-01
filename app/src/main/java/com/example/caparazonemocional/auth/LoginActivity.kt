package com.example.caparazonemocional.auth

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.caparazonemocional.MainActivity
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

        val emailInput = findViewById<EditText>(R.id.etEmailLogin)
        val passwordInput = findViewById<EditText>(R.id.etPasswordLogin)
        val loginBtn = findViewById<Button>(R.id.btnLogin)
        val createAccount = findViewById<TextView>(R.id.tvCreateAccount)

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
                    Toast.makeText(this@LoginActivity, "Inicio de sesión exitoso", Toast.LENGTH_LONG).show()

                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
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
