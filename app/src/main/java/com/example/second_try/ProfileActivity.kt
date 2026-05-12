package com.example.second_try

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.example.second_try.ui.components.AppTopBar
import com.example.second_try.ui.theme.Second_tryTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.setContent

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Second_tryTheme {
                ProfileScreen(
                    onNavigateBack = {
                        val intent = Intent(this, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser!!
    val dbRef = FirebaseDatabase.getInstance(
        "https://mental-health-72105-default-rtdb.europe-west1.firebasedatabase.app"
    ).getReference("Users").child(user.uid)

    val profileRef = dbRef.child("profile")

    var profileData by remember {
        mutableStateOf(
            mapOf(
                "username" to "",
                "birthDate" to "",
                "about" to "",
                "city" to "",
                "goal" to "",
                "favoriteAnimal" to "",
                "cones" to "0",
                "photosSaved" to "0"
            )
        )
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var profilePhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showFullScreenPhoto by remember { mutableStateOf(false) }
    var achievementsCount by remember { mutableStateOf(0) }

    // Загрузка фото из локального хранилища
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)
        val path = prefs.getString("profile_photo_path", null)
        if (path != null && File(path).exists()) profilePhotoUri = Uri.fromFile(File(path))
    }

    // Загрузка данных из Firebase
    LaunchedEffect(Unit) {
        // Загружаем ВСЕ данные пользователя
        dbRef.get().addOnSuccessListener { snap ->
            val data = mutableMapOf<String, String>()

            // Загружаем поля из profile
            snap.child("profile").children.forEach {
                val key = it.key ?: return@forEach
                val value = it.value
                data[key] = value?.toString() ?: ""
            }

            // Загружаем поля с верхнего уровня
            val topLevelKeys = listOf("cones", "photosSaved")
            topLevelKeys.forEach { key ->
                val value = snap.child(key).value
                if (value != null) data[key] = value.toString()
            }

            profileData = profileData + data
        }

        // Считаем достижения
        dbRef.child("achievementsDone").get().addOnSuccessListener { snap ->
            achievementsCount = snap.childrenCount.toInt()
        }
    }



    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val registrationDate =
        user.metadata?.creationTimestamp?.let { dateFormat.format(Date(it)) } ?: "-"

    val cones = profileData["cones"]?.toIntOrNull() ?: 0
    val level = when {
        cones >= 1001 -> 5
        cones >= 501 -> 4
        cones >= 251 -> 3
        cones >= 101 -> 2
        else -> 1
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Профиль",
                onBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showEditDialog = true }) {
                Text("✏️")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileImage(
                uri = profilePhotoUri,
                onClick = { showFullScreenPhoto = true }
            )

            Spacer(Modifier.height(16.dp))
            Text(text = profileData["username"] ?: "", style = MaterialTheme.typography.titleLarge)

            Spacer(Modifier.height(24.dp))
            ProfileField("Уровень", "$level")
            ProfileField("Количество малинок", profileData["cones"] ?: "")
            ProfileField("Дата регистрации", registrationDate)
            ProfileField("Количество фотографий", profileData["photosSaved"] ?: "")
            ProfileField("Количество достижений", achievementsCount.toString())
            ProfileField("Город", profileData["city"] ?: "")
            ProfileField("Дата рождения", profileData["birthDate"] ?: "")
            ProfileField("О себе", profileData["about"] ?: "")
            ProfileField("Любимое животное", profileData["favoriteAnimal"] ?: "")
            ProfileField("Цель", profileData["goal"] ?: "")
        }
    }

    // Диалог редактирования
    if (showEditDialog) {
        EditProfileDialog(
            currentData = profileData,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                profileRef.updateChildren(updated.mapValues { it.value as Any })
                profileData = profileData + updated
                showEditDialog = false
            }
        )
    }

    // Просмотр фото на весь экран
    if (showFullScreenPhoto) {
        FullScreenPhotoDialog(
            photoUri = profilePhotoUri,
            onDismiss = { showFullScreenPhoto = false },
            onNewPhotoSelected = { newUri ->
                profilePhotoUri = newUri
                saveProfilePhotoLocally(context, newUri)
                showFullScreenPhoto = false
            }
        )
    }
}

@Composable
fun ProfileField(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.Bold)
        Text(if (value.isNotEmpty()) value else "")
    }
}

@Composable
fun ProfileImage(uri: Uri?, onClick: () -> Unit) {
    val painter = if (uri != null)
        rememberAsyncImagePainter(uri)
    else
        painterResource(R.drawable.ic_orig)

    Image(
        painter = painter,
        contentDescription = "Фото профиля",
        modifier = Modifier
            .size(140.dp)
            .clickable { onClick() }
    )
}

@Composable
fun FullScreenPhotoDialog(
    photoUri: Uri?,
    onDismiss: () -> Unit,
    onNewPhotoSelected: (Uri) -> Unit
) {
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { newUri ->
            if (newUri != null) onNewPhotoSelected(newUri)
        }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val painter = if (photoUri != null)
                    rememberAsyncImagePainter(photoUri)
                else
                    painterResource(R.drawable.ic_orig)

                Image(
                    painter = painter,
                    contentDescription = "Фото профиля",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .clickable { }
                )

                Spacer(Modifier.height(16.dp))
                Button(onClick = { launcher.launch("image/*") }) {
                    Text("Выбрать новое фото")
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Закрыть", color = Color.Gray)
                }
            }
        }
    }
}

fun saveProfilePhotoLocally(context: Context, uri: Uri) {
    val input = context.contentResolver.openInputStream(uri)
    val file = File(context.filesDir, "profile_photo.jpg")
    val output = FileOutputStream(file)
    input?.copyTo(output)
    input?.close()
    output.close()

    val prefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("profile_photo_path", file.absolutePath).apply()
}

@Composable
fun EditProfileDialog(
    currentData: Map<String, String>,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit
) {
    var city by remember { mutableStateOf(currentData["city"] ?: "") }
    var birthDate by remember { mutableStateOf(currentData["birthDate"] ?: "") }
    var about by remember { mutableStateOf(currentData["about"] ?: "") }
    var goal by remember { mutableStateOf(currentData["goal"] ?: "") }
    var favoriteAnimal by remember { mutableStateOf(currentData["favoriteAnimal"] ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Редактировать профиль", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Город") })
                OutlinedTextField(value = birthDate, onValueChange = { birthDate = it }, label = { Text("Дата рождения") })
                OutlinedTextField(value = about, onValueChange = { about = it }, label = { Text("О себе") })
                OutlinedTextField(value = favoriteAnimal, onValueChange = { favoriteAnimal = it }, label = { Text("Любимое животное") })
                OutlinedTextField(value = goal, onValueChange = { goal = it }, label = { Text("Цель") })

                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    Button(onClick = {
                        onSave(
                            mapOf(
                                "city" to city,
                                "birthDate" to birthDate,
                                "about" to about,
                                "goal" to goal,
                                "favoriteAnimal" to favoriteAnimal
                            )
                        )
                    }) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}
