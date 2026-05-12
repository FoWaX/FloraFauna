package com.example.second_try

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.second_try.ui.components.AppTopBar
import com.example.second_try.ui.theme.AppColors
import com.example.second_try.ui.theme.Second_tryTheme
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

data class LeaderboardUser(
    val username: String,
    val cones: Int
)

class LeaderboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Second_tryTheme {
                LeaderboardScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun LeaderboardScreen(
    onBack: () -> Unit
) {
    var users by remember { mutableStateOf<List<LeaderboardUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val usersRef = remember {
        FirebaseDatabase.getInstance(
            "https://mental-health-72105-default-rtdb.europe-west1.firebasedatabase.app"
        ).getReference("Users")
    }

    DisposableEffect(Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val loadedUsers = mutableListOf<LeaderboardUser>()

                for (userSnapshot in snapshot.children) {
                    val username = userSnapshot.child("username")
                        .getValue(String::class.java)
                        ?: userSnapshot.child("email")
                            .getValue(String::class.java)
                            ?.substringBefore("@")
                        ?: "Пользователь"

                    val cones = userSnapshot.child("cones")
                        .getValue(Int::class.java)
                        ?: userSnapshot.child("cones")
                            .getValue(Long::class.java)
                            ?.toInt()
                        ?: 0

                    loadedUsers.add(
                        LeaderboardUser(
                            username = username,
                            cones = cones
                        )
                    )
                }

                users = loadedUsers.sortedByDescending { it.cones }
                isLoading = false
                errorText = null
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
                errorText = "Не удалось загрузить рейтинг: ${error.message}"
            }
        }

        usersRef.addValueEventListener(listener)

        onDispose {
            usersRef.removeEventListener(listener)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Рейтинг",
                onBack = onBack
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
                text = "Рейтинг пользователей",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextGreen
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Здесь показаны пользователи от первого к последнему по количеству малинок.",
                fontSize = 15.sp,
                color = AppColors.TextGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> {
                    Text(
                        text = "Загружаем рейтинг...",
                        fontSize = 16.sp,
                        color = AppColors.TextGray
                    )
                }

                errorText != null -> {
                    Text(
                        text = errorText ?: "",
                        fontSize = 16.sp,
                        color = AppColors.Coral
                    )
                }

                users.isEmpty() -> {
                    Text(
                        text = "Пока в рейтинге никого нет.",
                        fontSize = 16.sp,
                        color = AppColors.TextGray
                    )
                }

                else -> {
                    users.forEachIndexed { index, user ->
                        LeaderboardUserCard(
                            place = index + 1,
                            user = user
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun LeaderboardUserCard(
    place: Int,
    user: LeaderboardUser
) {
    val placeColor = when (place) {
        1 -> Color(0xFFFFD54F)
        2 -> Color(0xFFB0BEC5)
        3 -> Color(0xFFFFB74D)
        else -> AppColors.LightGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.WarmCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(containerColor = placeColor)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = place.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.username,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextDark
                )

                Text(
                    text = "Малинок: ${user.cones}",
                    fontSize = 14.sp,
                    color = AppColors.TextGray
                )
            }

            Image(
                painter = painterResource(id = R.drawable.prize),
                contentDescription = "Малинки",
                modifier = Modifier.size(30.dp)
            )
        }
    }
}