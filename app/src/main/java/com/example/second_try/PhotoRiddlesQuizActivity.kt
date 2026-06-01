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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.second_try.ui.theme.Second_tryTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PhotoRiddleOption(
    val text: String,
    val isCorrect: Boolean
)

data class PhotoRiddleQuestion(
    val imageResName: String,
    val questionText: String,
    val options: List<PhotoRiddleOption>
)

private fun parsePhotoRiddles(context: android.content.Context): List<PhotoRiddleQuestion> {
    val text = context.resources.openRawResource(R.raw.quiz_photo_riddles)
        .bufferedReader(Charsets.UTF_8)
        .use { it.readText() }

    val questions = mutableListOf<PhotoRiddleQuestion>()
    var currentImage: String? = null
    var currentQuestion: String? = null
    var currentOptions = mutableListOf<PhotoRiddleOption>()

    fun flush() {
        val img = currentImage ?: return
        val q = currentQuestion ?: return
        if (currentOptions.isNotEmpty()) {
            questions.add(PhotoRiddleQuestion(img, q, currentOptions.toList()))
        }
        currentImage = null; currentQuestion = null; currentOptions = mutableListOf()
    }

    for (rawLine in text.lines()) {
        val line = rawLine.trim()
        if (line.isBlank()) continue

        if (line.matches(Regex("ph_\\d+_\\d+"))) {
            flush()
            currentImage = line
            continue
        }

        if (line.startsWith("? ")) {
            currentQuestion = line.removePrefix("? ").trim()
            continue
        }

        if (line.startsWith("+ ") || line.startsWith("- ")) {
            currentOptions.add(PhotoRiddleOption(line.drop(2).trim(), line.startsWith("+ ")))
            continue
        }
    }
    flush()
    return questions
}

class PhotoRiddlesQuizActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Second_tryTheme {
                PhotoRiddlesQuizScreen(onBackPressed = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoRiddlesQuizScreen(onBackPressed: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val user = FirebaseAuth.getInstance().currentUser
    val dbRef = FirebaseDatabase.getInstance(
        "https://mental-health-72105-default-rtdb.europe-west1.firebasedatabase.app"
    ).getReference("Users").child(user!!.uid).child("quiz_progress")

    val questions = remember { parsePhotoRiddles(context) }

    var currentQuestionIndex by remember { mutableStateOf(0) }
    var correctCount by remember { mutableStateOf(0) }
    var selectedAnswerIndex by remember { mutableStateOf<Int?>(null) }
    var showAnswer by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    var halfRewarded by remember { mutableStateOf(false) }

    // Для каждого вопроса отдельно перемешиваем варианты (но стабильно в рамках отображения)
    val currentQuestion = questions.getOrNull(currentQuestionIndex)
    val shuffledOptions = remember(currentQuestionIndex) {
        currentQuestion?.options?.shuffled() ?: emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Фотозагадки", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back_arrow),
                            contentDescription = "Назад",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (!isFinished && currentQuestion != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Вопрос ${currentQuestionIndex + 1} из ${questions.size}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = currentQuestion.questionText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val imageResId = context.resources.getIdentifier(
                        currentQuestion.imageResName,
                        "drawable",
                        context.packageName
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F2))
                    ) {
                        if (imageResId != 0) {
                            Image(
                                painter = painterResource(id = imageResId),
                                contentDescription = currentQuestion.questionText,
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Изображение не найдено: ${currentQuestion.imageResName}")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    shuffledOptions.forEachIndexed { index, option ->
                        val isSelected = selectedAnswerIndex == index

                        val answerBgColor = when {
                            !showAnswer -> Color.White
                            isSelected && option.isCorrect -> Color(0xFFB9F6CA) // зеленый
                            isSelected && !option.isCorrect -> Color(0xFFFFCDD2) // красный
                            else -> Color.White
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable(enabled = !showAnswer) {
                                    selectedAnswerIndex = index
                                    showAnswer = true

                                    if (option.isCorrect) {
                                        correctCount++
                                    }

                                    scope.launch {
                                        delay(1000)

                                        if (currentQuestionIndex + 1 < questions.size) {
                                            currentQuestionIndex++
                                            selectedAnswerIndex = null
                                            showAnswer = false
                                        } else {
                                            isFinished = true
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = answerBgColor)
                        ) {
                            Text(
                                text = option.text,
                                modifier = Modifier.padding(14.dp),
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            } else {
                // Сохраняем прогресс новой викторины
                LaunchedEffect(Unit) {
                    dbRef.child("photo_riddles_done").setValue(true)

                    if (correctCount == questions.size) {
                        dbRef.child("perfect_photo_riddles").setValue(true)
                    }

                    if (correctCount * 2 >= questions.size) {
                        dbRef.child("photo_riddles_half_rewarded").get().addOnSuccessListener { snap ->
                            if (snap.getValue(Boolean::class.java) != true) {
                                halfRewarded = true
                                dbRef.child("photo_riddles_half_rewarded").setValue(true)
                                val conesRef = dbRef.parent!!.child("cones")
                                conesRef.get().addOnSuccessListener { conesSnap ->
                                    val cur = conesSnap.getValue(Int::class.java) ?: 0
                                    conesRef.setValue(cur + 75)
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Викторина завершена!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Верных ответов: $correctCount из ${questions.size}")
                    if (halfRewarded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "🍓 +75 малинок за прохождение теста!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(onClick = {
                        context.startActivity(Intent(context, TasksActivity::class.java))
                    }) {
                        Text("Вернуться к викторинам")
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