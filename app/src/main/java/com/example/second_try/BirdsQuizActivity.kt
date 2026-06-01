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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.second_try.ui.components.AppTopBar
import com.example.second_try.ui.theme.Second_tryTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class BirdsQuizActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val quizPart = intent.getIntExtra("quiz_part", 1).coerceIn(1, 2)

        setContent {
            Second_tryTheme {
                BirdsQuizScreen(
                    quizPart = quizPart,
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

data class BirdsOption(
    val id: String,
    val text: String,
    val isCorrect: Boolean
)

data class BirdsQuestion(
    val id: String,
    val imageResName: String,
    val text: String,
    val options: List<BirdsOption>
)

@Composable
fun BirdsQuizScreen(
    quizPart: Int,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser

    if (user == null) {
        Text(
            text = "Чтобы проходить викторины, нужно войти в аккаунт.",
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    val dbRef = remember(user.uid) {
        FirebaseDatabase.getInstance(
            "https://mental-health-72105-default-rtdb.europe-west1.firebasedatabase.app"
        ).getReference("Users").child(user.uid).child("quiz_progress")
    }

    val questions = remember(quizPart) {
        val all = parseBirdsQuiz(context)
        if (quizPart == 1) all.take(22) else all.drop(22)
    }

    val doneKey = remember(quizPart) {
        if (quizPart == 1) "birds_quiz_1_done" else "birds_quiz_2_done"
    }

    val perfectKey = remember(quizPart) {
        if (quizPart == 1) "perfect_birds_quiz_1" else "perfect_birds_quiz_2"
    }

    val halfRewardedKey = remember(quizPart) {
        if (quizPart == 1) "birds_quiz_1_half_rewarded" else "birds_quiz_2_half_rewarded"
    }

    var halfRewarded by rememberSaveable(quizPart) { mutableStateOf(false) }

    var currentIndex by rememberSaveable(quizPart) { mutableStateOf(0) }
    var selectedAnswers by rememberSaveable(quizPart) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var submittedQuestionIds by rememberSaveable(quizPart) { mutableStateOf<Set<String>>(emptySet()) }
    var isQuizFinished by rememberSaveable(quizPart) { mutableStateOf(false) }

    val totalQuestions = questions.size

    val correctCount = remember(selectedAnswers, submittedQuestionIds, questions) {
        questions.count { question ->
            val selectedOptionId = selectedAnswers[question.id]
            submittedQuestionIds.contains(question.id) &&
                    question.options.any { option ->
                        option.id == selectedOptionId && option.isCorrect
                    }
        }
    }

    LaunchedEffect(isQuizFinished) {
        if (isQuizFinished) {
            dbRef.child(doneKey).setValue(true)

            if (correctCount == totalQuestions) {
                dbRef.child(perfectKey).setValue(true)
            }

            if (correctCount * 2 >= totalQuestions) {
                dbRef.child(halfRewardedKey).get().addOnSuccessListener { snap ->
                    if (snap.getValue(Boolean::class.java) != true) {
                        halfRewarded = true
                        dbRef.child(halfRewardedKey).setValue(true)
                        val conesRef = dbRef.parent!!.child("cones")
                        conesRef.get().addOnSuccessListener { conesSnap ->
                            val cur = conesSnap.getValue(Int::class.java) ?: 0
                            conesRef.setValue(cur + 75)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Викторина: Птицы $quizPart",
                onBack = onBackPressed
            )
        }
    ) { padding ->
        if (isQuizFinished) {
            BirdsQuizResultScreen(
                modifier = Modifier.padding(padding),
                quizPart = quizPart,
                correctCount = correctCount,
                totalQuestions = totalQuestions,
                halfRewarded = halfRewarded,
                onBackToTasks = {
                    context.startActivity(Intent(context, TasksActivity::class.java))
                },
                onBackToMain = {
                    context.startActivity(Intent(context, MainActivity::class.java))
                }
            )
        } else {
            val question = questions[currentIndex]
            val selectedOptionId = selectedAnswers[question.id]
            val isAnswerSubmitted = submittedQuestionIds.contains(question.id)
            val isLastQuestion = currentIndex == questions.lastIndex

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = "Вопрос ${currentIndex + 1} из $totalQuestions",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(12.dp))

                BirdsQuestionImage(
                    imageResName = question.imageResName,
                    contentDescription = question.text
                )

                Spacer(modifier = Modifier.height(16.dp))

                BirdsQuestionStepCard(
                    number = currentIndex + 1,
                    question = question,
                    selectedOptionId = selectedOptionId,
                    isSubmitted = isAnswerSubmitted,
                    onSelect = { optionId ->
                        if (!isAnswerSubmitted) {
                            selectedAnswers = selectedAnswers + (question.id to optionId)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!isAnswerSubmitted) {
                    Button(
                        onClick = {
                            submittedQuestionIds = submittedQuestionIds + question.id
                        },
                        enabled = selectedOptionId != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Подтвердить")
                    }

                    if (selectedOptionId == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Сначала выбери один вариант ответа.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    BirdsAnswerResultText(
                        question = question,
                        selectedOptionId = selectedOptionId
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (isLastQuestion) {
                                isQuizFinished = true
                            } else {
                                currentIndex += 1
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isLastQuestion) "Завершить" else "Следующий вопрос")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun BirdsQuestionImage(
    imageResName: String,
    contentDescription: String
) {
    val context = LocalContext.current
    val resId = remember(imageResName) {
        context.resources.getIdentifier(
            imageResName,
            "drawable",
            context.packageName
        )
    }

    if (resId != 0) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Фото не найдено: $imageResName",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun BirdsQuestionStepCard(
    number: Int,
    question: BirdsQuestion,
    selectedOptionId: String?,
    isSubmitted: Boolean,
    onSelect: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "$number. ${question.text}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            question.options.forEach { option ->
                val isSelected = selectedOptionId == option.id

                val rowBg = when {
                    !isSubmitted && isSelected -> Color(0xFFEDE7F6)
                    !isSubmitted -> Color.Transparent
                    option.isCorrect -> Color(0xFFC8E6C9)
                    isSelected && !option.isCorrect -> Color(0xFFFFCDD2)
                    else -> Color.Transparent
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(rowBg, RoundedCornerShape(10.dp))
                        .clickable(enabled = !isSubmitted) {
                            onSelect(option.id)
                        }
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = if (isSubmitted) {
                            null
                        } else {
                            { onSelect(option.id) }
                        }
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = option.text,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BirdsAnswerResultText(
    question: BirdsQuestion,
    selectedOptionId: String?
) {
    val selectedOption = question.options.firstOrNull { it.id == selectedOptionId }
    val correctOption = question.options.firstOrNull { it.isCorrect }
    val isCorrect = selectedOption?.isCorrect == true

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (isCorrect) "Правильно!" else "Неправильно.",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
            )

            if (!isCorrect && correctOption != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Правильный ответ: ${correctOption.text}",
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun BirdsQuizResultScreen(
    modifier: Modifier = Modifier,
    quizPart: Int,
    correctCount: Int,
    totalQuestions: Int,
    halfRewarded: Boolean,
    onBackToTasks: () -> Unit,
    onBackToMain: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Птицы $quizPart пройдены!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Верных ответов: $correctCount из $totalQuestions",
                    fontSize = 17.sp
                )

                if (halfRewarded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🍓 +75 малинок за прохождение теста!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onBackToTasks,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Вернуться к викторинам")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onBackToMain,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Главное меню")
        }
    }
}

private fun parseBirdsQuiz(context: android.content.Context): List<BirdsQuestion> {
    val text = context.resources.openRawResource(R.raw.quiz_birds)
        .bufferedReader(Charsets.UTF_8)
        .use { it.readText() }

    val questions = mutableListOf<BirdsQuestion>()
    val optionRegex = Regex("""^([а-яёА-ЯЁ])\)\s*(.+)$""")

    var currentImage: String? = null
    var currentQuestion: String? = null
    var currentOptions = mutableListOf<Pair<String, Boolean>>()

    fun flush() {
        val img = currentImage ?: return
        val q = currentQuestion ?: return
        if (currentOptions.isNotEmpty()) {
            val qId = "q${questions.size + 1}"
            questions.add(
                BirdsQuestion(
                    id = qId,
                    imageResName = img,
                    text = q,
                    options = currentOptions.mapIndexed { i, (optText, isCorrect) ->
                        BirdsOption(id = "${qId}_$i", text = optText, isCorrect = isCorrect)
                    }
                )
            )
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

        val optionMatch = optionRegex.find(line)
        if (optionMatch != null) {
            val letter = optionMatch.groupValues[1]
            val rawText = optionMatch.groupValues[2]
            val isCorrect = rawText.contains(" - верно", ignoreCase = true)
            val cleanText = rawText
                .replace(Regex("""\s*-\s*верно\s*$""", RegexOption.IGNORE_CASE), "")
                .trim()
            currentOptions.add(Pair("$letter) $cleanText", isCorrect))
            continue
        }
    }
    flush()
    return questions
}
