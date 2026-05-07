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

    val quiz = remember(quizId) { getImageQuizById(quizId) }

    var currentQuestionIndex by remember { mutableStateOf(0) }
    var correctCount by remember { mutableStateOf(0) }
    var selectedAnswerIndex by remember { mutableStateOf<Int?>(null) }
    var showAnswer by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }

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
                }

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Тест завершён!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Правильных ответов: $correctCount из ${quiz.questions.size}")
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

private fun getImageQuizById(id: String): ImageQuizDefinition {
    val all = listOf(
        quizAmphibianSigns(),
        quizReptileSigns(),
        quizWhoIsWho(),
        quizAmphibianNames(),
        quizReptileNames(),
        quizDangerousOrNot()
    )
    return all.firstOrNull { it.id == id } ?: all.first()
}

private fun quizAmphibianSigns() = ImageQuizDefinition(
    id = "amphibian_signs",
    title = "Признаки земноводных",
    doneKey = "amphibian_signs_done",
    perfectKey = "perfect_amphibian_signs",
    questions = listOf(
        ImageQuizQuestion("Какое второе название земноводных?", listOf(
            ImageQuizAnswer(R.drawable.ph_0019_2, true),
            ImageQuizAnswer(R.drawable.ph_0019_3, false)
        )),
        ImageQuizQuestion("У земноводных голая влажная кожа", listOf(
            ImageQuizAnswer(R.drawable.ph_0003_3, true),
            ImageQuizAnswer(R.drawable.ph_0016_4, false),
            ImageQuizAnswer(R.drawable.ph_0011_3, false)
        )),
        ImageQuizQuestion("Выпуклые глаза с веками", listOf(
            ImageQuizAnswer(R.drawable.ph_0003_5, true),
            ImageQuizAnswer(R.drawable.ph_0018_4, false),
            ImageQuizAnswer(R.drawable.ph_0012_2, false)
        )),
        ImageQuizQuestion("На задних лапах перепонки", listOf(
            ImageQuizAnswer(R.drawable.ph_0003_4, true),
            ImageQuizAnswer(R.drawable.ph_0019_1, false),
            ImageQuizAnswer(R.drawable.ph_0015_2, false)
        )),
        ImageQuizQuestion("На пальцах нет когтей", listOf(
            ImageQuizAnswer(R.drawable.ph_0019_1, true),
            ImageQuizAnswer(R.drawable.ph_0016_4, false),
            ImageQuizAnswer(R.drawable.ph_0014_1, false)
        )),
        ImageQuizQuestion("У большинства амфибий нет хвоста", listOf(
            ImageQuizAnswer(R.drawable.ph_0003_2, true),
            ImageQuizAnswer(R.drawable.ph_0005_1, false),
            ImageQuizAnswer(R.drawable.ph_0016_2, false)
        )),
        ImageQuizQuestion("Тритон – земноводное (тоже голая кожа)", listOf(
            ImageQuizAnswer(R.drawable.ph_0005_3, true),
            ImageQuizAnswer(R.drawable.ph_0015_1, false),
            ImageQuizAnswer(R.drawable.ph_0017_2, false)
        )),
        ImageQuizQuestion("У самцов лягушек резонаторы, чтобы громко квакать", listOf(
            ImageQuizAnswer(R.drawable.ph_0018_1, true),
            ImageQuizAnswer(R.drawable.ph_0007_4, false),
            ImageQuizAnswer(R.drawable.ph_0018_4, false)
        )),
        ImageQuizQuestion("Круглое слуховое отверстие закрыто", listOf(
            ImageQuizAnswer(R.drawable.ph_0018_2, true),
            ImageQuizAnswer(R.drawable.ph_0018_5, false)
        ))
    )
)

private fun quizReptileSigns() = ImageQuizDefinition(
    id = "reptile_signs",
    title = "Признаки пресмыкающихся",
    doneKey = "reptile_signs_done",
    perfectKey = "perfect_reptile_signs",
    questions = listOf(
        ImageQuizQuestion("Как еще называют пресмыкающихся?", listOf(
            ImageQuizAnswer(R.drawable.ph_0019_3, true),
            ImageQuizAnswer(R.drawable.ph_0019_2, false)
        )),
        ImageQuizQuestion("Кожа рептилий покрыта роговыми чешуями", listOf(
            ImageQuizAnswer(R.drawable.ph_0018_6, true),
            ImageQuizAnswer(R.drawable.ph_0018_9, false),
            ImageQuizAnswer(R.drawable.ph_0004_2, false)
        )),
        ImageQuizQuestion("У ящериц подвижные веки", listOf(
            ImageQuizAnswer(R.drawable.ph_0018_4, true),
            ImageQuizAnswer(R.drawable.ph_0018_3, false),
            ImageQuizAnswer(R.drawable.ph_0003_5, false)
        )),
        ImageQuizQuestion("У змей немигающие глаза", listOf(
            ImageQuizAnswer(R.drawable.ph_0018_3, true),
            ImageQuizAnswer(R.drawable.ph_0018_5, false),
            ImageQuizAnswer(R.drawable.ph_0016_4, false)
        )),
        ImageQuizQuestion("На пальцах когти", listOf(
            ImageQuizAnswer(R.drawable.ph_0016_4, true),
            ImageQuizAnswer(R.drawable.ph_0019_1, false),
            ImageQuizAnswer(R.drawable.ph_0003_4, false)
        )),
        ImageQuizQuestion("У всех рептилий есть хвост", listOf(
            ImageQuizAnswer(R.drawable.ph_0016_2, true),
            ImageQuizAnswer(R.drawable.ph_0005_2, false)
        )),
        ImageQuizQuestion("Панцирь только у черепах", listOf(
            ImageQuizAnswer(R.drawable.ph_0014_2, true),
            ImageQuizAnswer(R.drawable.ph_0006_2, false),
            ImageQuizAnswer(R.drawable.ph_0017_3, false)
        )),
        ImageQuizQuestion("Слуховое отверстие открыто", listOf(
            ImageQuizAnswer(R.drawable.ph_0018_5, true),
            ImageQuizAnswer(R.drawable.ph_0018_2, false)
        )),
        ImageQuizQuestion("У рептилий нет резонаторов", listOf(
            ImageQuizAnswer(R.drawable.ph_0012_2, true),
            ImageQuizAnswer(R.drawable.ph_0018_1, false)
        ))
    )
)

private fun quizWhoIsWho() = ImageQuizDefinition(
    id = "who_is_who",
    title = "Кто есть кто?",
    doneKey = "who_is_who_done",
    perfectKey = "perfect_who_is_who",
    questions = listOf(
        ImageQuizQuestion("Найди земноводное", listOf(
            ImageQuizAnswer(R.drawable.ph_0005_1, true),
            ImageQuizAnswer(R.drawable.ph_0017_1, false),
            ImageQuizAnswer(R.drawable.ph_0015_2, false)
        )),
        ImageQuizQuestion("Кто из них рептилия?", listOf(
            ImageQuizAnswer(R.drawable.ph_0016_3, true),
            ImageQuizAnswer(R.drawable.ph_0005_3, false),
            ImageQuizAnswer(R.drawable.ph_0005_1, false)
        )),
        ImageQuizQuestion("Точно узнаешь пресмыкающееся", listOf(
            ImageQuizAnswer(R.drawable.ph_0014_1, true),
            ImageQuizAnswer(R.drawable.ph_0003_1, false),
            ImageQuizAnswer(R.drawable.ph_0001_2, false)
        )),
        ImageQuizQuestion("Из них одна – ящерица, а не змея", listOf(
            ImageQuizAnswer(R.drawable.ph_0007_3, true),
            ImageQuizAnswer(R.drawable.ph_0013_1, false),
            ImageQuizAnswer(R.drawable.ph_0008_1, false)
        )),
        ImageQuizQuestion("Где голова земноводного?", listOf(
            ImageQuizAnswer(R.drawable.ph_0003_5, true),
            ImageQuizAnswer(R.drawable.ph_0018_3, false),
            ImageQuizAnswer(R.drawable.ph_0012_2, false)
        )),
        ImageQuizQuestion("Кто амфибия?", listOf(
            ImageQuizAnswer(R.drawable.ph_0005_2, true),
            ImageQuizAnswer(R.drawable.ph_0012_1, false),
            ImageQuizAnswer(R.drawable.ph_0018_6, false)
        )),
        ImageQuizQuestion("Легко узнать амфибию", listOf(
            ImageQuizAnswer(R.drawable.ph_0018_9, true),
            ImageQuizAnswer(R.drawable.ph_0007_4, false),
            ImageQuizAnswer(R.drawable.ph_0016_4, false)
        )),
        ImageQuizQuestion("Кто пресмыкающееся?", listOf(
            ImageQuizAnswer(R.drawable.ph_0017_2, true),
            ImageQuizAnswer(R.drawable.ph_0005_1, false),
            ImageQuizAnswer(R.drawable.ph_0002_2, false)
        )),
        ImageQuizQuestion("Где лапа рептилии?", listOf(
            ImageQuizAnswer(R.drawable.ph_0016_4, true),
            ImageQuizAnswer(R.drawable.ph_0019_1, false),
            ImageQuizAnswer(R.drawable.ph_0003_4, false)
        ))
    )
)

private fun quizAmphibianNames() = ImageQuizDefinition(
    id = "amphibian_names",
    title = "Тренажер названий земноводных",
    doneKey = "amphibian_names_done",
    perfectKey = "perfect_amphibian_names",
    questions = listOf(
        ImageQuizQuestion("Жаба зеленая (выпуклые заушные железы)", listOf(
            ImageQuizAnswer(R.drawable.ph_0001_1, true),
            ImageQuizAnswer(R.drawable.ph_0018_5, false),
            ImageQuizAnswer(R.drawable.ph_0003_2, false)
        )),
        ImageQuizQuestion("Лягушка озерная (самая большая в Саратовской области)", listOf(
            ImageQuizAnswer(R.drawable.ph_0018_7, true),
            ImageQuizAnswer(R.drawable.ph_0006_2, false)
        )),
        ImageQuizQuestion("Лягушка остромордая", listOf(
            ImageQuizAnswer(R.drawable.ph_0004_2, true),
            ImageQuizAnswer(R.drawable.ph_0001_2, false),
            ImageQuizAnswer(R.drawable.ph_0018_9, false)
        )),
        ImageQuizQuestion("У чесночницы обыкновенной красные крапинки", listOf(
            ImageQuizAnswer(R.drawable.ph_0006_1, true),
            ImageQuizAnswer(R.drawable.ph_0002_2, false),
            ImageQuizAnswer(R.drawable.ph_0004_1, false)
        )),
        ImageQuizQuestion("Жерлянка краснобрюхая", listOf(
            ImageQuizAnswer(R.drawable.ph_0018_8, true),
            ImageQuizAnswer(R.drawable.ph_0003_3, false),
            ImageQuizAnswer(R.drawable.ph_0001_1, false)
        )),
        ImageQuizQuestion("Тритон обыкновенный", listOf(
            ImageQuizAnswer(R.drawable.ph_0005_2, true),
            ImageQuizAnswer(R.drawable.ph_0015_2, false),
            ImageQuizAnswer(R.drawable.ph_0015_1, false)
        ))
    )
)

private fun quizReptileNames() = ImageQuizDefinition(
    id = "reptile_names",
    title = "Тренажер названий пресмыкающихся",
    doneKey = "reptile_names_done",
    perfectKey = "perfect_reptile_names",
    questions = listOf(
        ImageQuizQuestion("Веретеница ломкая (безногая ящерица)", listOf(
            ImageQuizAnswer(R.drawable.ph_0007_2, true),
            ImageQuizAnswer(R.drawable.ph_0016_1, false),
            ImageQuizAnswer(R.drawable.ph_0009_1, false)
        )),
        ImageQuizQuestion("Гадюка Никольского (вся черная)", listOf(
            ImageQuizAnswer(R.drawable.ph_0008_4, true),
            ImageQuizAnswer(R.drawable.ph_0013_2, false),
            ImageQuizAnswer(R.drawable.ph_0013_1, false)
        )),
        ImageQuizQuestion("Гадюка степная (темный зигзаг на спине)", listOf(
            ImageQuizAnswer(R.drawable.ph_0009_2, true),
            ImageQuizAnswer(R.drawable.ph_0012_1, false),
            ImageQuizAnswer(R.drawable.ph_0008_3, false)
        )),
        ImageQuizQuestion("Медянка обыкновенная (медный цвет на брюхе)", listOf(
            ImageQuizAnswer(R.drawable.ph_0010_1, true),
            ImageQuizAnswer(R.drawable.ph_0015_2, false),
            ImageQuizAnswer(R.drawable.ph_0017_1, false)
        )),
        ImageQuizQuestion("Полоз узорчатый", listOf(
            ImageQuizAnswer(R.drawable.ph_0011_1, true),
            ImageQuizAnswer(R.drawable.ph_0009_1, false),
            ImageQuizAnswer(R.drawable.ph_0007_1, false)
        )),
        ImageQuizQuestion("Уж водяной", listOf(
            ImageQuizAnswer(R.drawable.ph_0012_1, true),
            ImageQuizAnswer(R.drawable.ph_0013_1, false),
            ImageQuizAnswer(R.drawable.ph_0018_3, false)
        )),
        ImageQuizQuestion("Уж обыкновенный", listOf(
            ImageQuizAnswer(R.drawable.ph_0013_2, true),
            ImageQuizAnswer(R.drawable.ph_0008_1, false),
            ImageQuizAnswer(R.drawable.ph_0008_3, false)
        )),
        ImageQuizQuestion("Черепаха болотная", listOf(
            ImageQuizAnswer(R.drawable.ph_0014_1, true),
            ImageQuizAnswer(R.drawable.ph_0018_3, false),
            ImageQuizAnswer(R.drawable.ph_0012_2, false)
        )),
        ImageQuizQuestion("Ящерица прыткая (самец)", listOf(
            ImageQuizAnswer(R.drawable.ph_0016_1, true),
            ImageQuizAnswer(R.drawable.ph_0016_3, false),
            ImageQuizAnswer(R.drawable.ph_0017_2, false)
        )),
        ImageQuizQuestion("Ящерица прыткая (самка)", listOf(
            ImageQuizAnswer(R.drawable.ph_0016_3, true),
            ImageQuizAnswer(R.drawable.ph_0016_2, false),
            ImageQuizAnswer(R.drawable.ph_0017_3, false)
        )),
        ImageQuizQuestion("Ящерица живородящая (Красная книга)", listOf(
            ImageQuizAnswer(R.drawable.ph_0015_2, true),
            ImageQuizAnswer(R.drawable.ph_0016_1, false)
        )),
        ImageQuizQuestion("Ящурка разноцветная", listOf(
            ImageQuizAnswer(R.drawable.ph_0017_1, true),
            ImageQuizAnswer(R.drawable.ph_0015_1, false),
            ImageQuizAnswer(R.drawable.ph_0016_2, false)
        ))
    )
)

private fun quizDangerousOrNot() = ImageQuizDefinition(
    id = "dangerous_or_not",
    title = "Опасные и безобидные",
    doneKey = "dangerous_or_not_done",
    perfectKey = "perfect_dangerous_or_not",
    questions = listOf(
        ImageQuizQuestion("Какая из этих змей ядовитая?", listOf(
            ImageQuizAnswer(R.drawable.ph_0008_3, true), // было «-верно-верно», это просто true
            ImageQuizAnswer(R.drawable.ph_0012_1, false),
            ImageQuizAnswer(R.drawable.ph_0013_1, false)
        )),
        ImageQuizQuestion("Водяного ужа часто убивают как ядовитую змею", listOf(
            ImageQuizAnswer(R.drawable.ph_0012_2, true),
            ImageQuizAnswer(R.drawable.ph_0020_7, false),
            ImageQuizAnswer(R.drawable.ph_0013_2, false)
        )),
        ImageQuizQuestion("Эта окраска сразу подскажет, что змея не опасна", listOf(
            ImageQuizAnswer(R.drawable.ph_0013_3, true),
            ImageQuizAnswer(R.drawable.ph_0009_3, false),
            ImageQuizAnswer(R.drawable.ph_0018_3, false)
        )),
        ImageQuizQuestion("Рядом с какой змеей находится очень опасно?", listOf(
            ImageQuizAnswer(R.drawable.ph_0009_1, true),
            ImageQuizAnswer(R.drawable.ph_0018_6, false),
            ImageQuizAnswer(R.drawable.ph_0010_3, false),
            ImageQuizAnswer(R.drawable.ph_0020_2, false)
        )),
        ImageQuizQuestion("Из амфибий Саратовской области эта наиболее опасна", listOf(
            ImageQuizAnswer(R.drawable.ph_0002_2, true),
            ImageQuizAnswer(R.drawable.ph_0021_2, false),
            ImageQuizAnswer(R.drawable.ph_0004_2, false)
        )),
        ImageQuizQuestion("Кожа жаб выделяет ядовитую жидкость", listOf(
            ImageQuizAnswer(R.drawable.ph_0001_1, true),
            ImageQuizAnswer(R.drawable.ph_0006_2, false),
            ImageQuizAnswer(R.drawable.ph_0021_1, false)
        )),
        ImageQuizQuestion("С помощью этого самцы громко «поют»", listOf(
            ImageQuizAnswer(R.drawable.ph_0018_1, true),
            ImageQuizAnswer(R.drawable.ph_0021_4, false),
            ImageQuizAnswer(R.drawable.ph_0018_5, false),
            ImageQuizAnswer(R.drawable.ph_0003_4, false),
            ImageQuizAnswer(R.drawable.ph_0021_5, false)
        )),
        ImageQuizQuestion("Этих безногих ящериц убивают, принимая за змею", listOf(
            ImageQuizAnswer(R.drawable.ph_0007_1, true),
            ImageQuizAnswer(R.drawable.ph_0012_1, false),
            ImageQuizAnswer(R.drawable.ph_0009_2, false),
            ImageQuizAnswer(R.drawable.ph_0020_3, false)
        )),
        ImageQuizQuestion("Ящерицы могут укусить, а ЭТО – вообще не ящерица", listOf(
            ImageQuizAnswer(R.drawable.ph_0005_1, true),
            ImageQuizAnswer(R.drawable.ph_0017_3, false),
            ImageQuizAnswer(R.drawable.ph_0015_1, false),
            ImageQuizAnswer(R.drawable.ph_0020_4, false)
        )),
        ImageQuizQuestion("Такая окраска предупреждает об опасности", listOf(
            ImageQuizAnswer(R.drawable.ph_0009_3, true),
            ImageQuizAnswer(R.drawable.ph_0011_1, false),
            ImageQuizAnswer(R.drawable.ph_0007_2, false),
            ImageQuizAnswer(R.drawable.ph_0021_5, false)
        ))
    )
)