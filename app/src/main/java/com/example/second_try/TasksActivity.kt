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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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

data class QuizMenuItem(
    val id: String,
    val title: String,
    val description: String,
    val launchMode: QuizLaunchMode,
    val targetQuizId: String? = null
)

enum class QuizLaunchMode {
    IMAGE_QUIZ,     // открываем ImageQuizzesActivity и передаем quiz_id
    PHOTO_RIDDLES,   // открываем PhotoRiddlesQuizActivity
    BEASTS,           // открываем BeastsQuizActivity
    BIRDS_TEXT        // открываем BirdsQuizActivity
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser ?: return

    val dbRef = FirebaseDatabase.getInstance(
        "https://mental-health-72105-default-rtdb.europe-west1.firebasedatabase.app"
    ).getReference("Users").child(user.uid).child("quiz_progress")

    val quizItems = remember {
        listOf(
            QuizMenuItem(
                id = "photo_riddles",
                title = "Фотозагадки",
                description = "Посмотри на фото и выбери правильный ответ.",
                launchMode = QuizLaunchMode.PHOTO_RIDDLES
            ),
            QuizMenuItem(
                id = "amphibian_signs",
                title = "Признаки земноводных",
                description = "Выбирай правильное изображение по признакам амфибий.",
                launchMode = QuizLaunchMode.IMAGE_QUIZ,
                targetQuizId = "amphibian_signs"
            ),
            QuizMenuItem(
                id = "reptile_signs",
                title = "Признаки пресмыкающихся",
                description = "Тренируем признаки рептилий по изображениям.",
                launchMode = QuizLaunchMode.IMAGE_QUIZ,
                targetQuizId = "reptile_signs"
            ),
            QuizMenuItem(
                id = "who_is_who",
                title = "Кто есть кто?",
                description = "Отличаем земноводных и пресмыкающихся.",
                launchMode = QuizLaunchMode.IMAGE_QUIZ,
                targetQuizId = "who_is_who"
            ),
            QuizMenuItem(
                id = "amphibian_names",
                title = "Тренажер названий земноводных",
                description = "Выбери фото нужного земноводного.",
                launchMode = QuizLaunchMode.IMAGE_QUIZ,
                targetQuizId = "amphibian_names"
            ),
            QuizMenuItem(
                id = "reptile_names",
                title = "Тренажер названий пресмыкающихся",
                description = "Выбери фото нужного пресмыкающегося.",
                launchMode = QuizLaunchMode.IMAGE_QUIZ,
                targetQuizId = "reptile_names"
            ),
            QuizMenuItem(
                id = "dangerous_or_not",
                title = "Опасные и безобидные",
                description = "Определи опасных и безопасных животных.",
                launchMode = QuizLaunchMode.IMAGE_QUIZ,
                targetQuizId = "dangerous_or_not"
            ),
            QuizMenuItem(
                id = "beasts_quiz_1",
                title = "Звери 1",
                description = "Первая часть викторины по зверям: фото животного + 3 вопроса с подтверждением.",
                launchMode = QuizLaunchMode.BEASTS,
                targetQuizId = "1"
            ),
            QuizMenuItem(
                id = "beasts_quiz_2",
                title = "Звери 2",
                description = "Вторая часть викторины по зверям: фото животного + 3 вопроса с подтверждением.",
                launchMode = QuizLaunchMode.BEASTS,
                targetQuizId = "2"
            ),
            QuizMenuItem(
                id = "birds_quiz_1",
                title = "Птицы 1",
                description = "Первая часть викторины по птицам: один вопрос, одно фото и проверка ответа.",
                launchMode = QuizLaunchMode.BIRDS_TEXT,
                targetQuizId = "1"
            ),
            QuizMenuItem(
                id = "birds_quiz_2",
                title = "Птицы 2",
                description = "Вторая часть викторины по птицам: один вопрос, одно фото и проверка ответа.",
                launchMode = QuizLaunchMode.BIRDS_TEXT,
                targetQuizId = "2"
            ),
            )
    }

    var doneMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var dialogQuiz by remember { mutableStateOf<QuizMenuItem?>(null) }

    LaunchedEffect(Unit) {
        dbRef.get().addOnSuccessListener { snapshot ->
            val loaded = mutableMapOf<String, Boolean>()
            quizItems.forEach { item ->
                loaded["${item.id}_done"] =
                    snapshot.child("${item.id}_done").getValue(Boolean::class.java) ?: false
            }
            doneMap = loaded
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
                modifier = Modifier.padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Готов к заданиям? 🌟",
                    fontSize = 20.sp,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(20.dp))

                quizItems.forEachIndexed { index, item ->
                    QuizButton(
                        text = item.title,
                        isDone = doneMap["${item.id}_done"] == true,
                        onClick = { dialogQuiz = item }
                    )

                    if (index != quizItems.lastIndex) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    dialogQuiz?.let { item ->
        AlertDialog(
            onDismissRequest = { dialogQuiz = null },
            title = { Text(item.title) },
            text = { Text(item.description) },
            confirmButton = {
                TextButton(
                    onClick = {
                        dialogQuiz = null
                        when (item.launchMode) {
                            QuizLaunchMode.IMAGE_QUIZ -> {
                                val intent = Intent(context, ImageQuizzesActivity::class.java)
                                intent.putExtra("quiz_id", item.targetQuizId ?: item.id)
                                context.startActivity(intent)
                            }

                            QuizLaunchMode.PHOTO_RIDDLES -> {
                                val intent = Intent(context, PhotoRiddlesQuizActivity::class.java)
                                context.startActivity(intent)
                            }

                            QuizLaunchMode.BEASTS -> {
                                val intent = Intent(context, BeastsQuizActivity::class.java)
                                intent.putExtra("quiz_part", item.targetQuizId?.toIntOrNull() ?: 1)
                                context.startActivity(intent)
                            }

                            QuizLaunchMode.BIRDS_TEXT -> {
                                val intent = Intent(context, BirdsQuizActivity::class.java)
                                intent.putExtra("quiz_part", item.targetQuizId?.toIntOrNull() ?: 1)
                                context.startActivity(intent)
                            }
                        }
                    }
                ) { Text("Начнём!") }
            },
            dismissButton = {
                TextButton(onClick = { dialogQuiz = null }) { Text("Отмена") }
            }
        )
    }

    // Если все 6 пройдены — сохраняем общий флаг
    LaunchedEffect(doneMap) {
        if (quizItems.isNotEmpty() && quizItems.all { doneMap["${it.id}_done"] == true }) {
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