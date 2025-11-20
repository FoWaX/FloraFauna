package com.example.second_try

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.second_try.ui.components.AppTopBar
import com.example.second_try.ui.theme.Second_tryTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

data class GalleryImage(
    val uri: Uri,
    var title: String = ""
)

class GalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Second_tryTheme {
                GalleryScreen(onNavigateBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) return

    val prefs = context.getSharedPreferences("gallery_${user.uid}", Context.MODE_PRIVATE)

    // --- Увеличиваем счётчик открытий галереи ---
    LaunchedEffect(Unit) {
        val dbRef = FirebaseDatabase.getInstance(
            "https://mental-health-72105-default-rtdb.europe-west1.firebasedatabase.app"
        ).getReference("Users").child(user.uid)

        val visitsRef = dbRef.child("galleryVisits")
        visitsRef.get().addOnSuccessListener { snapshot ->
            val currentCount = snapshot.getValue(Int::class.java) ?: 0
            val newCount = currentCount + 1
            visitsRef.setValue(newCount)

            // Проверяем достижение "Мастер кадра"
            if (newCount >= 10) {
                val doneRef = dbRef.child("achievementsDone").child("master_photo")
                val rewardedRef = dbRef.child("achievementsRewarded").child("master_photo")

                doneRef.setValue(true)

                rewardedRef.get().addOnSuccessListener { rewardSnap ->
                    val alreadyRewarded = rewardSnap.getValue(Boolean::class.java) ?: false
                    if (!alreadyRewarded) {
                        val conesRef = dbRef.child("cones")
                        conesRef.get().addOnSuccessListener { conesSnap ->
                            val currentCones = conesSnap.getValue(Int::class.java) ?: 0
                            conesRef.setValue(currentCones + 50) // +50 шишек
                        }
                        rewardedRef.setValue(true)
                    }
                }

                // Удаляем счётчик после получения достижения
                visitsRef.removeValue()
            }
        }
    }


    var showDeleteDialog by remember { mutableStateOf(false) }
    var images by remember {
        mutableStateOf<List<GalleryImage>>(
            prefs.getStringSet("image_paths", emptySet())
                ?.map {
                    val parts = it.split(";")
                    GalleryImage(Uri.parse(parts[0]), parts.getOrNull(1) ?: "")
                } ?: emptyList()
        )
    }

    var selectedImage by remember { mutableStateOf<GalleryImage?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf(setOf<Uri>()) }

    fun saveImagesToPrefs(list: List<GalleryImage>) {
        prefs.edit().putStringSet(
            "image_paths",
            list.map { "${it.uri};${it.title}" }.toSet()
        ).apply()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Твоя коллекция",
                onBack = {
                    if (selectionMode) {
                        selectionMode = false
                        selectedItems = emptySet()
                    } else {
                        onNavigateBack()
                    }
                },
                actions = {
                    if (selectionMode && selectedItems.isNotEmpty()) {
                        TextButton(onClick = { showDeleteDialog = true }) {
                            Text("Удалить", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            )

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Удаление") },
                    text = { Text("Удалить ${selectedItems.size} фото?") },
                    confirmButton = {
                        TextButton(onClick = {
                            images = images.filter { it.uri !in selectedItems }
                            saveImagesToPrefs(images)
                            selectedItems = emptySet()
                            selectionMode = false
                            showDeleteDialog = false
                        }) { Text("Удалить") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
                    }
                )
            }
        }
    ) { padding ->
        if (images.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Ты ещё не добавил ни одной фотографии", fontSize = 18.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(images) { image ->
                    val isSelected = selectionMode && selectedItems.contains(image.uri)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.pointerInput(image) {
                            detectTapGestures(
                                onLongPress = {
                                    selectionMode = true
                                    selectedItems = selectedItems + image.uri
                                },
                                onTap = {
                                    if (selectionMode) {
                                        selectedItems = if (isSelected) {
                                            selectedItems - image.uri
                                        } else {
                                            selectedItems + image.uri
                                        }
                                        if (selectedItems.isEmpty()) selectionMode = false
                                    } else {
                                        selectedImage = image
                                    }
                                }
                            )
                        }
                    ) {
                        Box {
                            Image(
                                painter = rememberAsyncImagePainter(model = image.uri),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(Color(0x44000000))
                                )
                            }
                        }

                        if (image.title.isNotBlank()) {
                            Text(
                                text = image.title,
                                modifier = Modifier.padding(top = 4.dp),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            selectedImage?.let { image ->
                FullscreenImageDialog(
                    image = image,
                    onDismiss = { selectedImage = null },
                    onDelete = {
                        images = images.filter { it.uri != image.uri }
                        saveImagesToPrefs(images)
                        selectedImage = null
                    },
                    onRename = { newTitle ->
                        images = images.map {
                            if (it.uri == image.uri) it.copy(title = newTitle) else it
                        }
                        selectedImage = selectedImage?.copy(title = newTitle)
                        saveImagesToPrefs(images)
                    }
                )
            }
        }
    }
}

@Composable
fun FullscreenImageDialog(
    image: GalleryImage,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit
) {
    var isRenaming by remember { mutableStateOf(false) }
    var tempTitle by remember { mutableStateOf(TextFieldValue(image.title)) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = image.uri),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            if (image.title.isNotBlank()) {
                Text(
                    text = image.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (isRenaming) {
                OutlinedTextField(
                    value = tempTitle,
                    onValueChange = {
                        if (it.text.length <= 20) tempTitle = it
                    },
                    label = { Text("Новое название") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = {
                        onRename(tempTitle.text)
                        isRenaming = false
                    }) { Text("Сохранить", fontSize = 14.sp) }

                    TextButton(onClick = {
                        tempTitle = TextFieldValue(image.title)
                        isRenaming = false
                    }) { Text("Отмена", fontSize = 14.sp) }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { isRenaming = true }) { Text("Переименовать", fontSize = 14.sp) }
                    TextButton(onClick = { showDeleteDialog = true }) { Text("Удалить", fontSize = 14.sp) }
                    TextButton(onClick = onDismiss) { Text("Закрыть", fontSize = 14.sp) }
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Удаление") },
                text = { Text("Вы уверены, что хотите удалить фото?") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }) { Text("Удалить") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
                }
            )
        }
    }
}
