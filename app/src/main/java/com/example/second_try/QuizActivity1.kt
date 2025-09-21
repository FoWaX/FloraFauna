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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.second_try.ui.theme.Second_tryTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.example.second_try.Question
import com.example.second_try.Option

data class Question(val text: String, val options: List<Option>)
data class Option(val imageRes: Int, val isCorrect: Boolean)



class QuizActivity1 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Second_tryTheme {
                QuizScreen1(onBackPressed = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen1(onBackPressed: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = FirebaseAuth.getInstance().currentUser
    val dbRef = FirebaseDatabase.getInstance(
        "https://mental-health-72105-default-rtdb.europe-west1.firebasedatabase.app"
    ).getReference("Users").child(user!!.uid).child("quiz_progress")

    val questions = listOf(
        Question("Кто из них земноводное?", listOf(
            Option(R.drawable.zem1, true),
            Option(R.drawable.pres1, false),
            Option(R.drawable.pres2, false),
            Option(R.drawable.zem2, true)
        )),
        Question("Выбери пресмыкающееся:", listOf(
            Option(R.drawable.zem3, false),
            Option(R.drawable.pres3, true),
            Option(R.drawable.zem4, false),
            Option(R.drawable.pres4, true)
        )),
        Question("Кто из них относится к земноводным?", listOf(
            Option(R.drawable.zem5, true),
            Option(R.drawable.pres5, false),
            Option(R.drawable.pres6, false),
            Option(R.drawable.zem6, true)
        )),
        Question("Выбери всех пресмыкающихся:", listOf(
            Option(R.drawable.pres7, true),
            Option(R.drawable.pres8, true),
            Option(R.drawable.zem7, false),
            Option(R.drawable.zem8, false)
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
                title = { Text("Тест: Кто есть кто?", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { onBackPressed() }) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_back_arrow),
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
                                    if (isCorrect) correctCount++
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
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .background(backgroundColor)
                            ) {
                                Image(
                                    painter = androidx.compose.ui.res.painterResource(id = option.imageRes),
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
                // Сохраняем прогресс в Firebase
                LaunchedEffect(Unit) {
                    dbRef.child("quiz1_done").setValue(true)
                }

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Тест завершён!", fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Ты ответил(а) правильно на $correctCount из ${questions.size} вопросов.")
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(onClick = {
                        context.startActivity(Intent(context, TasksActivity::class.java))
                    }) { Text("Вернуться к тестам") }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(onClick = {
                        context.startActivity(Intent(context, MainActivity::class.java))
                    }) { Text("Главное меню") }
                }
            }
        }
    }
}
