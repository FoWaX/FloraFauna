package com.example.second_try

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        setContent {
            Second_tryTheme {
                BirdsQuizScreen(onBackPressed = { finish() })
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
    val text: String,
    val options: List<BirdsOption>
)

@Composable
fun BirdsQuizScreen(onBackPressed: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val dbRef = remember(user) {
        FirebaseDatabase.getInstance(
            "https://mental-health-72105-default-rtdb.europe-west1.firebasedatabase.app"
        ).getReference("Users").child(user!!.uid).child("quiz_progress")
    }

    // БАЗОВЫЕ вопросы (без перемешивания)
    val rawQuestions = remember {
        buildBirdsQuizQuestions()
    }

    // Перемешиваем варианты ОДИН раз на запуск экрана
    val questions = remember(rawQuestions) {
        rawQuestions.map { q ->
            q.copy(options = q.options.shuffled())
        }
    }

    // selectedAnswers: questionId -> optionId
    var selectedAnswers by rememberSaveable { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isSubmitted by rememberSaveable { mutableStateOf(false) }

    val totalQuestions = questions.size

    val correctCount = remember(selectedAnswers, isSubmitted, questions) {
        if (!isSubmitted) 0
        else {
            questions.count { q ->
                val selectedOptionId = selectedAnswers[q.id]
                q.options.any { it.id == selectedOptionId && it.isCorrect }
            }
        }
    }

    // Сохранение прогресса в Firebase после подтверждения
    LaunchedEffect(isSubmitted) {
        if (isSubmitted) {
            dbRef.child("birds_quiz_done").setValue(true)
            if (correctCount == totalQuestions) {
                dbRef.child("perfect_birds_quiz").setValue(true)
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Викторина: Птицы",
                onBack = onBackPressed
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Выбери по одному ответу в каждом вопросе, затем нажми «Подтвердить».",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))

            questions.forEachIndexed { index, question ->
                BirdsQuestionCard(
                    number = index + 1,
                    question = question,
                    selectedOptionId = selectedAnswers[question.id],
                    isSubmitted = isSubmitted,
                    onSelect = { optionId ->
                        if (!isSubmitted) {
                            selectedAnswers = selectedAnswers.toMutableMap().apply {
                                this[question.id] = optionId
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            val allAnswered = questions.all { q -> selectedAnswers.containsKey(q.id) }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { isSubmitted = true },
                enabled = !isSubmitted && allAnswered,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        isSubmitted -> "Тест уже проверен"
                        !allAnswered -> "Ответь на все вопросы"
                        else -> "Подтвердить"
                    }
                )
            }

            if (!allAnswered && !isSubmitted) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Чтобы проверить результат, нужно ответить на все вопросы.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            if (isSubmitted) {
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Результат",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Верных ответов: $correctCount из $totalQuestions",
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        context.startActivity(Intent(context, TasksActivity::class.java))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Вернуться к викторинам")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        context.startActivity(Intent(context, MainActivity::class.java))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Главное меню")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BirdsQuestionCard(
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
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "$number. ${question.text}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            question.options.forEach { option ->
                val isSelected = selectedOptionId == option.id

                val rowBg = when {
                    !isSubmitted -> Color.Transparent
                    isSelected && option.isCorrect -> Color(0xFFC8E6C9) // зеленый
                    isSelected && !option.isCorrect -> Color(0xFFFFCDD2) // красный
                    else -> Color.Transparent
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .background(rowBg)
                        .clickable(enabled = !isSubmitted) { onSelect(option.id) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = if (isSubmitted) null else ({ onSelect(option.id) })
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = option.text,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

private fun buildBirdsQuizQuestions(): List<BirdsQuestion> {
    return listOf(
        q("q1", "Размах крыльев у беркута до",
            "1 метра" to false,
            "2,5 метров" to true,
            "3,5 метров" to false
        ),
        q("q2", "Беркут, орлан-белохвост и орел степной занесены",
            "только в Красную книгу РФ" to false,
            "только в Красную книгу СО" to false,
            "в обе Книги" to true
        ),
        q("q3", "Численность дрофы сокращается из-за",
            "изменения климата" to false,
            "строительства городов" to false,
            "браконьерства и распашки земель" to true
        ),
        q("q4", "«Дрофа» в переводе с древнеславянского",
            "«Большая птица»" to false,
            "«Бегущая птица»" to true,
            "«Тяжёлая птица»" to false
        ),
        q("q5", "Способ охоты филина:",
            "долго преследует жертву в полёте" to false,
            "атакует из засады, бесшумно пикируя" to true,
            "загоняет добычу стаей" to false
        ),
        q("q6", "Страуса африканского в Саратовской области",
            "разводят и выпускают в природу" to false,
            "можно увидеть только в цирке" to false,
            "содержат на специальных фермах" to true
        ),
        q("q7", "У журавлей есть интересная особенность поведения, они",
            "умеют петь сложные песни" to false,
            "часто танцуют, особенно в период размножения" to true,
            "строят гнёзда на кустах" to false
        ),
        q("q8", "Стая журавлей во время перелёта выглядит как",
            "шеренга (линия)" to false,
            "клин" to true,
            "беспорядочная группа" to false
        ),
        q("q9", "Почему королёк получил такое название?",
            "потому что поёт слово «ко-роль»" to false,
            "Из-за «короны» на голове" to true,
            "Он самый сильный среди мелких птиц" to false
        ),
        q("q10", "Королька в Саратовской области можно встретить",
            "только в хвойных лесах" to true,
            "в любом лесу или парке" to false,
            "вблизи водоёмов" to false
        ),
        q("q11", "Воробьиный сыч отличается от других сов тем, что он",
            "не охотится на мышей" to false,
            "может охотиться и ночью, и днём" to true,
            "строит гнёзда на земле" to false
        ),
        q("q12", "Вальдшнеп, в отличие от других куликов, живет",
            "на открытых болотах" to false,
            "на песчаных отмелях рек" to false,
            "в густых лесах" to true
        ),
        q("q13", "Длинный клюв нужен вальдшнепу для",
            "отличия самца от самки" to false,
            "добычи червей и насекомых из лесной почвы" to true,
            "защиты от хищников" to false
        ),
        q("q14", "Самцу фазана яркая окраска нужна, чтобы",
            "отпугивать хищников" to false,
            "привлекать самок в брачный период" to true,
            "маскироваться летом среди цветов" to false
        ),
        q("q15", "Соперники фазана по яркости окраски:",
            "беркут и орлан-белохвост" to false,
            "сизоворонка, щурка и зимородок" to true,
            "журавль и дрофа" to false
        ),
        q("q16", "Серые куропатки осенью и зимой живут",
            "поодиночке" to false,
            "стайками" to true,
            "парами" to false
        ),
        q("q17", "Серая куропатка устраивает своё гнездо",
            "в густых кустах" to false,
            "на земле" to true,
            "в дуплах деревьев" to false
        ),
        q("q18", "Выпь при опасности",
            "ныряет глубоко под воду" to false,
            "улетает прочь" to false,
            "вытягивается вертикально и сливается с тростником" to true
        ),
        q("q19", "Народное название выпи – «речной бык» или «бугай» за",
            "большой размер" to false,
            "громкие звуки, похожие на рев" to true,
            "бурый цвет оперения" to false
        ),
        q("q20", "Большой баклан после ныряния",
            "греется на солнце лёжа" to false,
            "сушит крылья, растопырив их" to true,
            "распушает перья на голове" to false
        ),
        q("q21", "Бакланы по манере летать стаей похожи на",
            "журавлей (друг за другом в цепочку)" to false,
            "гусей (клином)" to true,
            "стрижей (беспорядочной стаей)" to false
        ),
        q("q22", "Гнездо чомги необычное, оно",
            "сплетено из толстых веток" to false,
            "расположено высоко на дереве" to false,
            "плавает по воде" to true
        ),
        q("q23", "Как маленькие птенцы чомги передвигаются вместе с матерью?",
            "плавают на ее спине" to true,
            "летят за ней" to false,
            "бегут за ней по берегу" to false
        ),
        q("q24", "Щурка устраивает гнездо для птенцов в",
            "дуплах деревьев" to false,
            "старых птичьих гнёздах" to false,
            "норах, которые роет в песчаных обрывах" to true
        ),
        q("q25", "Основная угроза для щурок со стороны человека связана с",
            "отловом для зоопарков" to false,
            "разрушением берегов водоёмов" to false,
            "уничтожением этих птиц пчеловодами" to true
        ),
        q("q26", "Какую тактику охоты использует мухоловка-белошейка?",
            "Активно преследует насекомых в воздухе" to false,
            "Собирает насекомых с земли" to false,
            "Выслеживает с ветки, хватает в воздухе и возвращается" to true
        ),
        q("q27", "Зимует мухоловка-белошейка",
            "в Юго-Восточной Азии" to false,
            "остаётся зимовать в наших лесах" to false,
            "в Африку, на юг Сахары" to true
        ),
        q("q28", "Поползень лазает по дереву",
            "только снизу вверх" to false,
            "опираясь на хвост" to false,
            "и вверх, и вниз" to true
        ),
        q("q29", "Чем питается поползень осенью и что он делает с едой?",
            "Ест только насекомых, запасов не делает" to false,
            "Ест ягоды, потом улетает на юг" to false,
            "Ест семена, запасы прячет, запоминая места" to true
        ),
        q("q30", "Что делает гнездо ремеза уникальным? Оно",
            "построено из глины и веток" to false,
            "в форме варежки, из пуха, подвешено на конце ветки" to true,
            "находится под землёй в норе" to false
        ),
        q("q31", "Ремез для склеивания своего гнезда использует",
            "собственную слюну" to false,
            "паутину" to true,
            "ил и грязь" to false
        ),
        q("q32", "Интеллект ворона",
            "самый низкий среди птиц" to false,
            "один из самых высоких" to true,
            "обычный для всех врановых птиц" to false
        ),
        q("q33", "Ворон в полёте может",
            "хватать птиц" to false,
            "переворачиваться и лететь вниз спиной" to true,
            "петлять между деревьями с большой скоростью" to false
        ),
        q("q34", "Какую способность голубей использовали раньше люди?",
            "подражать человеческой речи" to false,
            "находить дорогу домой за сотни километров" to true,
            "видеть в полной темноте" to false
        ),
        q("q35", "Для голубей является вредной",
            "хлеб, особенно с плесенью" to true,
            "зерновая смесь для мелких птиц" to false,
            "овёс и пшеница" to false
        ),
        q("q36", "Соловьиные песни в природе означают",
            "Призыв сородичей к еде" to false,
            "Предупреждение об опасности" to false,
            "Сигнал, что территория занята для гнездования" to true
        ),
        q("q37", "Название «соловей» произошло от слова",
            "«соль», потому что он любит солонцы" to false,
            "означающего «буроватый или серый» цвет" to true,
            "«слава», за его знаменитое пение" to false
        ),
        q("q38", "Что необычного в голосе иволги? Она",
            "не издаёт звуков вообще" to false,
            "подражает другим птицам" to false,
            "поет мелодично или громко кричит, как кошка" to true
        ),
        q("q39", "Как охотится сизоворонка?",
            "выслеживает добычу на земле, бегая за ней" to false,
            "сидит на ветке и пикирует вниз на добычу" to true,
            "ловит насекомых на лету в воздухе" to false
        ),
        q("q40", "Сизоворонка зимует",
            "в Южной Америке" to false,
            "остается зимовать у нас" to false,
            "в Северной Африке" to true
        ),
        q("q41", "Снегирь питается зимой",
            "насекомыми под корой" to false,
            "исключительно семенами из шишек" to false,
            "ягодами и почками деревьев" to true
        ),
        q("q42", "Так же, как и снегирь, только зимой появляется у нас",
            "свиристель" to true,
            "клест" to false,
            "синица большая" to false
        ),
        q("q43", "Какой отличительный признак есть на голове удода?",
            "Хохолок из синих перьев" to false,
            "«Корона» из рыжих перьев с чёрными кончиками" to true,
            "Голая красная кожа" to false
        ),
        q("q44", "Как самка удода защищает своё гнездо?",
            "нападает клювом на врага" to false,
            "притворяется раненой, чтобы увести от гнезда" to false,
            "может «выстрелить» сильно пахнущим помётом" to true
        ),
        q("q45", "Куда улетает удод зимовать?",
            "в Южную Америку" to false,
            "в Африку" to true,
            "в Индию" to false
        )
    )
}

private fun q(
    id: String,
    text: String,
    a: Pair<String, Boolean>,
    b: Pair<String, Boolean>,
    c: Pair<String, Boolean>
): BirdsQuestion {
    return BirdsQuestion(
        id = id,
        text = text,
        options = listOf(
            BirdsOption(id = "${id}_a", text = "а) ${a.first}", isCorrect = a.second),
            BirdsOption(id = "${id}_b", text = "б) ${b.first}", isCorrect = b.second),
            BirdsOption(id = "${id}_c", text = "в) ${c.first}", isCorrect = c.second)
        )
    )
}