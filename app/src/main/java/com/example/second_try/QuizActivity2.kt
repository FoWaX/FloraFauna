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


class QuizActivity2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Second_tryTheme {
                QuizScreen2(onBackPressed = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen2(onBackPressed: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = FirebaseAuth.getInstance().currentUser
    val dbRef = FirebaseDatabase.getInstance(
        "https://mental-health-72105-default-rtdb.europe-west1.firebasedatabase.app"
    ).getReference("Users").child(user!!.uid).child("quiz_progress")

    val questions = listOf(
        Question("Кто из них зеленая  жаба?", listOf(
            Option(R.drawable.zem1, false),
            Option(R.drawable.zem2, true),
            Option(R.drawable.zem3, false),
            Option(R.drawable.zem4, false)
        )),
        Question("Где здесь обыкновенный тритон?", listOf(
            Option(R.drawable.zem5, false),
            Option(R.drawable.zem6, false),
            Option(R.drawable.zem7, true),
            Option(R.drawable.zem8, false)
        )),
        Question("Найди остромордую лягушку", listOf(
            Option(R.drawable.zem9, false),
            Option(R.drawable.zem10, false),
            Option(R.drawable.zem11, false),
            Option(R.drawable.zem12, true)
        )),
        Question("Только одно из них — краснобрюхая жерлянка. Какое?", listOf(
            Option(R.drawable.zem6, false),
            Option(R.drawable.zem4, true),
            Option(R.drawable.zem9, false),
            Option(R.drawable.zem1, false)
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
                LaunchedEffect(Unit) {
                    dbRef.child("quiz2_done").setValue(true)
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

