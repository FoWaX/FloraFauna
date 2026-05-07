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
    val imageResName: String, // например "ph_0058_1"
    val questionText: String,
    val options: List<PhotoRiddleOption>
)

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

    // Вопросы викторины
    val questions = remember {
        listOf(
            PhotoRiddleQuestion(
                imageResName = "ph_0058_1",
                questionText = "Кто это так потрудился испортить дерево? Зачем?",
                options = listOf(
                    PhotoRiddleOption("Дятел. Своим твердым клювом он долбил, чтобы добыть древесных личинок.", true),
                    PhotoRiddleOption("Лось рогами. Он пытался зимой добыть себе кору для еды", false),
                    PhotoRiddleOption("Лесные мыши. Они начинали рыть для себя норки на зиму в мягкой древесине", false)
                )
            ),
            PhotoRiddleQuestion(
                imageResName = "ph_0059_1",
                questionText = "Что это за отверстия в крутом отвесном берегу? Кто их сделал и зачем?",
                options = listOf(
                    PhotoRiddleOption("Норы птиц. Береговые ласточки роют норы для гнезд.", true),
                    PhotoRiddleOption("Это норы мышей. Обрыв обвалился и их стало видно", false),
                    PhotoRiddleOption("Норы амфибий-чесночниц. Они роют их для зимней спячки", false)
                )
            ),
            PhotoRiddleQuestion(
                imageResName = "ph_0060_1",
                questionText = "Это не ОСА, а … ? Почему у нее такая окраска?",
                options = listOf(
                    PhotoRiddleOption("Муха. Подражает осам, чтобы ее боялись трогать.", true),
                    PhotoRiddleOption("Шершень. Потому что оса и шершень родственники", false),
                    PhotoRiddleOption("Овод-самец. Чтобы привлекать самок", false)
                )
            ),
            PhotoRiddleQuestion(
                imageResName = "ph_0061_1",
                questionText = "Кто повредил дерево? С какой целью?",
                options = listOf(
                    PhotoRiddleOption("Бобры подгрызают деревья зубами. Для постройки плотины на водоеме.", true),
                    PhotoRiddleOption("Дерево срубили туристы. Для костра", false),
                    PhotoRiddleOption("Насекомые-короеды. Их личинки едят древесину", false)
                )
            ),
            PhotoRiddleQuestion(
                imageResName = "ph_0062_1",
                questionText = "Что это за отверстие в обрыве? Кто его сделал и для чего?",
                options = listOf(
                    PhotoRiddleOption("Это нора-гнездо птицы золотистой щурки. Она сама роет его для выведения птенцов.", true),
                    PhotoRiddleOption("Отверстие сделали дети. Чтобы поместить туда «клад»", false),
                    PhotoRiddleOption("Нору вырыла собака. Для того, чтобы посмотреть «что там»", false)
                )
            ),
            PhotoRiddleQuestion(
                imageResName = "ph_0063_1",
                questionText = "Кто это построил? Почему не надо подходить близко?",
                options = listOf(
                    PhotoRiddleOption("Гнездо шершней. Очень опасные своим ядом жалящие насекомые.", true),
                    PhotoRiddleOption("Гнездо белки. Чтобы не беспокоить детенышей", false),
                    PhotoRiddleOption("Гнездо диких пчел. Пчелы могут напасть", false)
                )
            ),
            PhotoRiddleQuestion(
                imageResName = "ph_0064_1",
                questionText = "Назовите этих насекомых. Какая драма разыгралась на цветке?",
                options = listOf(
                    PhotoRiddleOption("Хищный клоп и божья коровка. Один хищник ест другого хищника.", true),
                    PhotoRiddleOption("Два клопа. Соперничают из-за самки", false),
                    PhotoRiddleOption("Жук-нарывник и божья коровка. Хищное насекомое напало на нехищное", false)
                )
            ),
            PhotoRiddleQuestion(
                imageResName = "ph_0065_1",
                questionText = "На этикетке орфографическая и биологическая ошибки",
                options = listOf(
                    PhotoRiddleOption("Надо писать: акарициды; клещи – паукообразные.", true),
                    PhotoRiddleOption("Надо писать: акарациды; преперат против клещей", false),
                    PhotoRiddleOption("Надо писать: окароциды; препарат против насекомых", false)
                )
            ),
            PhotoRiddleQuestion(
                imageResName = "ph_0066_1",
                questionText = "Какие животные наследили? Сколько их было?",
                options = listOf(
                    PhotoRiddleOption("Птицы. Три.", true),
                    PhotoRiddleOption("Лягушки. Две", false),
                    PhotoRiddleOption("Двустворчатые моллюски. Четыре", false)
                )
            ),
            PhotoRiddleQuestion(
                imageResName = "ph_0067_1",
                questionText = "Назовите животное и хищное водное растение",
                options = listOf(
                    PhotoRiddleOption("Тритон и пузырчатка.", true),
                    PhotoRiddleOption("Ящерица и ряска", false),
                    PhotoRiddleOption("Головастик лягушки и росянка", false)
                )
            ),
            PhotoRiddleQuestion(
                imageResName = "ph_0068_1",
                questionText = "Страшный охотник напал на жертву, это…",
                options = listOf(
                    PhotoRiddleOption("Паук доломедес и личинка стрекозы.", true),
                    PhotoRiddleOption("Паук тарантул и сверчок", false),
                    PhotoRiddleOption("Паук крестовик и личинка плавунца", false)
                )
            ),
            PhotoRiddleQuestion(
                imageResName = "ph_0069_1",
                questionText = "Растение это или животное? Опасно ли для человека?",
                options = listOf(
                    PhotoRiddleOption("Животное губка-бодяга. Не опасно.", true),
                    PhotoRiddleOption("Растение зеленая водоросль. Не опасно", false),
                    PhotoRiddleOption("Животное коралл. Не опасно", false)
                )
            )
        )
    }

    var currentQuestionIndex by remember { mutableStateOf(0) }
    var correctCount by remember { mutableStateOf(0) }
    var selectedAnswerIndex by remember { mutableStateOf<Int?>(null) }
    var showAnswer by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }

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
                }

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Викторина завершена!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Верных ответов: $correctCount из ${questions.size}")

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