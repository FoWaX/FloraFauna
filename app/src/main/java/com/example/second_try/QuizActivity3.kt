package com.example.second_try

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.second_try.ui.theme.Second_tryTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.second_try.Question
import com.example.second_try.Option
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


class QuizActivity3 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Second_tryTheme {
                QuizScreen3(onBackPressed = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen3(onBackPressed: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = FirebaseAuth.getInstance().currentUser
    val dbRef = FirebaseDatabase.getInstance(
        "https://mental-health-72105-default-rtdb.europe-west1.firebasedatabase.app"
    ).getReference("Users").child(user!!.uid).child("quiz_progress")

    val questions = listOf(
        Question("Сможешь найти здесь веретеницу ломкую?", listOf(
            Option(R.drawable.pres1, false),
            Option(R.drawable.pres2, true),
            Option(R.drawable.pres3, false),
            Option(R.drawable.pres4, false)
        )),
        Question("Где здесь гадюка Никольского?", listOf(
            Option(R.drawable.pres5, true),
            Option(R.drawable.pres6, false),
            Option(R.drawable.pres7, false),
            Option(R.drawable.pres8, false)
        )),
        Question("Попробуй угадай, где здесь обыкновенный уж?", listOf(
            Option(R.drawable.pres9, true),
            Option(R.drawable.pres10, false),
            Option(R.drawable.pres11, false),
            Option(R.drawable.pres12, false)
        )),
        Question("Кто из из этих четверых - болотная черепаха?", listOf(
            Option(R.drawable.pres13, false),
            Option(R.drawable.pres14, false),
            Option(R.drawable.pres1, true),
            Option(R.drawable.pres8, false)
        ))
    )

    var currentQuestionIndex by remember { mutableStateOf(0) }
    var correctCount by remember { mutableStateOf(0) }
    var selectedAnswers by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showAnswer by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }

    val question = questions.getOrNull(currentQuestionIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Тест: Земноводные", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { onBackPressed() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back_arrow),
                            contentDescription = "Назад",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF6200EE))
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (!isFinished && question != null) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = question.text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    question.options.forEachIndexed { index, option ->
                        val wasSelected = selectedAnswers.contains(index)
                        val isCorrect = option.isCorrect

                        val backgroundColor = when {
                            !showAnswer -> Color.Transparent
                            wasSelected && isCorrect -> Color(0xFF00C853)
                            wasSelected && !isCorrect -> Color(0xFFD50000)
                            !wasSelected && isCorrect -> Color(0xFFB2FF59)
                            else -> Color.Transparent
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable(enabled = !showAnswer) {
                                    selectedAnswers = setOf(index)
                                    showAnswer = true

                                    if (option.isCorrect) {
                                        correctCount++
                                    }

                                    scope.launch {
                                        delay(1000)
                                        if (currentQuestionIndex + 1 < questions.size) {
                                            currentQuestionIndex++
                                            selectedAnswers = emptySet()
                                            showAnswer = false
                                        } else {
                                            isFinished = true
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .background(backgroundColor)
                            ) {
                                Image(
                                    painter = painterResource(id = option.imageRes),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Сохраняем прогресс, что Quiz1 пройден
                LaunchedEffect(Unit) {
                    dbRef.child("quiz3_done").setValue(true)

                    // Проверяем идеальное прохождение
                    if (correctCount == questions.size) {
                        dbRef.child("perfect_quiz3").setValue(true)
                    }
                }


                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Тест завершён!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Ты ответил(а) правильно на $correctCount из ${questions.size} вопросов.")

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(onClick = {
                        context.startActivity(Intent(context, TasksActivity::class.java))
                    }) {
                        Text("Вернуться к тестам")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(onClick = {
                        context.startActivity(Intent(context, MainActivity::class.java))
                    }) {
                        Text("Главное меню")
                    }
                }
            }
        }
    }
}

