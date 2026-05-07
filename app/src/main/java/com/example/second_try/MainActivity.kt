package com.example.second_try

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.second_try.ui.components.AppTopBar
import com.example.second_try.ui.theme.Second_tryTheme
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.database.FirebaseDatabase
import android.content.SharedPreferences
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.BorderStroke
import com.example.second_try.ui.theme.AppColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

lateinit var photoUri: Uri
lateinit var takePictureLauncher: ActivityResultLauncher<Uri>

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var profilePhotoUriState = mutableStateOf<Uri?>(null)
    lateinit var profileResultLauncher: ActivityResultLauncher<Intent>


    private fun launchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val photoFile = createImageFile()
            photoUri = FileProvider.getUriForFile(this, "$packageName.provider", photoFile)
            takePictureLauncher.launch(photoUri)
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1001)
        }
    }

    // Сохраняем фото локально для каждого пользователя
    private fun saveImageLocally(uri: Uri) {
        val user = auth.currentUser ?: return

        // сохраняем путь в локальную коллекцию
        val prefs = getSharedPreferences("gallery_${user.uid}", Context.MODE_PRIVATE)
        val existingPaths = prefs.getStringSet("image_paths", mutableSetOf()) ?: mutableSetOf()
        val updatedPaths = existingPaths.toMutableSet()
        updatedPaths.add("$uri;")
        prefs.edit().putStringSet("image_paths", updatedPaths).apply()

        // локальное сохранение количества (если тебе нужно для оффлайна)
        val achievementsPrefs = getSharedPreferences("achievements_prefs", MODE_PRIVATE)
        val photosSavedLocal = achievementsPrefs.getInt("photos_saved", 0) + 1
        achievementsPrefs.edit().putInt("photos_saved", photosSavedLocal).apply()

        // обновляем Firebase
        val dbRef = FirebaseDatabase.getInstance(
            "https://mental-health-72105-default-rtdb.europe-west1.firebasedatabase.app"
        ).getReference("Users").child(user.uid)

        dbRef.child("photosSaved").get().addOnSuccessListener { snapshot ->
            val currentCount = snapshot.getValue(Int::class.java) ?: 0
            dbRef.child("photosSaved").setValue(currentCount + 1)
        }

        Toast.makeText(this, "Фото сохранено!", Toast.LENGTH_SHORT).show()
    }


    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                Toast.makeText(this, "Фото успешно сделано!", Toast.LENGTH_SHORT).show()
                photoUri?.let {
                    saveImageLocally(it) // ✅ Сохраняем локально
                }
            } else {
                Toast.makeText(this, "Фото не было сделано.", Toast.LENGTH_SHORT).show()
            }
        }

        // Проверка авторизации пользователя
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            profileResultLauncher =
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == RESULT_OK) {
                        val prefs = getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)
                        val path = prefs.getString("profile_photo_path", null)
                        if (path != null && File(path).exists()) {
                            profilePhotoUriState.value = Uri.fromFile(File(path))
                        } else {
                            profilePhotoUriState.value = null
                        }
                        setContent {
                            Second_tryTheme {
                                MainScreen(
                                    onLogout = {
                                        FirebaseAuth.getInstance().signOut()
                                        startActivity(Intent(this, LoginActivity::class.java))
                                        finish()
                                    },
                                    onCameraClick = { launchCamera() }
                                )
                            }
                        }
                    }
                }
            setContent {
                Second_tryTheme {
                    MainScreen(
                        onLogout = {
                            auth.signOut()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        },
                        onCameraClick = {
                            launchCamera()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MainMenuButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.PrimaryGreen,
            contentColor = Color.White
        ),
        border = BorderStroke(1.dp, AppColors.BorderDark),
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier, onLogout: () -> Unit, onCameraClick: () -> Unit) {
    val context = LocalContext.current
    var profilePhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Загрузка фото из SharedPreferences
    val prefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)

    // Загружаем фото при старте и при возврате на экран
    LaunchedEffect(Unit) {
        val path = prefs.getString("profile_photo_path", null)
        if (path != null && File(path).exists()) {
            profilePhotoUri = Uri.fromFile(File(path))
        } else {
            profilePhotoUri = null
        }
    }

    // Слушатель (дополнительно, на случай, если изменения происходят в реальном времени)
    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "profile_photo_path") {
                val newPath = prefs.getString("profile_photo_path", null)
                profilePhotoUri = if (newPath != null && File(newPath).exists()) {
                    Uri.fromFile(File(newPath))
                } else null
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Главное меню",
                actions = {
                    val mainActivity = context as? MainActivity

                    IconButton(
                        onClick = {
                            context.startActivity(
                                Intent(context, LeaderboardActivity::class.java)
                            )
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_trophey),
                            contentDescription = "Рейтинг",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            mainActivity?.let {
                                val intent = Intent(it, ProfileActivity::class.java)
                                it.profileResultLauncher.launch(intent)
                            }
                        }
                    ) {
                        val painter = if (profilePhotoUri != null)
                            coil.compose.rememberAsyncImagePainter(profilePhotoUri)
                        else
                            painterResource(id = R.drawable.ic_orig)

                        Image(
                            painter = painter,
                            contentDescription = "Профиль",
                            modifier = Modifier
                                .size(40.dp)
                                .padding(4.dp)
                        )
                    }


                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Чудеса природы",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextGreen
                )

                // 🔥 Секретная кнопка поверх текста
                InvisibleSecretButton(
                    secretKey = "secret2Found",
                    modifier = Modifier
                        .matchParentSize() // перекрывает всю надпись
                )
            }


            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = 0.dp)
                ) {
                    MainMenuButton(
                        text = "Познаем новое",
                        onClick = { context.startActivity(Intent(context, ExploreActivity::class.java)) }
                    )

                    MainMenuButton(
                        text = "Твоя коллекция фотографий",
                        onClick = { context.startActivity(Intent(context, GalleryActivity::class.java)) }
                    )

                    MainMenuButton(
                        text = "Календарь",
                        onClick = { context.startActivity(Intent(context, CalendarActivity::class.java)) }
                    )

                    MainMenuButton(
                        text = "Твои достижения",
                        onClick = { context.startActivity(Intent(context, AchievementsActivity::class.java)) }
                    )

                    MainMenuButton(
                        text = "Викторины",
                        onClick = { context.startActivity(Intent(context, TasksActivity::class.java)) }
                    )
                }

                CharacterWithGreeting(
                    greeting = "Привет, любитель познавать окружающий мир!",
                    modifier = Modifier.align(Alignment.BottomStart)
                )

                Button(
                    onClick = onLogout,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.DarkGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Выйти",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                FloatingActionButton(
                    onClick = onCameraClick,
                    containerColor = AppColors.Coral,
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = "Камера",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CharacterWithGreeting(greeting: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.BottomStart
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Bottom
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.LightGreen),
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            ) {
                Text(
                    text = greeting,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Start
                )
            }

            Image(
                painter = painterResource(id = R.drawable.character),
                contentDescription = "Персонаж",
                modifier = Modifier
                    .size(160.dp)
                    .padding(start = 16.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    Second_tryTheme {
        MainScreen(onLogout = {}, onCameraClick = {})
    }
}
