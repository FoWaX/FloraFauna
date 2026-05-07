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
        buildBirdsQuizQuestions(quizPart)
    }

    val doneKey = remember(quizPart) {
        if (quizPart == 1) "birds_quiz_1_done" else "birds_quiz_2_done"
    }

    val perfectKey = remember(quizPart) {
        if (quizPart == 1) "perfect_birds_quiz_1" else "perfect_birds_quiz_2"
    }

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

private fun buildBirdsQuizQuestions(quizPart: Int): List<BirdsQuestion> {
    val allQuestions = listOf(
        q(
            id = "q1",
            imageResName = "ph_0151_1",
            text = "Размах крыльев у беркута до",
            a = "1 метра" to false,
            b = "2,5 метров" to true,
            c = "3,5 метров" to false
        ),
        q(
            id = "q2",
            imageResName = "ph_0151_1",
            text = "Беркут, орлан-белохвост и орел степной занесены",
            a = "только в Красную книгу РФ" to false,
            b = "только в Красную книгу СО" to false,
            c = "в обе Книги" to true
        ),
        q(
            id = "q3",
            imageResName = "ph_0150_1",
            text = "Численность дрофы сокращается из-за",
            a = "изменения климата" to false,
            b = "строительства городов" to false,
            c = "браконьерства и распашки земель" to true
        ),
        q(
            id = "q4",
            imageResName = "ph_0150_1",
            text = "«Дрофа» в переводе с древнеславянского",
            a = "«Большая птица»" to false,
            b = "«Бегущая птица»" to true,
            c = "«Тяжёлая птица»" to false
        ),
        q(
            id = "q5",
            imageResName = "ph_0152_1",
            text = "Способ охоты филина:",
            a = "долго преследует жертву в полёте" to false,
            b = "атакует из засады, бесшумно пикируя" to true,
            c = "загоняет добычу стаей" to false
        ),
        q(
            id = "q6",
            imageResName = "ph_0153_1",
            text = "Страуса африканского в Саратовской области",
            a = "разводят и выпускают в природу" to false,
            b = "можно увидеть только в цирке" to false,
            c = "содержат на специальных фермах" to true
        ),
        q(
            id = "q7",
            imageResName = "ph_0154_1",
            text = "У журавлей есть интересная особенность поведения, они",
            a = "умеют петь сложные песни" to false,
            b = "часто танцуют, особенно в период размножения" to true,
            c = "строят гнёзда на кустах" to false
        ),
        q(
            id = "q8",
            imageResName = "ph_0154_1",
            text = "Стая журавлей во время перелёта выглядит как",
            a = "шеренга (линия)" to false,
            b = "клин" to true,
            c = "беспорядочная группа" to false
        ),
        q(
            id = "q9",
            imageResName = "ph_0155_1",
            text = "Почему королёк получил такое название?",
            a = "потому что поёт слово «ко-роль»" to false,
            b = "из-за «короны» на голове" to true,
            c = "он самый сильный среди мелких птиц" to false
        ),
        q(
            id = "q10",
            imageResName = "ph_0155_1",
            text = "Королька в Саратовской области можно встретить",
            a = "только в хвойных лесах" to true,
            b = "в любом лесу или парке" to false,
            c = "вблизи водоёмов" to false
        ),
        q(
            id = "q11",
            imageResName = "ph_0156_1",
            text = "Воробьиный сыч отличается от других сов тем, что он",
            a = "не охотится на мышей" to false,
            b = "может охотиться и ночью, и днём" to true,
            c = "строит гнёзда на земле" to false
        ),
        q(
            id = "q12",
            imageResName = "ph_0158_1",
            text = "Вальдшнеп, в отличие от других куликов, живет",
            a = "на открытых болотах" to false,
            b = "на песчаных отмелях рек" to false,
            c = "в густых лесах" to true
        ),
        q(
            id = "q13",
            imageResName = "ph_0158_1",
            text = "Длинный клюв нужен вальдшнепу для",
            a = "отличия самца от самки" to false,
            b = "добычи червей и насекомых из лесной почвы" to true,
            c = "защиты от хищников" to false
        ),
        q(
            id = "q14",
            imageResName = "ph_0159_1",
            text = "Самцу фазана яркая окраска нужна, чтобы",
            a = "отпугивать хищников" to false,
            b = "привлекать самок в брачный период" to true,
            c = "маскироваться летом среди цветов" to false
        ),
        q(
            id = "q15",
            imageResName = "ph_0157_1",
            text = "Соперники фазана по яркости окраски:",
            a = "беркут и орлан-белохвост" to false,
            b = "сизоворонка, щурка и зимородок" to true,
            c = "журавль и дрофа" to false
        ),
        q(
            id = "q16",
            imageResName = "ph_0160_1",
            text = "Серые куропатки осенью и зимой живут",
            a = "поодиночке" to false,
            b = "стайками" to true,
            c = "парами" to false
        ),
        q(
            id = "q17",
            imageResName = "ph_0160_1",
            text = "Серая куропатка устраивает своё гнездо",
            a = "в густых кустах" to false,
            b = "на земле" to true,
            c = "в дуплах деревьев" to false
        ),
        q(
            id = "q18",
            imageResName = "ph_0161_1",
            text = "Выпь при опасности",
            a = "ныряет глубоко под воду" to false,
            b = "улетает прочь" to false,
            c = "вытягивается вертикально и сливается с тростником" to true
        ),
        q(
            id = "q19",
            imageResName = "ph_0161_1",
            text = "Народное название выпи – «речной бык» или «бугай» за",
            a = "большой размер" to false,
            b = "громкие звуки, похожие на рев" to true,
            c = "бурый цвет оперения" to false
        ),
        q(
            id = "q20",
            imageResName = "ph_0162_1",
            text = "Большой баклан после ныряния",
            a = "греется на солнце лёжа" to false,
            b = "сушит крылья, растопырив их" to true,
            c = "распушает перья на голове" to false
        ),
        q(
            id = "q21",
            imageResName = "ph_0162_1",
            text = "Бакланы по манере летать стаей похожи на",
            a = "журавлей (друг за другом в цепочку)" to false,
            b = "гусей (клином)" to true,
            c = "стрижей (беспорядочной стаей)" to false
        ),
        q(
            id = "q22",
            imageResName = "ph_0163_1",
            text = "Гнездо чомги необычное, оно",
            a = "сплетено из толстых веток" to false,
            b = "расположено высоко на дереве" to false,
            c = "плавает по воде" to true
        ),
        q(
            id = "q23",
            imageResName = "ph_0163_1",
            text = "Как маленькие птенцы чомги передвигаются вместе с матерью?",
            a = "плавают на ее спине" to true,
            b = "летят за ней" to false,
            c = "бегут за ней по берегу" to false
        ),
        q(
            id = "q24",
            imageResName = "ph_0164_1",
            text = "Щурка устраивает гнездо для птенцов в",
            a = "дуплах деревьев" to false,
            b = "старых птичьих гнёздах" to false,
            c = "норах, которые роет в песчаных обрывах" to true
        ),
        q(
            id = "q25",
            imageResName = "ph_0164_1",
            text = "Основная угроза для щурок со стороны человека связана с",
            a = "отловом для зоопарков" to false,
            b = "разрушением берегов водоёмов" to false,
            c = "уничтожением этих птиц пчеловодами" to true
        ),
        q(
            id = "q26",
            imageResName = "ph_0165_1",
            text = "Какую тактику охоты использует мухоловка-белошейка?",
            a = "активно преследует насекомых в воздухе" to false,
            b = "собирает насекомых с земли" to false,
            c = "выслеживает с ветки, хватает в воздухе и возвращается" to true
        ),
        q(
            id = "q27",
            imageResName = "ph_0165_1",
            text = "Зимует мухоловка-белошейка",
            a = "в Юго-Восточной Азии" to false,
            b = "остаётся зимовать в наших лесах" to false,
            c = "в Африке, на юге Сахары" to true
        ),
        q(
            id = "q28",
            imageResName = "ph_0166_1",
            text = "Поползень лазает по дереву",
            a = "только снизу вверх" to false,
            b = "опираясь на хвост" to false,
            c = "и вверх, и вниз" to true
        ),
        q(
            id = "q29",
            imageResName = "ph_0166_1",
            text = "Чем питается поползень осенью и что он делает с едой?",
            a = "ест только насекомых, запасов не делает" to false,
            b = "ест ягоды, потом улетает на юг" to false,
            c = "ест семена, запасы прячет, запоминая места" to true
        ),
        q(
            id = "q30",
            imageResName = "ph_0167_1",
            text = "Что делает гнездо ремеза уникальным? Оно",
            a = "построено из глины и веток" to false,
            b = "в форме варежки, из пуха, подвешено на конце ветки" to true,
            c = "находится под землёй в норе" to false
        ),
        q(
            id = "q31",
            imageResName = "ph_0167_1",
            text = "Ремез для склеивания своего гнезда использует",
            a = "собственную слюну" to false,
            b = "паутину" to true,
            c = "ил и грязь" to false
        ),
        q(
            id = "q32",
            imageResName = "ph_0168_1",
            text = "Интеллект ворона",
            a = "самый низкий среди птиц" to false,
            b = "один из самых высоких" to true,
            c = "обычный для всех врановых птиц" to false
        ),
        q(
            id = "q33",
            imageResName = "ph_0168_1",
            text = "Ворон в полёте может",
            a = "хватать птиц" to false,
            b = "переворачиваться и лететь вниз спиной" to true,
            c = "петлять между деревьями с большой скоростью" to false
        ),
        q(
            id = "q34",
            imageResName = "ph_0169_1",
            text = "Какую способность голубей использовали раньше люди?",
            a = "подражать человеческой речи" to false,
            b = "находить дорогу домой за сотни километров" to true,
            c = "видеть в полной темноте" to false
        ),
        q(
            id = "q35",
            imageResName = "ph_0169_1",
            text = "Для голубей является вредной",
            a = "хлеб, особенно с плесенью" to true,
            b = "зерновая смесь для мелких птиц" to false,
            c = "овёс и пшеница" to false
        ),
        q(
            id = "q36",
            imageResName = "ph_0170_1",
            text = "Соловьиные песни в природе означают",
            a = "призыв сородичей к еде" to false,
            b = "предупреждение об опасности" to false,
            c = "сигнал, что территория занята для гнездования" to true
        ),
        q(
            id = "q37",
            imageResName = "ph_0170_1",
            text = "Название «соловей» произошло от слова",
            a = "«соль», потому что он любит солонцы" to false,
            b = "означающего «буроватый или серый» цвет" to true,
            c = "«слава», за его знаменитое пение" to false
        ),
        q(
            id = "q38",
            imageResName = "ph_0171_1",
            text = "Что необычного в голосе иволги? Она",
            a = "не издаёт звуков вообще" to false,
            b = "подражает другим птицам" to false,
            c = "поет мелодично или громко кричит, как кошка" to true
        ),
        q(
            id = "q39",
            imageResName = "ph_0172_1",
            text = "Как охотится сизоворонка?",
            a = "выслеживает добычу на земле, бегая за ней" to false,
            b = "сидит на ветке и пикирует вниз на добычу" to true,
            c = "ловит насекомых на лету в воздухе" to false
        ),
        q(
            id = "q40",
            imageResName = "ph_0172_1",
            text = "Сизоворонка зимует",
            a = "в Южной Америке" to false,
            b = "остается зимовать у нас" to false,
            c = "в Северной Африке" to true
        ),
        q(
            id = "q41",
            imageResName = "ph_0173_1",
            text = "Снегирь питается зимой",
            a = "насекомыми под корой" to false,
            b = "исключительно семенами из шишек" to false,
            c = "ягодами и почками деревьев" to true
        ),
        q(
            id = "q42",
            imageResName = "ph_0175_1",
            text = "Так же, как и снегирь, только зимой появляется у нас",
            a = "свиристель" to true,
            b = "клест" to false,
            c = "синица большая" to false
        ),
        q(
            id = "q43",
            imageResName = "ph_0174_1",
            text = "Какой отличительный признак есть на голове удода?",
            a = "хохолок из синих перьев" to false,
            b = "«корона» из рыжих перьев с чёрными кончиками" to true,
            c = "голая красная кожа" to false
        ),
        q(
            id = "q44",
            imageResName = "ph_0174_1",
            text = "Как самка удода защищает своё гнездо?",
            a = "нападает клювом на врага" to false,
            b = "притворяется раненой, чтобы увести от гнезда" to false,
            c = "может «выстрелить» сильно пахнущим помётом" to true
        ),
        q(
            id = "q45",
            imageResName = "ph_0174_1",
            text = "Куда улетает удод зимовать?",
            a = "в Южную Америку" to false,
            b = "в Африку" to true,
            c = "в Индию" to false
        )
    )

    return if (quizPart == 1) {
        allQuestions.take(22)
    } else {
        allQuestions.drop(22)
    }
}

private fun q(
    id: String,
    imageResName: String,
    text: String,
    a: Pair<String, Boolean>,
    b: Pair<String, Boolean>,
    c: Pair<String, Boolean>
): BirdsQuestion {
    return BirdsQuestion(
        id = id,
        imageResName = imageResName,
        text = text,
        options = listOf(
            BirdsOption(id = "${id}_a", text = "а) ${a.first}", isCorrect = a.second),
            BirdsOption(id = "${id}_b", text = "б) ${b.first}", isCorrect = b.second),
            BirdsOption(id = "${id}_c", text = "в) ${c.first}", isCorrect = c.second)
        )
    )
}
