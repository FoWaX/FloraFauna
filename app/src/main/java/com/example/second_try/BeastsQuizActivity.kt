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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.cardColors
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
import kotlin.random.Random

class BeastsQuizActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Second_tryTheme {
                BeastsQuizScreen(onBackPressed = { finish() })
            }
        }
    }
}

// -------------------- DATA MODELS --------------------

data class BeastsRawOption(
    val text: String,
    val isCorrect: Boolean
)

data class BeastsRawQuestion(
    val text: String,
    val options: List<BeastsRawOption>
)

data class BeastsRawGroup(
    val animalName: String,
    val imageResName: String,
    val questions: List<BeastsRawQuestion>
)

data class BeastsUiOption(
    val text: String,
    val isCorrect: Boolean
)

data class BeastsUiQuestion(
    val id: String, // уникальный id вопроса
    val text: String,
    val options: List<BeastsUiOption>
)

data class BeastsUiGroup(
    val animalName: String,
    val imageResName: String,
    val imageResId: Int,
    val questions: List<BeastsUiQuestion>
)

// -------------------- ACTUAL SCREEN --------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeastsQuizScreen(onBackPressed: () -> Unit) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val dbRef = FirebaseDatabase.getInstance(
        "https://mental-health-72105-default-rtdb.europe-west1.firebasedatabase.app"
    ).getReference("Users").child(user!!.uid).child("quiz_progress")

    // Загружаем и готовим викторину 1 раз
    val groups = remember {
        parseBeastsQuizFromRaw(context)
            .mapIndexed { groupIndex, rawGroup ->
                val resId = context.resources.getIdentifier(
                    rawGroup.imageResName,
                    "drawable",
                    context.packageName
                )

                BeastsUiGroup(
                    animalName = rawGroup.animalName,
                    imageResName = rawGroup.imageResName,
                    imageResId = resId,
                    questions = rawGroup.questions.mapIndexed { qIndex, rawQuestion ->
                        BeastsUiQuestion(
                            id = "g${groupIndex}_q$qIndex",
                            text = rawQuestion.text,
                            options = rawQuestion.options.shuffled(Random(System.nanoTime())).map {
                                BeastsUiOption(
                                    text = it.text,
                                    isCorrect = it.isCorrect
                                )
                            }
                        )
                    }
                )
            }
    }

    var currentGroupIndex by remember { mutableStateOf(0) }

    // выбранные ответы: key = question.id, value = index выбранного варианта
    val selectedAnswers = remember { mutableStateMapOf<String, Int>() }

    // какие группы уже подтверждены (чтобы показывать подсветку)
    val confirmedGroups = remember { mutableStateMapOf<Int, Boolean>() }

    var showFinalDialog by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }

    val totalQuestions = groups.sumOf { it.questions.size }

    // Подсчёт результата (неотвеченные считаются неверными)
    val correctAnswersCount = remember(selectedAnswers, groups) {
        groups.sumOf { group ->
            group.questions.count { question ->
                val selectedIndex = selectedAnswers[question.id]
                selectedIndex != null && question.options.getOrNull(selectedIndex)?.isCorrect == true
            }
        }
    }

    if (isFinished) {
        // Сохраняем прогресс в Firebase один раз
        LaunchedEffect(Unit) {
            dbRef.child("beasts_quiz_done").setValue(true)
            if (correctAnswersCount == totalQuestions) {
                dbRef.child("perfect_beasts_quiz").setValue(true)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Викторина: Звери", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBackPressed) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Викторина завершена!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("Правильных ответов: $correctAnswersCount из $totalQuestions")
                Spacer(Modifier.height(24.dp))

                Button(onClick = {
                    context.startActivity(Intent(context, TasksActivity::class.java))
                }) {
                    Text("Вернуться к викторинам")
                }

                Spacer(Modifier.height(12.dp))

                Button(onClick = {
                    context.startActivity(Intent(context, MainActivity::class.java))
                }) {
                    Text("Главное меню")
                }
            }
        }

        return
    }

    if (groups.isEmpty()) {
        // Если вдруг raw-файл не распарсился
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Викторина: Звери", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBackPressed) {
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
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Не удалось загрузить вопросы викторины.\nПроверь файл res/raw/beasts_quiz_text.txt")
            }
        }
        return
    }

    val currentGroup = groups[currentGroupIndex]
    val isCurrentGroupConfirmed = confirmedGroups[currentGroupIndex] == true

    // Проверяем, на все ли 3 вопроса текущей группы пользователь выбрал ответ
    val allThreeAnswered = currentGroup.questions.all { q -> selectedAnswers.containsKey(q.id) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Викторина: Звери", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ---------------- Стрелки навигации сверху ----------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentGroupIndex > 0) currentGroupIndex--
                    },
                    enabled = currentGroupIndex > 0
                ) {
                    Text(
                        "◀",
                        fontSize = 22.sp,
                        color = if (currentGroupIndex > 0) Color.Black else Color.Gray
                    )
                }

                Text(
                    text = "Группа ${currentGroupIndex + 1} из ${groups.size}",
                    fontWeight = FontWeight.SemiBold
                )

                IconButton(
                    onClick = {
                        if (currentGroupIndex < groups.lastIndex) currentGroupIndex++
                    },
                    enabled = currentGroupIndex < groups.lastIndex
                ) {
                    Text(
                        "▶",
                        fontSize = 22.sp,
                        color = if (currentGroupIndex < groups.lastIndex) Color.Black else Color.Gray
                    )
                }
            }

            // ---------------- Контент группы ----------------
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Фото животного
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp),
                    colors = cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    if (currentGroup.imageResId != 0) {
                        Image(
                            painter = painterResource(id = currentGroup.imageResId),
                            contentDescription = currentGroup.animalName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Изображение не найдено: ${currentGroup.imageResName}", color = Color.Gray)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = currentGroup.animalName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp))

                currentGroup.questions.forEachIndexed { qIndex, question ->
                    BeastsQuestionBlock(
                        questionNumber = qIndex + 1,
                        question = question,
                        selectedIndex = selectedAnswers[question.id],
                        isConfirmed = isCurrentGroupConfirmed,
                        onSelectOption = { optionIndex ->
                            // После подтверждения текущей группы ответы менять нельзя
                            if (!isCurrentGroupConfirmed) {
                                selectedAnswers[question.id] = optionIndex
                            }
                        }
                    )
                    Spacer(Modifier.height(14.dp))
                }

                Spacer(Modifier.height(12.dp))

                // -------- Кнопки действия --------
                when {
                    !isCurrentGroupConfirmed -> {
                        Button(
                            onClick = {
                                // Подтверждаем группу (включаем подсветку)
                                confirmedGroups[currentGroupIndex] = true

                                // Если это последняя группа — сразу спрашиваем финальное подтверждение
                                if (currentGroupIndex == groups.lastIndex) {
                                    showFinalDialog = true
                                }
                            },
                            enabled = allThreeAnswered,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text("Подтвердить ответы")
                        }

                        if (!allThreeAnswered) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Сначала выбери по одному ответу в каждом из трёх вопросов",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    isCurrentGroupConfirmed && currentGroupIndex < groups.lastIndex -> {
                        Button(
                            onClick = { currentGroupIndex++ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text("Следующая тройка вопросов")
                        }
                    }

                    isCurrentGroupConfirmed && currentGroupIndex == groups.lastIndex -> {
                        Button(
                            onClick = { showFinalDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text("Завершить викторину")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // -------- Финальный диалог подтверждения --------
    if (showFinalDialog) {
        AlertDialog(
            onDismissRequest = { showFinalDialog = false },
            title = { Text("Завершение викторины") },
            text = {
                Text("Ты подтверждаешь ответы на последних вопросах. Если где-то не ответил(а), лучше вернись и проверь.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFinalDialog = false
                        isFinished = true
                    }
                ) {
                    Text("Подтвердить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showFinalDialog = false }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

// -------------------- QUESTION UI BLOCK --------------------

@Composable
private fun BeastsQuestionBlock(
    questionNumber: Int,
    question: BeastsUiQuestion,
    selectedIndex: Int?,
    isConfirmed: Boolean,
    onSelectOption: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = cardColors(containerColor = Color(0xFFFFFBF2))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "$questionNumber. ${question.text}",
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )

            Spacer(Modifier.height(8.dp))

            question.options.forEachIndexed { index, option ->
                val isSelected = selectedIndex == index

                val rowColor = when {
                    !isConfirmed -> Color.Transparent
                    !isSelected -> Color.Transparent
                    option.isCorrect -> Color(0xFFB9F6CA) // зелёный
                    else -> Color(0xFFFFCDD2) // красный
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(rowColor)
                        .selectable(
                            selected = isSelected,
                            onClick = { onSelectOption(index) },
                            enabled = !isConfirmed
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null, // onClick уже на Row
                        enabled = !isConfirmed
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(option.text, fontSize = 14.sp)
                }
            }
        }
    }
}

// -------------------- PARSER --------------------

private fun parseBeastsQuizFromRaw(context: android.content.Context): List<BeastsRawGroup> {
    val text = context.resources.openRawResource(R.raw.beasts_quiz_text)
        .bufferedReader(Charsets.UTF_8)
        .use { it.readText() }

    val lines = text.lines()

    val headerRegex = Regex("""^(.+?)\s+(ph_\d+_\d+)\s*$""", RegexOption.IGNORE_CASE)
    val questionRegex = Regex("""^\d+\.\s*(.+)$""")
    val optionRegex = Regex("""^[A-Za-zА-Яа-яЁё]\)\s*(.+)$""")

    val groups = mutableListOf<BeastsRawGroup>()

    var currentAnimalName: String? = null
    var currentImageResName: String? = null
    var currentQuestions = mutableListOf<BeastsRawQuestion>()

    var currentQuestionText: String? = null
    var currentOptions = mutableListOf<BeastsRawOption>()

    fun flushQuestionIfNeeded() {
        val qText = currentQuestionText
        if (qText != null && currentOptions.isNotEmpty()) {
            currentQuestions.add(
                BeastsRawQuestion(
                    text = qText.trim(),
                    options = currentOptions.toList()
                )
            )
        }
        currentQuestionText = null
        currentOptions = mutableListOf()
    }

    fun flushGroupIfNeeded() {
        flushQuestionIfNeeded()

        val name = currentAnimalName
        val image = currentImageResName
        if (name != null && image != null && currentQuestions.isNotEmpty()) {
            groups.add(
                BeastsRawGroup(
                    animalName = name.trim(),
                    imageResName = image.trim(),
                    questions = currentQuestions.toList()
                )
            )
        }

        currentAnimalName = null
        currentImageResName = null
        currentQuestions = mutableListOf()
    }

    for (rawLine in lines) {
        val line = rawLine.trim()
        if (line.isBlank()) continue

        // 1) Заголовок группы: "Белка ph_0077_1"
        val headerMatch = headerRegex.find(line)
        if (headerMatch != null) {
            // перед новой группой сохраняем старую
            flushGroupIfNeeded()

            currentAnimalName = headerMatch.groupValues[1].trim()
            currentImageResName = headerMatch.groupValues[2].trim()
            continue
        }

        // 2) Вопрос: "1. ..."
        val questionMatch = questionRegex.find(line)
        if (questionMatch != null) {
            // сохраняем предыдущий вопрос
            flushQuestionIfNeeded()

            currentQuestionText = questionMatch.groupValues[1].trim()
            continue
        }

        // 3) Вариант: "а) ..."
        val optionMatch = optionRegex.find(line)
        if (optionMatch != null && currentQuestionText != null) {
            val rawOptionText = optionMatch.groupValues[1].trim()

            val isCorrect = rawOptionText.contains("верно", ignoreCase = true)

            // удаляем пометки "верно" (включая дубли и тире)
            val cleaned = rawOptionText
                .replace(Regex("""\s*[-–—]?\s*верно\b""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\s{2,}"""), " ")
                .trim()

            currentOptions.add(
                BeastsRawOption(
                    text = cleaned,
                    isCorrect = isCorrect
                )
            )
            continue
        }
    }

    // сохранить последнюю группу
    flushGroupIfNeeded()

    // На всякий случай: если в какой-то группе >3 вопросов, режем по 3
    // (у тебя как раз по 3, но защита не помешает)
    val normalized = mutableListOf<BeastsRawGroup>()
    for (group in groups) {
        if (group.questions.size <= 3) {
            normalized.add(group)
        } else {
            // если вдруг текст собрал больше 3 вопросов в блок — разобьём
            val chunks = group.questions.chunked(3)
            chunks.forEachIndexed { idx, chunk ->
                normalized.add(
                    BeastsRawGroup(
                        animalName = if (idx == 0) group.animalName else "${group.animalName} (${idx + 1})",
                        imageResName = group.imageResName,
                        questions = chunk
                    )
                )
            }
        }
    }

    return normalized
}