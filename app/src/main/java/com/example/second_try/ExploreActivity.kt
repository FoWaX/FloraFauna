package com.example.second_try

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.second_try.ui.theme.Second_tryTheme
import com.example.second_try.ui.components.AppTopBar
import androidx.compose.foundation.background

class ExploreActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Second_tryTheme {
                ExploreScreen(onNavigateBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(onNavigateBack: () -> Unit) {
    val reptiles = listOf("pres1", "pres2", "pres3", "pres4", "pres5", "pres6", "pres7", "pres8", "pres9", "pres10", "pres11", "pres12", "pres13", "pres14")
    val amphibians = listOf("zem1", "zem2", "zem3", "zem4", "zem5", "zem6", "zem7", "zem8", "zem9", "zem10", "zem11", "zem12")

    val reptilesTitles = mapOf(
        "pres1" to "Болотная черепаха",
        "pres2" to "Веретеница ломкая",
        "pres3" to "Веретиница ломкая",
        "pres4" to "Водяной уж",
        "pres5" to "Гадюка Никольского",
        "pres6" to "Гадюка Никольского",
        "pres7" to "Обыкновенная медянка",
        "pres8" to "Обыкновенный уж",
        "pres9" to "Обыкновенный уж",
        "pres10" to "Разноцветная ящурка",
        "pres11" to "Степная гадюка",
        "pres12" to "Степная гадюка",
        "pres13" to "Узорчатый полоз",
        "pres14" to "Узорчатый полоз",
    )

    val amphibiansTitles = mapOf(
        "zem1" to "Зеленая жаба",
        "zem2" to "Зеленая жаба",
        "zem3" to "Краснобрюхая жерлянка",
        "zem4" to "Краснобрюхая жерлянка",
        "zem5" to "Обыкновенная чесночница",
        "zem6" to "Тритон обыкновенный",
        "zem7" to "Тритон обыкновенный",
        "zem8" to "Озерная лягушка",
        "zem9" to "Озерная лягушка",
        "zem10" to "Озерная лягушка",
        "zem11" to "Остромордая лягушка",
        "zem12" to "Остромордая лягушка",
    )

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Познаем  новое",
                onBack = onNavigateBack
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                SectionBlock(
                    title = "Пресмыкающиеся",
                    pdfTitle = "Памятка для определения пресмыкающихся",
                    pdfResId = R.raw.oprrep,
                    imageIds = reptiles,
                    imageTitles = reptilesTitles
                )
            }
            item {
                SectionBlock(
                    title = "Земноводные",
                    pdfTitle = "Памятка для определения Земноводных",
                    pdfResId = R.raw.oprzem,
                    imageIds = amphibians,
                    imageTitles = amphibiansTitles
                )
            }
        }
    }
}

@Composable
fun SectionBlock(
    title: String,
    pdfTitle: String,
    pdfResId: Int,
    imageIds: List<String>,
    imageTitles: Map<String, String>
) {
    val context = LocalContext.current

    Column(modifier = Modifier.padding(16.dp)) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable {
                    val intent = Intent(context, PdfViewerActivity::class.java)
                    intent.putExtra("pdf_name", context.resources.getResourceEntryName(pdfResId))
                    context.startActivity(intent)
                },
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(pdfTitle, fontWeight = FontWeight.Medium, fontSize = 18.sp)
                Text("Нажмите, чтобы открыть PDF", fontSize = 14.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        ImageGrid(imageIds, imageTitles)
    }
}

@Composable
fun ImageGrid(imageIds: List<String>, imageTitles: Map<String, String>) {
    val context = LocalContext.current
    var selectedImage by remember { mutableStateOf<String?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .heightIn(max = 600.dp)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(imageIds) { imageName ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clickable { selectedImage = imageName },
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Image(
                    painter = painterResource(
                        id = getDrawableIdByName(imageName)
                    ),
                    contentDescription = imageTitles[imageName] ?: imageName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Когда выбранное изображение изменилось — сохраняем "просмотр" (скрытый флаг) в SharedPreferences
    selectedImage?.let { imageName ->
        // side-effect: записываем при первом показе диалога
        LaunchedEffect(imageName) {
            val prefs = context.getSharedPreferences("viewed_cards", Context.MODE_PRIVATE)
            val existing = prefs.getStringSet("viewed_set", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            if (!existing.contains(imageName)) {
                existing.add(imageName)
                prefs.edit().putStringSet("viewed_set", existing).apply()
            }
        }

        // Превью выбранного изображения (диалог)
        Dialog(onDismissRequest = { selectedImage = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { selectedImage = null } // Закрыть при тапе вне
            ) {
                Image(
                    painter = painterResource(id = getDrawableIdByName(imageName)),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                )
                Text(
                    text = imageTitles[imageName] ?: imageName,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .fillMaxWidth()
                        .padding(16.dp),
                    color = Color.White,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun getDrawableIdByName(name: String): Int {
    val context = LocalContext.current
    return remember(name) {
        context.resources.getIdentifier(name, "drawable", context.packageName)
    }
}
