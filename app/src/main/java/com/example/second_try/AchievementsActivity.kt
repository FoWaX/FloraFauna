    package com.example.second_try

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
    import com.example.second_try.R

    data class Achievement(
        val id: String,
        val title: String,
        val iconRes: Int,
        val unlocked: Boolean
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

        // состояния
        var quiz1Done by remember { mutableStateOf(false) }
        var allQuizzesDone by remember { mutableStateOf(false) }
        var photosSaved by remember { mutableStateOf(0) }

        // 1. читаем прогресс викторин
        LaunchedEffect(Unit) {
            dbRef?.child("quiz_progress")?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    quiz1Done = snapshot.child("quiz1_done").getValue(Boolean::class.java) ?: false
                    allQuizzesDone = snapshot.child("all_quizzes_done").getValue(Boolean::class.java) ?: false
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

        // список достижений
        val achievements = listOf(
            Achievement("quiz1_done", "Первый шаг", R.drawable.ic_medal, quiz1Done),
            Achievement("all_quizzes_done", "Все викторины", R.drawable.ic_trophy, allQuizzesDone),
            Achievement("photos", "Фотограф природы", R.drawable.camera, photosSaved >= 5)
        )

        // 2. сохраняем достижения и выдаём шишки
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
                            // начисляем награду
                            val reward = when (achievement.id) {
                                "quiz1_done" -> 50
                                "all_quizzes_done" -> 100
                                "photos" -> 150
                                else -> 0
                            }
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
                title = { Text(achievement.title) },
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
                            text = when (achievement.id) {
                                "quiz1_done" -> "Получено за прохождение первой викторины."
                                "all_quizzes_done" -> "Получено за прохождение всех викторин."
                                "photos" -> "Получено за сохранение 5 фотографий."
                                else -> ""
                            },
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Закрыть")
                    }
                }
            )
        }
    }
