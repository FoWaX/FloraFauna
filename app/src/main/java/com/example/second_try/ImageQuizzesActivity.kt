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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ImageQuizzesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val quizId = intent.getStringExtra("quiz_id") ?: "amphibian_signs"

        setContent {
            Second_tryTheme {
                ImageQuizScreen(
                    quizId = quizId,
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

// ----------------------- МОДЕЛИ -----------------------

data class ImageQuizAnswer(
    val imageRes: Int,
    val isCorrect: Boolean
)

data class ImageQuizQuestion(
    val text: String,
    val answers: List<ImageQuizAnswer>
)

data class ImageQuizDefinition(
    val id: String,
    val title: String,
    val doneKey: String,
    val perfectKey: String,
    val halfRewardedKey: String,
    val questions: List<ImageQuizQuestion>
)

// ----------------------- UI -----------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageQuizScreen(
    quizId: String,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = FirebaseAuth.getInstance().currentUser ?: return

    val dbRef = FirebaseDatabase.getInstance(
        "https://mental-health-72105-default-rtdb.europe-west1.firebasedatabase.app"
    ).getReference("Users").child(user.uid).child("quiz_progress")

    val quiz = remember(quizId) { getImageQuizById(context, quizId) }

    var currentQuestionIndex by remember { mutableStateOf(0) }
    var correctCount by remember { mutableStateOf(0) }
    var selectedAnswerIndex by remember { mutableStateOf<Int?>(null) }
    var showAnswer by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    var halfRewarded by remember { mutableStateOf(false) }

    // ВАЖНО: варианты одного вопроса перемешиваем и запоминаем до перехода к следующему
    var shuffledAnswers by remember(currentQuestionIndex, quiz.id) {
        mutableStateOf(
            quiz.questions.getOrNull(currentQuestionIndex)?.answers?.shuffled().orEmpty()
        )
    }

    val question = quiz.questions.getOrNull(currentQuestionIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(quiz.title, color = Color.White) },
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
            if (!isFinished && question != null) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Вопрос ${currentQuestionIndex + 1} из ${quiz.questions.size}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = question.text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    shuffledAnswers.forEachIndexed { index, answer ->
                        val wasSelected = selectedAnswerIndex == index

                        val backgroundColor = when {
                            !showAnswer -> Color.Transparent
                            wasSelected && answer.isCorrect -> Color(0xFF00C853) // зелёный
                            wasSelected && !answer.isCorrect -> Color(0xFFD50000) // красный
                            else -> Color.Transparent // правильный отдельно НЕ показываем
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable(enabled = !showAnswer) {
                                    selectedAnswerIndex = index
                                    showAnswer = true

                                    if (answer.isCorrect) {
                                        correctCount++
                                    }

                                    scope.launch {
                                        delay(1000)

                                        if (currentQuestionIndex + 1 < quiz.questions.size) {
                                            currentQuestionIndex++
                                            selectedAnswerIndex = null
                                            showAnswer = false

                                            // перемешиваем ответы уже для следующего вопроса
                                            shuffledAnswers = quiz.questions[currentQuestionIndex].answers.shuffled()
                                        } else {
                                            isFinished = true
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .background(backgroundColor)
                            ) {
                                Image(
                                    painter = painterResource(id = answer.imageRes),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // сохраняем прогресс
                LaunchedEffect(Unit) {
                    dbRef.child(quiz.doneKey).setValue(true)

                    if (correctCount == quiz.questions.size) {
                        dbRef.child(quiz.perfectKey).setValue(true)
                    }

                    if (correctCount * 2 >= quiz.questions.size) {
                        dbRef.child(quiz.halfRewardedKey).get().addOnSuccessListener { snap ->
                            if (snap.getValue(Boolean::class.java) != true) {
                                halfRewarded = true
                                dbRef.child(quiz.halfRewardedKey).setValue(true)
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
                    Text("Тест завершён!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Правильных ответов: $correctCount из ${quiz.questions.size}")
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

// ----------------------- ДАННЫЕ ВИКТОРИН -----------------------

private data class ImageQuizRaw(
    val id: String,
    val title: String,
    val doneKey: String,
    val perfectKey: String,
    val halfRewardedKey: String,
    val questions: List<ImageQuizQuestionRaw>
)

private data class ImageQuizQuestionRaw(
    val text: String,
    val answers: List<ImageQuizAnswerRaw>
)

private data class ImageQuizAnswerRaw(
    val imageResName: String,
    val isCorrect: Boolean
)

private fun parseImageQuizzes(context: android.content.Context): List<ImageQuizRaw> {
    val text = context.resources.openRawResource(R.raw.quiz_image)
        .bufferedReader(Charsets.UTF_8)
        .use { it.readText() }

    val headerRegex = Regex("""^===\s*(.+?)\s*\|\s*(.+?)\s*\|\s*(.+?)\s*\|\s*(.+?)\s*\|\s*(.+?)\s*===$""")
    val quizzes = mutableListOf<ImageQuizRaw>()

    var currentId: String? = null
    var currentTitle: String? = null
    var currentDoneKey: String? = null
    var currentPerfectKey: String? = null
    var currentHalfKey: String? = null
    var currentQuestions = mutableListOf<ImageQuizQuestionRaw>()
    var currentQuestionText: String? = null
    var currentAnswers = mutableListOf<ImageQuizAnswerRaw>()

    fun flushQuestion() {
        val qt = currentQuestionText ?: return
        if (currentAnswers.isNotEmpty()) {
            currentQuestions.add(ImageQuizQuestionRaw(qt, currentAnswers.toList()))
        }
        currentQuestionText = null
        currentAnswers = mutableListOf()
    }

    fun flushQuiz() {
        flushQuestion()
        val id = currentId ?: return
        if (currentQuestions.isNotEmpty()) {
            quizzes.add(ImageQuizRaw(
                id, currentTitle!!, currentDoneKey!!, currentPerfectKey!!, currentHalfKey!!,
                currentQuestions.toList()
            ))
        }
        currentId = null; currentTitle = null; currentDoneKey = null
        currentPerfectKey = null; currentHalfKey = null
        currentQuestions = mutableListOf()
    }

    for (rawLine in text.lines()) {
        val line = rawLine.trim()
        if (line.isBlank()) continue

        val headerMatch = headerRegex.find(line)
        if (headerMatch != null) {
            flushQuiz()
            currentId = headerMatch.groupValues[1].trim()
            currentTitle = headerMatch.groupValues[2].trim()
            currentDoneKey = headerMatch.groupValues[3].trim()
            currentPerfectKey = headerMatch.groupValues[4].trim()
            currentHalfKey = headerMatch.groupValues[5].trim()
            continue
        }

        if (line.startsWith("? ")) {
            flushQuestion()
            currentQuestionText = line.removePrefix("? ").trim()
            continue
        }

        if (line.startsWith("+ ") || line.startsWith("- ")) {
            val isCorrect = line.startsWith("+ ")
            currentAnswers.add(ImageQuizAnswerRaw(line.drop(2).trim(), isCorrect))
            continue
        }
    }
    flushQuiz()
    return quizzes
}

private fun getImageQuizById(context: android.content.Context, id: String): ImageQuizDefinition {
    val raw = parseImageQuizzes(context)
    return raw.firstOrNull { it.id == id }?.let { rawQuiz ->
        ImageQuizDefinition(
            id = rawQuiz.id,
            title = rawQuiz.title,
            doneKey = rawQuiz.doneKey,
            perfectKey = rawQuiz.perfectKey,
            halfRewardedKey = rawQuiz.halfRewardedKey,
            questions = rawQuiz.questions.map { rawQ ->
                ImageQuizQuestion(
                    text = rawQ.text,
                    answers = rawQ.answers.map { rawA ->
                        ImageQuizAnswer(
                            imageRes = context.resources.getIdentifier(
                                rawA.imageResName, "drawable", context.packageName
                            ),
                            isCorrect = rawA.isCorrect
                        )
                    }
                )
            }
        )
    } ?: ImageQuizDefinition("", "", "", "", "", emptyList())
}

