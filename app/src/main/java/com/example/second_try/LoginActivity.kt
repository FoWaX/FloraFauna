    package com.example.second_try

    import android.content.Intent
    import android.os.Bundle
    import android.widget.Toast
    import androidx.appcompat.app.AppCompatActivity
    import com.example.second_try.databinding.ActivityLoginBinding
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.database.FirebaseDatabase
    import java.text.SimpleDateFormat
    import java.util.Date
    import java.util.Locale
    import androidx.appcompat.app.AppCompatDelegate


    class LoginActivity : AppCompatActivity() {

        private lateinit var auth: FirebaseAuth
        private lateinit var binding: ActivityLoginBinding

        override fun onCreate(savedInstanceState: Bundle?) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

            super.onCreate(savedInstanceState)

            binding = ActivityLoginBinding.inflate(layoutInflater)
            setContentView(binding.root)

            auth = FirebaseAuth.getInstance()

            // Логин через email и пароль
            binding.loginBtn.setOnClickListener {
                val email = binding.emailEditText.text.toString()
                val password = binding.passwordEditText.text.toString()

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(applicationContext, "Пустые поля", Toast.LENGTH_SHORT).show()
                } else {
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                checkStatusAndNavigate()
                            } else {
                                Toast.makeText(applicationContext, "Ошибка авторизации", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }

            // Переход на экран регистрации
            binding.registerBtn.setOnClickListener {
                startActivity(Intent(this, RegisterActivity::class.java))
            }
        }

        private fun checkStatusAndNavigate() {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(applicationContext, "Пользователь не найден", Toast.LENGTH_SHORT).show()
                return
            }

            val userId = currentUser.uid
            val databaseRef = FirebaseDatabase.getInstance("https://mental-health-72105-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("Users")
                .child(userId)

            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            navigateToMain()
        }

        private fun navigateToMain() {
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            finish()
        }
    }
