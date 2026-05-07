package com.example.second_try

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
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

        val quizPart = intent.getIntExtra("quiz_part", 1).coerceIn(1, 2)

        setContent {
            Second_tryTheme {
                BeastsQuizScreen(
                    quizPart = quizPart,
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

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
    val id: String,
    val text: String,
    val options: List<BeastsUiOption>
)

data class BeastsUiGroup(
    val animalName: String,
    val imageResName: String,
    val imageResId: Int,
    val questions: List<BeastsUiQuestion>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeastsQuizScreen(
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

    val groups = remember(quizPart) {
        val allRawGroups = parseBeastsQuizFromRaw(context)
        val middle = (allRawGroups.size + 1) / 2

        val rawGroupsForPart = if (quizPart == 1) {
            allRawGroups.take(middle)
        } else {
            allRawGroups.drop(middle)
        }

        rawGroupsForPart.mapIndexed { groupIndex, rawGroup ->
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
                        id = "part${quizPart}_g${groupIndex}_q$qIndex",
                        text = rawQuestion.text,
                        options = rawQuestion.options
                            .shuffled(Random(System.nanoTime()))
                            .map { rawOption ->
                                BeastsUiOption(
                                    text = rawOption.text,
                                    isCorrect = rawOption.isCorrect
                                )
                            }
                    )
                }
            )
        }
    }

    val doneKey = remember(quizPart) {
        if (quizPart == 1) "beasts_quiz_1_done" else "beasts_quiz_2_done"
    }

    val perfectKey = remember(quizPart) {
        if (quizPart == 1) "perfect_beasts_quiz_1" else "perfect_beasts_quiz_2"
    }

    var currentGroupIndex by remember { mutableStateOf(0) }

    val selectedAnswers: SnapshotStateMap<String, Int> = remember {
        mutableStateMapOf()
    }

    val confirmedGroups: SnapshotStateMap<Int, Boolean> = remember {
        mutableStateMapOf()
    }

    var showFinalDialog by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }

    val totalQuestions = groups.sumOf { it.questions.size }

    val correctAnswersCount by remember {
        derivedStateOf {
            groups.sumOf { group ->
                group.questions.count { question ->
                    val selectedIndex = selectedAnswers[question.id]
                    selectedIndex != null &&
                            question.options.getOrNull(selectedIndex)?.isCorrect == true
                }
            }
        }
    }

    if (isFinished) {
        LaunchedEffect(Unit) {
            dbRef.child(doneKey).setValue(true)

            if (correctAnswersCount == totalQuestions) {
                dbRef.child(perfectKey).setValue(true)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("Викторина: Звери $quizPart", color = Color.White)
                    },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Викторина завершена!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                Text("Правильных ответов: $correctAnswersCount из $totalQuestions")

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        context.startActivity(Intent(context, TasksActivity::class.java))
                    }
                ) {
                    Text("Вернуться к викторинам")
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        context.startActivity(Intent(context, MainActivity::class.java))
                    }
                ) {
                    Text("Главное меню")
                }
            }
        }

        return
    }

    if (groups.isEmpty()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("Викторина: Звери $quizPart", color = Color.White)
                    },
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

    val allThreeAnswered = currentGroup.questions.all { question ->
        selectedAnswers.containsKey(question.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Викторина: Звери $quizPart", color = Color.White)
                },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
                        text = "◀",
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
                        text = "▶",
                        fontSize = 22.sp,
                        color = if (currentGroupIndex < groups.lastIndex) Color.Black else Color.Gray
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
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
                            Text(
                                text = "Изображение не найдено: ${currentGroup.imageResName}",
                                color = Color.Gray
                            )
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
                            if (!isCurrentGroupConfirmed) {
                                selectedAnswers[question.id] = optionIndex
                            }
                        }
                    )

                    Spacer(Modifier.height(14.dp))
                }

                Spacer(Modifier.height(12.dp))

                when {
                    !isCurrentGroupConfirmed -> {
                        Button(
                            onClick = {
                                confirmedGroups[currentGroupIndex] = true

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
                                text = "Сначала выбери по одному ответу в каждом из трёх вопросов",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    isCurrentGroupConfirmed && currentGroupIndex < groups.lastIndex -> {
                        Button(
                            onClick = {
                                currentGroupIndex++
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text("Следующая тройка вопросов")
                        }
                    }

                    isCurrentGroupConfirmed && currentGroupIndex == groups.lastIndex -> {
                        Button(
                            onClick = {
                                showFinalDialog = true
                            },
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

    if (showFinalDialog) {
        AlertDialog(
            onDismissRequest = {
                showFinalDialog = false
            },
            title = {
                Text("Завершение викторины")
            },
            text = {
                Text("Ты подтверждаешь ответы на последних вопросах.\nЕсли где-то не ответил(а), лучше вернись и проверь.")
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
                    onClick = {
                        showFinalDialog = false
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

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
                    option.isCorrect -> Color(0xFFB9F6CA)
                    else -> Color(0xFFFFCDD2)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(rowColor)
                        .selectable(
                            selected = isSelected,
                            onClick = {
                                onSelectOption(index)
                            },
                            enabled = !isConfirmed
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                        enabled = !isConfirmed
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = option.text,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

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

        val headerMatch = headerRegex.find(line)

        if (headerMatch != null) {
            flushGroupIfNeeded()

            currentAnimalName = headerMatch.groupValues[1].trim()
            currentImageResName = headerMatch.groupValues[2].trim()

            continue
        }

        val questionMatch = questionRegex.find(line)

        if (questionMatch != null) {
            flushQuestionIfNeeded()

            currentQuestionText = questionMatch.groupValues[1].trim()

            continue
        }

        val optionMatch = optionRegex.find(line)

        if (optionMatch != null && currentQuestionText != null) {
            val rawOptionText = optionMatch.groupValues[1].trim()
            val isCorrect = rawOptionText.contains("верно", ignoreCase = true)

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

    flushGroupIfNeeded()

    val normalized = mutableListOf<BeastsRawGroup>()

    for (group in groups) {
        if (group.questions.size <= 3) {
            normalized.add(group)
        } else {
            val chunks = group.questions.chunked(3)

            chunks.forEachIndexed { index, chunk ->
                normalized.add(
                    BeastsRawGroup(
                        animalName = if (index == 0) {
                            group.animalName
                        } else {
                            "${group.animalName} (${index + 1})"
                        },
                        imageResName = group.imageResName,
                        questions = chunk
                    )
                )
            }
        }
    }

    return normalized
}
