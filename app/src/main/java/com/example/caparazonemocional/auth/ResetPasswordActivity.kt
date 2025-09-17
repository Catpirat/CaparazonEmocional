package com.example.caparazonemocional.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.caparazonemocional.BuildConfig
import com.example.caparazonemocional.R
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ResetPasswordActivity : AppCompatActivity() {

    private var accessToken: String? = null
    private var refreshToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        val passwordInput = findViewById<EditText>(R.id.etNewPassword)
        val confirmPasswordInput = findViewById<EditText>(R.id.etConfirmPassword)
        val resetBtn = findViewById<Button>(R.id.btnSetNewPassword)

        // Manejar deep link
        intent?.data?.let { handleDeepLink(it) }

        resetBtn.setOnClickListener {
            val pass1 = passwordInput.text.toString().trim()
            val pass2 = confirmPasswordInput.text.toString().trim()

            when {
                pass1.isEmpty() || pass2.isEmpty() -> {
                    Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                }
                pass1 != pass2 -> {
                    Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                }
                accessToken == null -> {
                    Toast.makeText(this, "Token inválido — abre el enlace desde el correo", Toast.LENGTH_LONG).show()
                }
                else -> {
                    actualizarPassword(pass1)
                }
            }
        }
    }

    private fun actualizarPassword(newPassword: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val client = HttpClient(Android)
            try {
                val supaUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
                val endpoint = "$supaUrl/auth/v1/user"
                val bodyJson = """{"password":"$newPassword"}"""

                Log.d("ResetPassword", "=== HTTP REQUEST DEBUG ===")
                Log.d("ResetPassword", "URL: $endpoint")
                Log.d("ResetPassword", "Access Token: ${accessToken?.take(50)}...")
                Log.d("ResetPassword", "Body: $bodyJson")

                // CAMBIO: usar PUT en lugar de PATCH
                val response: HttpResponse = client.put(endpoint) {
                    header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    header("Authorization", "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                    setBody(bodyJson)
                }

                Log.d("ResetPassword", "Response Status: ${response.status}")

                val responseBody = response.bodyAsText()
                Log.d("ResetPassword", "Response Body: $responseBody")

                if (response.status.isSuccess()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(
                            this@ResetPasswordActivity,
                            "Contraseña actualizada correctamente",
                            Toast.LENGTH_LONG
                        ).show()
                        startActivity(Intent(this@ResetPasswordActivity, LoginActivity::class.java))
                        finish()
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(
                            this@ResetPasswordActivity,
                            "Error ${response.status.value}: $responseBody",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ResetPassword", "Exception: ${e.message}", e)
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(
                        this@ResetPasswordActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                client.close()
            }
        }
    }

    private fun handleDeepLink(data: Uri) {
        // Agregar logs para debug
        println("Deep link recibido: $data")
        println("Fragment: ${data.fragment}")

        data.fragment?.split("&")?.forEach { param ->
            val parts = param.split("=", limit = 2) // limita a 2 partes
            if (parts.size == 2) {
                val key = parts[0]
                val value = parts[1]
                println("Parámetro: $key = $value")

                when (key) {
                    "access_token" -> {
                        accessToken = value
                        println("Access token guardado")
                    }
                    "refresh_token" -> {
                        refreshToken = value
                        println("Refresh token guardado")
                    }
                }
            }
        }

        // Validar que tienes el token
        if (accessToken != null) {
            Toast.makeText(this, "Token recibido correctamente", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Error: no se recibió access_token", Toast.LENGTH_LONG).show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { handleDeepLink(it) }
    }
}