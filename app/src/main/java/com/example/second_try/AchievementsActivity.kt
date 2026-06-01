package com.example.second_try

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.second_try.ui.components.AppTopBar
import com.example.second_try.ui.theme.Second_tryTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

data class Achievement(
    val id: String,
    val title: String,
    val iconRes: Int,
    val unlocked: Boolean,
    val reward: Int = 0,
    val description: String = ""
)

class AchievementsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Second_tryTheme {
                AchievementsScreen(onNavigateBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(onNavigateBack: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val dbRef = user?.let {
        FirebaseDatabase.getInstance(
            "https://mental-health-72105-default-rtdb.europe-west1.firebasedatabase.app"
        ).getReference("Users").child(it.uid)
    }

    // состояния (Firebase-driven) — по каждой викторине
    var perfectPhotoRiddles by remember { mutableStateOf(false) }
    var perfectAmphibianSigns by remember { mutableStateOf(false) }
    var perfectReptileSigns by remember { mutableStateOf(false) }
    var perfectWhoIsWho by remember { mutableStateOf(false) }
    var perfectAmphibianNames by remember { mutableStateOf(false) }
    var perfectReptileNames by remember { mutableStateOf(false) }
    var perfectDangerousOrNot by remember { mutableStateOf(false) }
    var perfectBeastsQuiz1 by remember { mutableStateOf(false) }
    var perfectBeastsQuiz2 by remember { mutableStateOf(false) }
    var perfectBirdsQuiz1 by remember { mutableStateOf(false) }
    var perfectBirdsQuiz2 by remember { mutableStateOf(false) }
    var perfectAllQuizzes by remember { mutableStateOf(false) }

    var anyQuizDone by remember { mutableStateOf(false) }
    var allQuizzesDone by remember { mutableStateOf(false) }
    var masterPhotoUnlocked by remember { mutableStateOf(false) }
    var photosSaved by remember { mutableStateOf(0) }

    // --- Состояние просмотренных карточек (локально) ---
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("viewed_cards", Context.MODE_PRIVATE)

    var viewedSet by remember { mutableStateOf(prefs.getStringSet("viewed_set", setOf()) ?: setOf()) }

    // Подписка на изменения SharedPreferences, чтобы экран реагировал сразу
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            if (key == "viewed_set") {
                viewedSet = sp.getStringSet("viewed_set", setOf()) ?: setOf()
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        dbRef?.child("achievementsDone")?.child("master_photo")
            ?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    masterPhotoUnlocked = snapshot.getValue(Boolean::class.java) ?: false
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // читаем прогресс викторин и photosSaved из Firebase
    LaunchedEffect(Unit) {
        dbRef?.child("quiz_progress")?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                fun bool(key: String) = snapshot.child(key).getValue(Boolean::class.java) ?: false

                perfectPhotoRiddles = bool("perfect_photo_riddles")
                perfectAmphibianSigns = bool("perfect_amphibian_signs")
                perfectReptileSigns = bool("perfect_reptile_signs")
                perfectWhoIsWho = bool("perfect_who_is_who")
                perfectAmphibianNames = bool("perfect_amphibian_names")
                perfectReptileNames = bool("perfect_reptile_names")
                perfectDangerousOrNot = bool("perfect_dangerous_or_not")
                perfectBeastsQuiz1 = bool("perfect_beasts_quiz_1")
                perfectBeastsQuiz2 = bool("perfect_beasts_quiz_2")
                perfectBirdsQuiz1 = bool("perfect_birds_quiz_1")
                perfectBirdsQuiz2 = bool("perfect_birds_quiz_2")

                perfectAllQuizzes = perfectPhotoRiddles && perfectAmphibianSigns &&
                    perfectReptileSigns && perfectWhoIsWho && perfectAmphibianNames &&
                    perfectReptileNames && perfectDangerousOrNot &&
                    perfectBeastsQuiz1 && perfectBeastsQuiz2 &&
                    perfectBirdsQuiz1 && perfectBirdsQuiz2

                val doneKeys = listOf(
                    "photo_riddles_done", "amphibian_signs_done", "reptile_signs_done",
                    "who_is_who_done", "amphibian_names_done", "reptile_names_done",
                    "dangerous_or_not_done", "beasts_quiz_1_done", "beasts_quiz_2_done",
                    "birds_quiz_1_done", "birds_quiz_2_done"
                )
                anyQuizDone = doneKeys.any { bool(it) }
                allQuizzesDone = bool("all_quizzes_done")
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        dbRef?.child("photosSaved")?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                photosSaved = snapshot.getValue(Int::class.java) ?: 0
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    val REPTILES_TOTAL = 14
    val AMPHIBIANS_TOTAL = 12
    val reptileCount = viewedSet.count { it.startsWith("pres") }
    val amphibianCount = viewedSet.count { it.startsWith("zem") }
    val zoologistUnlocked = amphibianCount >= AMPHIBIANS_TOTAL
    val herpetologistUnlocked = reptileCount >= REPTILES_TOTAL

    // список достижений
    val achievements = listOf(
        // ── Безошибочные — по каждой из 11 викторин ──
        Achievement("perfect_photo_riddles", "Безошибочный! Фотозагадки", R.drawable.ic_medal, perfectPhotoRiddles, 120,
            "Ты разгадал все фотозагадки без единой ошибки — глаз-алмаз! 📸"),
        Achievement("perfect_amphibian_signs", "Безошибочный! Признаки земноводных", R.drawable.ic_medal, perfectAmphibianSigns, 120,
            "Ты знаешь все признаки земноводных назубок — никаких ошибок! 🐸"),
        Achievement("perfect_reptile_signs", "Безошибочный! Признаки пресмыкающихся", R.drawable.ic_medal, perfectReptileSigns, 120,
            "Ни одной ошибки в признаках рептилий — настоящий эксперт! 🦎"),
        Achievement("perfect_who_is_who", "Безошибочный! Кто есть кто?", R.drawable.ic_medal, perfectWhoIsWho, 120,
            "Ты мгновенно отличаешь земноводных от рептилий — безупречно! 🌟"),
        Achievement("perfect_amphibian_names", "Безошибочный! Названия земноводных", R.drawable.ic_medal, perfectAmphibianNames, 120,
            "Все названия земноводных — без единой ошибки! Фантастика! 🐊"),
        Achievement("perfect_reptile_names", "Безошибочный! Названия пресмыкающихся", R.drawable.ic_medal, perfectReptileNames, 120,
            "Все названия пресмыкающихся на отлично — это впечатляет! 🐍"),
        Achievement("perfect_dangerous_or_not", "Безошибочный! Опасные и безобидные", R.drawable.ic_medal, perfectDangerousOrNot, 120,
            "Ты точно знаешь кто опасен, а кто нет — ни разу не ошибся! ⚡"),
        Achievement("perfect_beasts_quiz_1", "Безошибочный! Звери 1", R.drawable.ic_medal, perfectBeastsQuiz1, 120,
            "Первая часть про зверей — без единой ошибки! Вот это зверюга! 🐺"),
        Achievement("perfect_beasts_quiz_2", "Безошибочный! Звери 2", R.drawable.ic_medal, perfectBeastsQuiz2, 120,
            "Вторая часть про зверей — снова без ошибок! Невероятно! 🦁"),
        Achievement("perfect_birds_quiz_1", "Безошибочный! Птицы 1", R.drawable.ic_medal, perfectBirdsQuiz1, 120,
            "Первая часть про птиц — идеально! Ты просто орёл! 🦅"),
        Achievement("perfect_birds_quiz_2", "Безошибочный! Птицы 2", R.drawable.ic_medal, perfectBirdsQuiz2, 120,
            "Вторая часть про птиц — снова без ошибок! Вот это полёт мысли! 🦜"),
        // ── Главный приз ──
        Achievement("perfect_all_quizzes", "Абсолютно безошибочный!", R.drawable.ic_trophey, perfectAllQuizzes, 500,
            "Все 11 викторин на 100%! Ты настоящая легенда знаний о животных! 🏆"),
        // ── Прогресс ──
        Achievement("quiz_first_done", "Первый шаг", R.drawable.ic_medal, anyQuizDone, 50,
            "Ты прошёл свою первую викторину — это начало большого пути! 🌟"),
        Achievement("all_quizzes_done", "Все викторины пройдены!", R.drawable.ic_trophey, allQuizzesDone, 100,
            "Ты прошёл все 11 викторин! Это настоящий подвиг! 🎉"),
        // ── Фото ──
        Achievement("first_photo", "Первое фото", R.drawable.camera, photosSaved >= 1, 30,
            "Ты сохранил своё первое фото в галерею. Начало положено! 📸"),
        Achievement("photos", "Фотограф природы", R.drawable.camera, photosSaved >= 5, 150,
            "Целых 5 фотографий в галерее! Ты настоящий фотограф природы! 📷"),
        Achievement("master_photo", "Мастер кадра", R.drawable.ic_trophey, masterPhotoUnlocked, 50,
            "Ты так часто заглядывал в галерею! Настоящий мастер кадра! 🎨"),
        // ── Карточки ──
        Achievement("zoologist", "Зоолог-любитель", R.drawable.ic_medal, zoologistUnlocked, 50,
            "Ты прочитал все карточки о земноводных — теперь ты настоящий зоолог-любитель! 🐊"),
        Achievement("herpetologist", "Герпетолог", R.drawable.ic_trophey, herpetologistUnlocked, 50,
            "Ты прочитал все карточки о пресмыкающихся. Ты достойный герпетолог! 🦖")
    )

    // 2. сохраняем достижения и выдаём шишки (логика похожа на уже существующую)
    LaunchedEffect(achievements) {
        achievements.forEach { achievement ->
            if (achievement.unlocked) {
                // пишем в achievementsDone
                dbRef?.child("achievementsDone")?.child(achievement.id)?.setValue(true)

                // проверяем reward
                val rewardedRef = dbRef?.child("achievementsRewarded")?.child(achievement.id)
                rewardedRef?.get()?.addOnSuccessListener { snapshot ->
                    val alreadyRewarded = snapshot.getValue(Boolean::class.java) ?: false
                    if (!alreadyRewarded) {
                        val reward = achievement.reward
                        val conesRef = dbRef.child("cones")
                        conesRef.get().addOnSuccessListener { conesSnap ->
                            val currentCones = conesSnap.getValue(Int::class.java) ?: 0
                            conesRef.setValue(currentCones + reward)
                        }

                        // отмечаем, что награда выдана
                        rewardedRef.setValue(true)
                    }
                }
            }
        }
    }

    // прогрессбар
    val unlockedCount = achievements.count { it.unlocked }
    val totalCount = achievements.size
    val progress = unlockedCount.toFloat() / totalCount.toFloat()

    Scaffold(
        topBar = { AppTopBar(title = "Достижения", onBack = onNavigateBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Открыто $unlockedCount из $totalCount достижений",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .padding(vertical = 12.dp),
                color = Color(0xFF6200EE)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🔒", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Некоторые достижения пока скрыты — это сюрпризы! 🎁\nИсследуй приложение, проходи викторины, читай карточки — и они будут открываться одно за другим. Нажми на открытое достижение, чтобы узнать о нём больше!",
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(achievements) { achievement ->
                    AchievementItem(achievement)
                }
            }
        }
    }
}

@Composable
fun AchievementItem(achievement: Achievement) {
    var showDialog by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        if (achievement.unlocked) {
            Image(
                painter = painterResource(id = achievement.iconRes),
                contentDescription = achievement.title,
                modifier = Modifier
                    .size(72.dp)
                    .padding(4.dp)
                    .clickable { showDialog = true }
            )
            Text(
                text = achievement.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_lock),
                contentDescription = "Закрыто",
                modifier = Modifier
                    .size(72.dp)
                    .padding(4.dp)
            )
            Text(
                text = "???",
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = achievement.title,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = achievement.iconRes),
                        contentDescription = achievement.title,
                        modifier = Modifier
                            .size(120.dp)
                            .padding(8.dp)
                    )
                    Text(
                        text = achievement.description,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Награда: ",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${achievement.reward} малинок",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "🍓", fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Ура, понятно!")
                }
            }
        )
    }
}
