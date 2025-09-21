package com.example.second_try

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.second_try.ui.components.AppTopBar
import com.example.second_try.ui.theme.Second_tryTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text

class TasksActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Second_tryTheme {
                TasksScreen(
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val dbRef = FirebaseDatabase.getInstance(
        "https://mental-health-72105-default-rtdb.europe-west1.firebasedatabase.app"
    ).getReference("Users").child(user!!.uid).child("quiz_progress")

    // Прогресс каждого квиза
    var quiz1Done by remember { mutableStateOf(false) }
    var quiz2Done by remember { mutableStateOf(false) }
    var quiz3Done by remember { mutableStateOf(false) }

    var dialogType by remember { mutableStateOf<Int?>(null) }

    // Загружаем прогресс из Firebase
    LaunchedEffect(Unit) {
        dbRef.get().addOnSuccessListener { snapshot ->
            quiz1Done = snapshot.child("quiz1_done").getValue(Boolean::class.java) ?: false
            quiz2Done = snapshot.child("quiz2_done").getValue(Boolean::class.java) ?: false
            quiz3Done = snapshot.child("quiz3_done").getValue(Boolean::class.java) ?: false
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Весёлые викторины!",
                onBack = {
                    val intent = Intent(context, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Готов к весёлому заданию? 🌟",
                    fontSize = 20.sp,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(24.dp))

                QuizButton(
                    text = "Начать викторину: Кто есть кто?",
                    isDone = quiz1Done,
                    onClick = { dialogType = 1 }
                )

                Spacer(modifier = Modifier.height(16.dp))

                QuizButton(
                    text = "Начать викторину: Узнай земноводное!",
                    isDone = quiz2Done,
                    onClick = { dialogType = 2 }
                )

                Spacer(modifier = Modifier.height(16.dp))

                QuizButton(
                    text = "Начать викторину: Пресмыкающееся в кадре!",
                    isDone = quiz3Done,
                    onClick = { dialogType = 3 }
                )
            }
        }
    }

    dialogType?.let { type ->
        val (title, message, activityClass) = when (type) {
            1 -> Triple(
                "Тест: Кто есть кто?",
                "В этом задании ты будешь выбирать, кто из животных — земноводное, а кто — пресмыкающееся. Посмотри на картинки внимательно и выбирай правильно! 🦎🐸",
                QuizActivity1::class.java
            )
            2 -> Triple(
                "Тест: Земноводное по названию",
                "Тебе нужно будет выбрать земноводное, соответствующее указанному названию. Все картинки — земноводные, но правильный только один! 🐸",
                QuizActivity2::class.java
            )
            3 -> Triple(
                "Тест: Пресмыкающееся по названию",
                "Теперь найди пресмыкающееся по названию! Все картинки — пресмыкающиеся, но правильный вариант только один. Удачи! 🐍",
                QuizActivity3::class.java
            )
            else -> Triple("", "", QuizActivity1::class.java)
        }

        AlertDialog(
            onDismissRequest = { dialogType = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    dialogType = null
                    context.startActivity(Intent(context, activityClass))
                }) { Text("Начнём!") }
            },
            dismissButton = {
                TextButton(onClick = { dialogType = null }) { Text("Отмена") }
            }
        )
    }

    // Если все три квиза пройдены, можно сохранять глобальный прогресс
    LaunchedEffect(quiz1Done, quiz2Done, quiz3Done) {
        if (quiz1Done && quiz2Done && quiz3Done) {
            dbRef.child("all_quizzes_done").setValue(true)
        }
    }
}

@Composable
fun QuizButton(text: String, isDone: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = onClick, modifier = Modifier.weight(1f)) {
            Text(text)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isDone) "✅" else "❌",
            fontSize = 14.sp,
            color = if (isDone) Color(0xFF388E3C) else Color(0xFFD32F2F)
        )
    }
}
