package com.example.second_try

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.second_try.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.database.*
import androidx.appcompat.app.AppCompatDelegate

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var usersRef: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = FirebaseDatabase.getInstance("https://mental-health-72105-default-rtdb.europe-west1.firebasedatabase.app")
        usersRef = database.getReference("Users")
        auth = FirebaseAuth.getInstance()

        binding.registerBtn.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val username = binding.usernameEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
                showError("Все поля должны быть заполнены")
                return@setOnClickListener
            }

            if (password.length < 6) {
                showError("Пароль должен содержать не менее 6 символов")
                return@setOnClickListener
            }

            // 1) Сначала пробуем создать пользователя (он автоматически станет авторизованным)
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val newUser = auth.currentUser
                        if (newUser == null) {
                            showError("Не удалось создать пользователя. Попробуйте снова.")
                            return@addOnCompleteListener
                        }

                        // 2) Теперь, когда пользователь аутентифицирован, проверяем уникальность username
                        usersRef.orderByChild("username").equalTo(username)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.exists()) {
                                        // имя уже занято — удаляем созданный аккаунт и сообщаем пользователю
                                        newUser.delete().addOnCompleteListener { delTask ->
                                            if (delTask.isSuccessful) {
                                                showError("Имя пользователя уже занято. Пожалуйста, выберите другое имя.")
                                            } else {
                                                val err = delTask.exception?.message ?: "неизвестная ошибка"
                                                showError("Имя занято, но не удалось удалить аккаунт: $err")
                                            }
                                        }
                                    } else {
                                        // имя свободно — сохраняем данные в базу
                                        val userInfo = mapOf(
                                            "email" to email,
                                            "username" to username
                                        )
                                        usersRef.child(newUser.uid).setValue(userInfo)
                                            .addOnCompleteListener { setTask ->
                                                if (setTask.isSuccessful) {
                                                    Toast.makeText(applicationContext, "Регистрация успешна!", Toast.LENGTH_LONG).show()
                                                    // можно перейти на LoginActivity или сразу на MainActivity — как тебе нужно
                                                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                                                    finish()
                                                } else {
                                                    // если не удалось записать в БД — удаляем аккаунт и показываем ошибку
                                                    newUser.delete()
                                                    val err = setTask.exception?.message ?: "Ошибка при сохранении данных"
                                                    showError("Ошибка при сохранении данных: $err")
                                                }
                                            }
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    // при ошибке чтения — удалим аккаунт и покажем сообщение
                                    auth.currentUser?.delete()
                                    showError("Ошибка при проверке имени: ${error.message}")
                                }
                            })
                    } else {
                        // обработка ошибок создания аккаунта (перевод кодов)
                        val errCode = (task.exception as? FirebaseAuthException)?.errorCode
                        val message = when (errCode) {
                            "ERROR_INVALID_EMAIL" -> "Некорректный формат email"
                            "ERROR_WEAK_PASSWORD" -> "Пароль должен содержать не менее 6 символов"
                            "ERROR_EMAIL_ALREADY_IN_USE" -> "Этот email уже зарегистрирован"
                            "ERROR_NETWORK_REQUEST_FAILED" -> "Ошибка сети. Проверьте соединение"
                            else -> {
                                // если нет кода — используем текст исключения (полный)
                                task.exception?.message ?: "Ошибка регистрации"
                            }
                        }
                        showError(message)
                    }
                }
        }

        binding.loginWithEmailBtn.setOnClickListener {
            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Установка отступов с учётом системных баров
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun showError(message: String) {
        // показываем полный текст на русском
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }
}
