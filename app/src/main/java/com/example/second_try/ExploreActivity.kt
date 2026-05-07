package com.example.second_try

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.second_try.ui.components.AppTopBar
import com.example.second_try.ui.theme.Second_tryTheme
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.saveable.rememberSaveable
import java.text.Collator
import java.util.Locale

// ------------------------ МОДЕЛИ ------------------------

data class AnimalTopic(
    val title: String,
    val imageResName: String, // имя drawable, например "ph_0001_1"
    val viewedId: String,     // id для записей viewed_set: "zem1" или "pres1"
    val text: String
)

data class AnimalSection(
    val title: String,
    val items: List<AnimalTopic>
)

data class AnimalClassBlock(
    val title: String,
    val sections: List<AnimalSection>
)

enum class CatalogSource {
    MAIN,        // buildAnimalData
    DANGEROUS,   // buildDangerousAnimalsData
    MAMMALS,      // buildMammalsData
    BIRDS
}

data class SearchIndexItem(
    val source: CatalogSource,
    val classTitle: String?,   // для птиц может быть null
    val sectionTitle: String?, // для птиц может быть null
    val topic: AnimalTopic,
    val blockIndex: Int? = null,
    val sectionIndex: Int? = null,
    val topicIndex: Int
)

data class IndexedMammalTopic(
    val blockIndex: Int,
    val sectionIndex: Int,
    val topicIndex: Int,
    val topic: AnimalTopic
)

// ------------------------ Навигация (локальная) ------------------------

// внутри sealed class Screen — замените/добавьте:
sealed class Screen {
    object Home : Screen()
    object ClassesMenu : Screen()
    // уберите DangerousPlaceholder или замените:
    object DangerousHome : Screen()
    data class DangerousSections(val blockIndex: Int) : Screen()
    data class DangerousTopicList(val blockIndex: Int, val sectionIndex: Int) : Screen()
    data class DangerousTopicDetail(val blockIndex: Int, val sectionIndex: Int, val topicIndex: Int) : Screen()

    data class MammalsSections(val blockIndex: Int) : Screen()
    data class MammalsTopicList(val blockIndex: Int, val sectionIndex: Int) : Screen()
    data class MammalsTopicDetail(val blockIndex: Int, val sectionIndex: Int, val topicIndex: Int) : Screen()
    object MammalsIntroDetail : Screen()
    object MammalsListDetail : Screen()

    object BirdsList : Screen()
    data class BirdsDetail(val topicIndex: Int) : Screen()

    data class Sections(val classIndex: Int) : Screen()
    data class TopicList(val classIndex: Int, val sectionIndex: Int) : Screen()
    data class TopicDetail(val classIndex: Int, val sectionIndex: Int, val topicIndex: Int) : Screen()
}


// ------------------------ ACTIVITY ------------------------

class ExploreActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Second_tryTheme {
                ExploreHost()
            }
        }
    }
}

// ------------------------ HOST (управляет экранами) ------------------------

@Composable
fun ExploreHost() {
    val context = LocalContext.current
    val data = remember { buildAnimalData(context) }
    val dangerousData = remember { buildDangerousAnimalsData(context) }
    val mammalsData = remember { buildMammalsData(context) }
    val birdsData = remember { buildBirdsData(context) }

    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    val searchIndex = remember(data, dangerousData, mammalsData, birdsData) {
        buildSearchIndex(
            mainData = data,
            dangerousData = dangerousData,
            mammalsData = mammalsData,
            birdsData = birdsData
        )
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }

    val searchResult = filterSearchItems(searchIndex, searchQuery, limit = 15)

    when (val s = screen) {
        is Screen.Home -> {
            ExploreHomeScreen(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                searchResult = searchResult,
                onSearchItemClick = { item ->
                    when (item.source) {
                        CatalogSource.MAIN -> {
                            screen = Screen.TopicDetail(
                                classIndex = item.blockIndex ?: 0,
                                sectionIndex = item.sectionIndex ?: 0,
                                topicIndex = item.topicIndex
                            )
                        }
                        CatalogSource.DANGEROUS -> {
                            screen = Screen.DangerousTopicDetail(
                                blockIndex = item.blockIndex ?: 0,
                                sectionIndex = item.sectionIndex ?: 0,
                                topicIndex = item.topicIndex
                            )
                        }
                        CatalogSource.MAMMALS -> {
                            screen = Screen.MammalsTopicDetail(
                                blockIndex = item.blockIndex ?: 0,
                                sectionIndex = item.sectionIndex ?: 0,
                                topicIndex = item.topicIndex
                            )
                        }
                        CatalogSource.BIRDS -> {
                            screen = Screen.BirdsDetail(item.topicIndex)
                        }
                    }
                },
                onOpenClasses = { screen = Screen.ClassesMenu },
                onOpenDangerous = { screen = Screen.DangerousHome },
                onOpenMammals = { screen = Screen.MammalsSections(0) },
                onOpenBirds = { screen = Screen.BirdsList }
            )
        }
        is Screen.ClassesMenu -> {
            AnimalClassesScreen(
                onBack = { screen = Screen.Home },
                onOpenClass = { classIndex -> screen = Screen.Sections(classIndex) },
                data = data
            )
        }

        // ---------- Опасные животные ----------
        is Screen.DangerousHome -> {
            DangerousHomeScreen(
                onBack = { screen = Screen.Home },
                onOpenCategory = { idx -> screen = Screen.DangerousSections(idx) },
                data = dangerousData
            )
        }
        is Screen.DangerousSections -> {
            val b = s.blockIndex
            DangerousSectionsScreen(
                onBack = { screen = Screen.DangerousHome },
                onOpenSection = { sectionIndex -> screen = Screen.DangerousTopicList(b, sectionIndex) },
                classBlock = dangerousData[b]
            )
        }
        is Screen.DangerousTopicList -> {
            val b = s.blockIndex
            DangerousTopicListScreen(
                onBack = { screen = Screen.DangerousSections(b) },
                onOpenTopic = { topicIndex -> screen = Screen.DangerousTopicDetail(b, s.sectionIndex, topicIndex) },
                section = dangerousData[b].sections[s.sectionIndex]
            )
        }
        is Screen.DangerousTopicDetail -> {
            val topic = dangerousData[s.blockIndex].sections[s.sectionIndex].items[s.topicIndex]
            DangerousTopicDetailScreen(
                onBack = { screen = Screen.DangerousTopicList(s.blockIndex, s.sectionIndex) },
                topic = topic
            )
        }

        // ---------- Пресмыкающиеся / Земноводные (уже было) ----------
        is Screen.Sections -> {
            val idx = s.classIndex
            AnimalSectionsScreen(
                onBack = { screen = Screen.ClassesMenu },
                onOpenSection = { sectionIndex -> screen = Screen.TopicList(idx, sectionIndex) },
                classBlock = data[idx]
            )
        }
        is Screen.TopicList -> {
            TopicListScreen(
                onBack = { screen = Screen.Sections(s.classIndex) },
                onOpenTopic = { topicIndex -> screen = Screen.TopicDetail(s.classIndex, s.sectionIndex, topicIndex) },
                section = data[s.classIndex].sections[s.sectionIndex]
            )
        }
        is Screen.TopicDetail -> {
            val topic = data[s.classIndex].sections[s.sectionIndex].items[s.topicIndex]
            TopicDetailScreen(
                onBack = { screen = Screen.TopicList(s.classIndex, s.sectionIndex) },
                topic = topic
            )
        }

        //------------------Млекопитающие---------------------
        is Screen.MammalsSections -> {
            MammalsAlphabetScreen(
                onBack = { screen = Screen.Home },
                onOpenIntro = {
                    screen = Screen.MammalsIntroDetail
                },
                onOpenList = {
                    screen = Screen.MammalsListDetail
                },
                onOpenTopic = { blockIndex, sectionIndex, topicIndex ->
                    screen = Screen.MammalsTopicDetail(blockIndex, sectionIndex, topicIndex)
                },
                data = mammalsData
            )
        }
        is Screen.MammalsTopicList -> {
            val b = s.blockIndex
            MammalsTopicListScreen(
                onBack = { screen = Screen.MammalsSections(b) },
                onOpenTopic = { topicIndex -> screen = Screen.MammalsTopicDetail(b, s.sectionIndex, topicIndex) },
                section = mammalsData[b].sections[s.sectionIndex]
            )
        }
        is Screen.MammalsTopicDetail -> {
            val topic = mammalsData[s.blockIndex].sections[s.sectionIndex].items[s.topicIndex]
            MammalsTopicDetailScreen(
                onBack = { screen = Screen.MammalsSections(0) },
                topic = topic
            )
        }

        Screen.MammalsIntroDetail -> {
            MammalsTopicDetailScreen(
                onBack = { screen = Screen.MammalsSections(0) },
                topic = MAMMALS_INTRO_TOPIC
            )
        }

        Screen.MammalsListDetail -> {
            MammalsTopicDetailScreen(
                onBack = { screen = Screen.MammalsSections(0) },
                topic = MAMMALS_LIST_TOPIC
            )
        }

        //--------------------Птицы-----------------
        is Screen.BirdsList -> {
            BirdsTopicListScreen(
                onBack = { screen = Screen.Home },
                onOpenTopic = { topicIndex -> screen = Screen.BirdsDetail(topicIndex) },
                items = birdsData
            )
        }

        is Screen.BirdsDetail -> {
            val topic = birdsData[s.topicIndex]
            BirdsTopicDetailScreen(
                onBack = { screen = Screen.BirdsList },
                topic = topic
            )
        }
    }
}


// ------------------------ ЭКРАНЫ ------------------------

@Composable
fun TopicPreviewText(topic: AnimalTopic) {
    Text(
        text = topic.text.take(90) + if (topic.text.length > 90) "..." else "",
        fontSize = 14.sp,
        lineHeight = 18.sp,
        color = Color.Gray
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreHomeScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResult: SearchFilterResult,
    onSearchItemClick: (SearchIndexItem) -> Unit,
    onOpenClasses: () -> Unit,
    onOpenDangerous: () -> Unit,
    onOpenMammals: () -> Unit,
    onOpenBirds: () -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(title = "Познаем новое", onBack = {})
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // -------- Поиск сверху --------
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Поиск животного") },
                placeholder = { Text("Введите название...") }
            )

            // -------- Результаты поиска --------
            if (searchQuery.isNotBlank()) {
                Spacer(Modifier.height(12.dp))

                if (searchResult.items.isEmpty()) {
                    Text(
                        "Ничего не найдено",
                        color = Color.Gray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                } else {
                    SearchResultsBlock(
                        items = searchResult.items,
                        hasMore = searchResult.hasMore,
                        onItemClick = onSearchItemClick
                    )
                }

                Spacer(Modifier.height(16.dp))
            } else {
                Spacer(Modifier.height(16.dp))
            }

            // -------- Кнопки разделов --------
            Button(
                onClick = onOpenClasses,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Text("Пресмыкающиеся и земноводные", fontSize = 18.sp)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onOpenDangerous,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Text("Опасные животные", fontSize = 18.sp)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onOpenMammals,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Text("Млекопитающие", fontSize = 18.sp)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onOpenBirds,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Text("Птицы ТОП 25", fontSize = 18.sp)
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Здесь находятся памятки и темы. Нажмите на кнопку чтобы перейти к разделам.",
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun BirdsTopicListScreen(
    onBack: () -> Unit,
    onOpenTopic: (Int) -> Unit,
    items: List<AnimalTopic>
) {
    Scaffold(topBar = { AppTopBar(title = "Птицы", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            items.forEachIndexed { idx, topic ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onOpenTopic(idx) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        val ctx = LocalContext.current
                        val resId = ctx.resources.getIdentifier(topic.imageResName, "drawable", ctx.packageName)

                        if (resId != 0) {
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = topic.title,
                                modifier = Modifier.size(80.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                                Text("Нет фото", fontSize = 12.sp, color = Color.Gray)
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column {
                            Text(topic.title, fontWeight = FontWeight.Medium)
                            TopicPreviewText(topic)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BirdsTopicDetailScreen(onBack: () -> Unit, topic: AnimalTopic) {
    // Переиспользуем уже готовую карточку
    TopicDetailScreen(onBack = onBack, topic = topic)
}

private fun flattenMammalsData(data: List<AnimalClassBlock>): List<IndexedMammalTopic> {
    return data.flatMapIndexed { blockIndex, block ->
        block.sections.flatMapIndexed { sectionIndex, section ->
            section.items.mapIndexed { topicIndex, topic ->
                IndexedMammalTopic(
                    blockIndex = blockIndex,
                    sectionIndex = sectionIndex,
                    topicIndex = topicIndex,
                    topic = topic
                )
            }
        }
    }
}

private fun getMammalFirstLetter(title: String): String {
    return title
        .trim()
        .firstOrNull { it.isLetter() }
        ?.uppercaseChar()
        ?.toString()
        ?: "#"
}

private const val MAMMALS_INTRO_TEXT = """
Дмитрий Сергеевич Худяков – легендарный саратовский краевед, популяризатор сведений о природе и истории края, создатель и бессменный ведущий телепередачи «Не за тридевять земель», которая с 1960 г. выходила в эфир 1697 раз! Книга «Наши звери…» в доступной детям форме подробно рассказывает о всех млекопитающих Саратовской области. В Приложении список сокращен. Также использованы сведения из книги «Млекопитающие Севера Нижнего Поволжья. Книга 1», изданной учеными СГУ в 2009 г. Тексты-описания очень удобно использовать в качестве докладов на уроках, добавив 2-3 иллюстрации. Авторы фото млекопитающих – преподаватели Саратовского госуниверситета, учителя биологии, краеведы-любители. Иллюстрации, взятые из Интернета, помечены зеленой полоской снизу.
"""

private val MAMMALS_INTRO_TOPIC = AnimalTopic(
    title = "Легендарный краевед",
    imageResName = "ph_0075_1",
    viewedId = "mammals_intro",
    text = MAMMALS_INTRO_TEXT.trimIndent()
)

private const val MAMMALS_LIST_TEXT = """
а здесь ты увидишь сколько и какие млекопитающие есть в нашем разделе про них!
"""

private val MAMMALS_LIST_TOPIC = AnimalTopic(
    title = "список млекопитающих",
    imageResName = "ph_0076_1",
    viewedId = "mammals_list",
    text = MAMMALS_LIST_TEXT.trimIndent()
)
@Composable
fun MammalsInfoCard(
    topic: AnimalTopic,
    onClick: () -> Unit
) {
    val ctx = LocalContext.current
    val resId = ctx.resources.getIdentifier(
        topic.imageResName,
        "drawable",
        ctx.packageName
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6E6))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (resId != 0) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = topic.title,
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет фото",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = topic.title,
                    fontWeight = FontWeight.Medium
                )

                TopicPreviewText(topic)
            }
        }
    }
}

@Composable
fun MammalsAlphabetScreen(
    onBack: () -> Unit,
    onOpenIntro: () -> Unit,
    onOpenList: () -> Unit,
    onOpenTopic: (blockIndex: Int, sectionIndex: Int, topicIndex: Int) -> Unit,
    data: List<AnimalClassBlock>
) {
    val collator = remember {
        Collator.getInstance(Locale("ru", "RU"))
    }

    val groupedTopics = remember(data) {
        flattenMammalsData(data)
            .sortedWith { first, second ->
                collator.compare(first.topic.title, second.topic.title)
            }
            .groupBy { item ->
                getMammalFirstLetter(item.topic.title)
            }
    }

    val letters = remember(groupedTopics) {
        groupedTopics.keys.sortedWith { first, second ->
            collator.compare(first, second)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(title = "Млекопитающие", onBack = onBack)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "О чем тут?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            MammalsInfoCard(
                topic = MAMMALS_INTRO_TOPIC,
                onClick = onOpenIntro
            )

            MammalsInfoCard(
                topic = MAMMALS_LIST_TOPIC,
                onClick = onOpenList
            )

            Spacer(Modifier.height(8.dp))

            letters.forEach { letter ->
                Text(
                    text = letter,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )

                groupedTopics[letter]?.forEach { item ->
                    val topic = item.topic

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                onOpenTopic(
                                    item.blockIndex,
                                    item.sectionIndex,
                                    item.topicIndex
                                )
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6E6))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val ctx = LocalContext.current
                            val resId = ctx.resources.getIdentifier(
                                topic.imageResName,
                                "drawable",
                                ctx.packageName
                            )

                            if (resId != 0) {
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = topic.title,
                                    modifier = Modifier.size(80.dp),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(80.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Нет фото",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = topic.title,
                                    fontWeight = FontWeight.Medium
                                )

                                TopicPreviewText(topic)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MammalsHomeScreen(onBack: () -> Unit, onOpenCategory: (Int) -> Unit, data: List<AnimalClassBlock>) {
    Scaffold(topBar = { AppTopBar(title = "Млекопитающие", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Выберите отряд", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(12.dp))
            data.forEachIndexed { idx, block ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onOpenCategory(idx) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(block.title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MammalsSectionsScreen(onBack: () -> Unit, onOpenSection: (Int) -> Unit, classBlock: AnimalClassBlock) {
    AnimalSectionsScreen(onBack = onBack, onOpenSection = onOpenSection, classBlock = classBlock)
}

@Composable
fun MammalsTopicListScreen(onBack: () -> Unit, onOpenTopic: (Int) -> Unit, section: AnimalSection) {
    TopicListScreen(onBack = onBack, onOpenTopic = onOpenTopic, section = section)
}

@Composable
fun MammalsTopicDetailScreen(onBack: () -> Unit, topic: AnimalTopic) {
    TopicDetailScreen(onBack = onBack, topic = topic)
}

@Composable
fun DangerousPlaceholderScreen(onBack: () -> Unit) {
    Scaffold(topBar = { AppTopBar(title = "Опасные животные", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Text("Раздел «Опасные животные» — пока заглушка.", fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            Text("Позже сюда будет добавлен текст и фотографии.", color = Color.Gray)
        }
    }
}

@Composable
fun AnimalClassesScreen(onBack: () -> Unit, onOpenClass: (Int) -> Unit, data: List<AnimalClassBlock>) {
    Scaffold(topBar = { AppTopBar(title = "Пресмыкающиеся и Земноводные", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // PDF cards moved here
            PdfCardsBlock()

            Spacer(Modifier.height(12.dp))

            data.forEachIndexed { index, cls ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onOpenClass(index) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(cls.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AnimalSectionsScreen(onBack: () -> Unit, onOpenSection: (Int) -> Unit, classBlock: AnimalClassBlock) {
    Scaffold(topBar = { AppTopBar(title = classBlock.title, onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            classBlock.sections.forEachIndexed { idx, section ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onOpenSection(idx) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F7EF))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(section.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text("${section.items.size} тем", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun TopicListScreen(onBack: () -> Unit, onOpenTopic: (Int) -> Unit, section: AnimalSection) {
    Scaffold(topBar = { AppTopBar(title = section.title, onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            section.items.forEachIndexed { idx, topic ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onOpenTopic(idx) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6E6))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        val ctx = LocalContext.current
                        val resId = ctx.resources.getIdentifier(topic.imageResName, "drawable", ctx.packageName)
                        if (resId != 0) {
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = topic.title,
                                modifier = Modifier.size(80.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                                Text("Нет фото", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(topic.title, fontWeight = FontWeight.Medium)
                            TopicPreviewText(topic)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopicDetailScreen(onBack: () -> Unit, topic: AnimalTopic) {
    val context = LocalContext.current
    var showFullscreen by remember { mutableStateOf(false) }
    val imageResId = remember(topic.imageResName) {
        context.resources.getIdentifier(topic.imageResName, "drawable", context.packageName)
    }

    Scaffold(topBar = { AppTopBar(title = topic.title, onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // image preview (click -> fullscreen)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp)
                    .clickable { showFullscreen = true },
                colors = CardDefaults.cardColors(containerColor = Color.LightGray)
            ) {
                if (imageResId != 0) {
                    Image(
                        painter = painterResource(id = imageResId),
                        contentDescription = topic.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Изображение отсутствует", color = Color.DarkGray)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(topic.title, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text(topic.text, fontSize = 16.sp, lineHeight = 22.sp)
        }
    }

    if (showFullscreen) {
        FullscreenZoomImageDialog(
            imageResId = imageResId,
            onDismiss = { showFullscreen = false },
            onShown = {
                // сохранение просмотра в SharedPreferences
                val prefs = context.getSharedPreferences("viewed_cards", Context.MODE_PRIVATE)
                val existing = prefs.getStringSet("viewed_set", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                if (!existing.contains(topic.viewedId)) {
                    existing.add(topic.viewedId)
                    prefs.edit().putStringSet("viewed_set", existing).apply()
                }
            }
        )
    }
}

// ------------------------ ЭКРАНЫ: Опасные животные ------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DangerousHomeScreen(onBack: () -> Unit, onOpenCategory: (Int) -> Unit, data: List<AnimalClassBlock>) {
    Scaffold(topBar = { AppTopBar(title = "Опасные животные", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Выберите категорию", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(12.dp))
            data.forEachIndexed { idx, block ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onOpenCategory(idx) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(block.title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DangerousSectionsScreen(onBack: () -> Unit, onOpenSection: (Int) -> Unit, classBlock: AnimalClassBlock) {
    Scaffold(topBar = { AppTopBar(title = classBlock.title, onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            classBlock.sections.forEachIndexed { idx, section ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onOpenSection(idx) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F7EF))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(section.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text("${section.items.size} тем", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun DangerousTopicListScreen(onBack: () -> Unit, onOpenTopic: (Int) -> Unit, section: AnimalSection) {
    // можно переиспользовать TopicListScreen, но для автономности — дублируем
    Scaffold(topBar = { AppTopBar(title = section.title, onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            section.items.forEachIndexed { idx, topic ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onOpenTopic(idx) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6E6))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        val ctx = LocalContext.current
                        val resId = ctx.resources.getIdentifier(topic.imageResName, "drawable", ctx.packageName)
                        if (resId != 0) {
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = topic.title,
                                modifier = Modifier.size(80.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                                Text("Нет фото", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(topic.title, fontWeight = FontWeight.Medium)
                            TopicPreviewText(topic)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DangerousTopicDetailScreen(onBack: () -> Unit, topic: AnimalTopic) {
    // Можно переиспользовать TopicDetailScreen — этот компонент совпадает с ним,
    // но вынесен отдельно чтобы не нарушать существующий поток.
    val context = LocalContext.current
    var showFullscreen by remember { mutableStateOf(false) }
    val imageResId = remember(topic.imageResName) {
        context.resources.getIdentifier(topic.imageResName, "drawable", context.packageName)
    }

    Scaffold(topBar = { AppTopBar(title = topic.title, onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp)
                    .clickable { showFullscreen = true },
                colors = CardDefaults.cardColors(containerColor = Color.LightGray)
            ) {
                if (imageResId != 0) {
                    Image(
                        painter = painterResource(id = imageResId),
                        contentDescription = topic.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Изображение отсутствует", color = Color.DarkGray)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(topic.title, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text(topic.text, fontSize = 16.sp, lineHeight = 22.sp)
        }
    }

    if (showFullscreen) {
        FullscreenZoomImageDialog(
            imageResId = imageResId,
            onDismiss = { showFullscreen = false },
            onShown = {
                // при показе — можно сохранять просмотр в viewed_set (если нужно)
                val prefs = context.getSharedPreferences("viewed_cards", Context.MODE_PRIVATE)
                val existing = prefs.getStringSet("viewed_set", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                if (!existing.contains(topic.viewedId)) {
                    existing.add(topic.viewedId)
                    prefs.edit().putStringSet("viewed_set", existing).apply()
                }
            }
        )
    }
}


// ------------------------ PDF BLOCK ------------------------

@Composable
fun PdfCardsBlock() {
    val context = LocalContext.current
    Column {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable {
                    val intent = Intent(context, PdfViewerActivity::class.java)
                    intent.putExtra("pdf_name", context.resources.getResourceEntryName(R.raw.pdf_presm))
                    context.startActivity(intent)
                },
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Определитель пресмыкающихся", fontWeight = FontWeight.Medium, fontSize = 18.sp)
                Text("Нажмите, чтобы открыть PDF", fontSize = 14.sp, color = Color.Gray)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable {
                    val intent = Intent(context, PdfViewerActivity::class.java)
                    intent.putExtra("pdf_name", context.resources.getResourceEntryName(R.raw.oprzem))
                    context.startActivity(intent)
                },
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Определитель земноводных", fontWeight = FontWeight.Medium, fontSize = 18.sp)
                Text("Нажмите, чтобы открыть PDF", fontSize = 14.sp, color = Color.Gray)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable {
                    val intent = Intent(context, PdfViewerActivity::class.java)
                    intent.putExtra("pdf_name", context.resources.getResourceEntryName(R.raw.list_presm))
                    context.startActivity(intent)
                },
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Список пресмыкающихся", fontWeight = FontWeight.Medium, fontSize = 18.sp)
                Text("Нажмите, чтобы открыть PDF", fontSize = 14.sp, color = Color.Gray)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable {
                    val intent = Intent(context, PdfViewerActivity::class.java)
                    intent.putExtra("pdf_name", context.resources.getResourceEntryName(R.raw.list_zem))
                    context.startActivity(intent)
                },
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Список земноводных", fontWeight = FontWeight.Medium, fontSize = 18.sp)
                Text("Нажмите, чтобы открыть PDF", fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}

// ------------------------ FULLSCREEN ZOOM IMAGE DIALOG -----------------------

@Composable
fun FullscreenZoomImageDialog(
    imageResId: Int,
    onDismiss: () -> Unit,
    onShown: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) { onShown() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tap ->
                            if (scale > 1f) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                scale = 2.5f
                                offsetX = -(tap.x - size.width / 2f)
                                offsetY = -(tap.y - size.height / 2f)
                            }
                        },
                        onTap = { onDismiss() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (imageResId != 0) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                ) {

                    Image(
                        painter = painterResource(id = imageResId),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    offsetX += pan.x
                                    offsetY += pan.y
                                }
                            }
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                            }
                    )
                }

            } else {
                Text("Изображение недоступно", color = Color.White)
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Text(
                    "Двойной тап — зум\nТап по фону — закрыть",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
            }
        }
    }
}



// ------------------------ ДАННЫЕ (полная иерархия, тексты из твоего сообщения) ------------------------

    fun buildAnimalData(context: Context): List<AnimalClassBlock> {
    fun img(name: String) = name // сохраняем имя, разрешение id делается при показе

    // Тексты взяты из того, что ты прислал (укорочены/перенесены сюда).
    val amphibians = AnimalClassBlock(
        title = "Класс Земноводные",
        sections = listOf(
            AnimalSection(
                title = "Отряд Бесхвостые",
                items = listOf(
                    AnimalTopic(
                        title = "Зеленая жаба",
                        imageResName = img("ph_0001_1"),
                        viewedId = "zem1",
                        text = """
Многочисленный распространенный вид Саратовской области. Длина тела 8-10 см. Кожа суховатая, бугорчатая, с хорошо заметными зелеными пятнами. Сзади глаз крупные вздутые околоушные железы (только у жаб). Еще отличие жаб от лягушек: на суше они ходят, а лягушки прыгают. Предпочитает открытые места, часто встречается в городе, на даче, в сельской местности, уходит далеко от водоема. Активна в темное время. Ест разнообразных мелких наземных беспозвоночных (жуков, клопов, многоножек, червей и др.) даже тех, у которых предупреждающая окраска, примерно 20-30 штук в сутки. Этим приносят пользу человеку. Ими питаются ужи, хищные птицы и вороны, ежи, куницы, выдра, гадюки, серая крыса и др. Зимуют на суше, в апреле приходят в водоемы, откладывают 2-10 тысяч икринок (диаметр 2 мм) в виде слизистого шнура с икринками в два ряда. После откладывания икры самка уходит от водоема, а самец охраняет кладку. Развитие через стадию головастика. В природе живут до 10 лет.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Ядовитые железы",
                        imageResName = img("ph_0001_2"),
                        viewedId = "zem1_j1",
                        text = """
За глазами у жабы околоушные железы (паротиды), в них содержатся ядовитые вещества, вызывающие раздражение кожи и сильное раздражение слизистых оболочек у человека, а у мелких животных вызывают нарушения сердечно-сосудистой деятельности. Если вы потрогали жабу, надо тщательно вымыть руки!
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Резонаторы жабы",
                        imageResName = img("ph_0001_3"),
                        viewedId = "zem1_r1",
                        text = """
После зимовки самцы жаб приходят в водоем первыми. В брачный период (апрель-май) они привлекают самок специфическими звуками. Горловой мешок (у жаб один) – резонатор – многократно усиливает эти звуки.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Озерная лягушка",
                        imageResName = img("ph_0003_2"),
                        viewedId = "zem3",
                        text = """
Самый распространенный вид амфибий Саратовской области. Встречаются по берегам крупных рек, в малых реках, старицах, прудах. По размеру это самые крупные амфибии области (15-18 см). Зимуют на дне водоема, впадают в анабиоз, дышат кожей. Из спячки выходят в апреле, период икрометания до 1,5 месяцев. Самки откладывают от 700 до нескольких тысяч икринок (диаметр 7 мм) порциями, в виде шаровидных скоплений, прикрепленных к водным растениям. Из яиц выходят личинки с неразвитым ртом. Достигнув размера 15 мм, после прорыва рта, питаются сами одноклеточными, мелкими ракообразными и др. Через 80 дней завершается превращение хвостатого головастика в бесхвостого лягушонка 18-28 мм. На зимовку уходят в сентябре-октябре. Питаются лягушки утром и днем, на суше и в воде (в основном насекомыми, но могут поедать червей, моллюсков, головастиков, мальков рыб). Крупные лягушки иногда нападают на других амфибий, молодых ужей, мелких птиц и грызунов. Их поедают: обыкновенный уж, хищные рыбы, цапли, выпи, хищные птицы, вороны, лисицы, ондатры, куницы. Лягушек отлавливают для научных целей как лабораторных животных.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Остромордая лягушка",
                        imageResName = img("ph_0004_1"),
                        viewedId = "zem11",
                        text = """
Обычный вид по всей территории Саратовской области. Обитает в пойменных лесах, влажных лугах, населенных пунктах. Длина тела до 8 см. Через глаз проходит темное височное пятно, вдоль спины светлая полоса. Активны утром и вечером, большую часть времени проводят на суше. Зимуют на суше, в норах грызунов, в подвалах, очень редко на дне водоемов. Рано выходят из спячки (конец марта-апрель), перемещаются к водоемам, и самцы начинают «петь» (резонаторы внутренние). Самки начинают откладывать икринки (6 мм) при температуре выше +5 одним скоплением (до 2 тысяч) на дне водоема в прогреваемых местах, через сутки масса всплывает. Самки уходят на сушу, а самцы охраняют кладки икры 7-14 дней. Головастики держатся стайками. Лягушата (15-20 мм) появляются в конце лета. В природе живут до 12 лет. Питаются чаще всего летающими насекомыми, резко прыгая за ними. Икру и личинок активно поедают водные животные, взрослых – змеи, озерная лягушка, хищные и околоводные птицы, лисицы.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Краснобрюхая жерлянка",
                        imageResName = img("ph_0002_1"),
                        viewedId = "zem2",
                        text = """
Обитают в прогреваемых заросших водоемах с медленным течением или стоячих. Активную жизнь проводят в основном в воде, их реже, чем других амфибий можно встретить на суше, не далее 4 м от водоема. В Правобережье встречаются чем в Левобережье. Длина тела до 6,5 см. Зимуют на суше, редко на дне водоемов. Просыпаются в середине апреля, приходят в водоем. Самка откладывает икринки (7-8 мм) порциями по 10-30 шт. на подводные растения. Бесхвостые сеголетки (10-12 мм) появляются в конце июля. На зимовку уходят в сентябре. Питаются водными беспозвоночными. Хищники не едят жерлянок.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Обыкновенная чесночница",
                        imageResName = img("ph_0006_1"),
                        viewedId = "zem5",
                        text = """
Небольшие амфибии (7-8 см) наиболее сухопутные из всех видов Саратовской области. Повсеместно встречаются в Правобережье в лиственных лесах, на полях, огородах, в Левобережье реже. Зимуют на суше, в норах грызунов, в подвалах. Просыпаются в конце марта. Размножаются в непересыхающих водоемах с чистой водой. Самки откладывают икринки (2 мм) в виде слизистых шнуров до 1 м длиной, в которых от 300 до 3 тысяч икринок. Головастики растут 60-140 суток. К моменту метаморфоза (превращения в бесхвостую стадию) головастик в полтора раза превышает размер самки (!). Это интересная особенность чесночниц. Молодые особи (15-35 мм) выходят из водоемов в июле-августе. Питаются вечером и ночью.
                        """.trimIndent()
                    ),

                )
            ),
                    AnimalSection(
                        title = "Отряд Хвостатые",
                        items = listOf(
                            AnimalTopic(
                                title = "Обыкновенный тритон",
                                imageResName = img("ph_0005_1"),
                                viewedId = "zem6",
                                text = """
Широко распространенный вид в Правобережье Саратовской области, в Левобережье редок, можно встретить и в г. Саратове. Длина тела до 11 см. Кожа гладкая или мелкозернистая. На голове темные полоски, хорошо видна темная полоса через глаз. Окраска светло-бурая, самки светлее. Зимуют на суше в норах, под пнями, в подвалах, могут собираться группами, даже с другими видами амфибий. Выходят в конце апреля, направляются в стоячие водоемы для размножения. В этот период у самца отрастает на спине гребень с выростами оранжевого цвета с голубой полоской. Самки откладывают овальные икринки (2 мм), каждую заворачивая каждую отдельно в листья подводных растений, всего до 600 штук за период размножения. У личинки, вышедшей из икринки, наружные перистые жабры, зачаточные конечности, нет рта, потом питаются мелкими водными беспозвоночными. Через 2-3 месяца личинки завершают превращение, формируются легкие. Молодые тритоны переходят к жизни на суше, активны вечером и ночью. Весной и осенью во время миграций тритонов можно встретить и днем. Питание: черви, мелкие насекомые, многоножки, пауки. Поедают тритонов: хищные рыбы, цапли, ужи, озерная лягушка. Погибают в холодное зимы, личинки – при пересыхании водоемов.
                        """.trimIndent()
                            )
                        )
                    )
        )
    )

    val reptiles = AnimalClassBlock(
        title = "Класс Пресмыкающиеся",
        sections = listOf(
            AnimalSection(
                title = "Отряд Черепахи",
                items = listOf(
                    AnimalTopic(
                        title = "Болотная черепаха",
                        imageResName = img("ph_0014_1"),
                        viewedId = "pres14",
                        text = """
Единственный вид черепах, обитающих в Саратовской области в дикой природе. Распространена по всей области, в водоемах со стоячей водой и богатой растительностью, в том числе искусственных (каналах, прудах). Размер панциря до 19 см.  Активны в утренние и вечерние часы, утром греются, при опасности ныряют, больше времени проводят в воде. На зимовку уходят в конце сентября на дно водоема, в ил. Выходят во второй половине апреля-мае. В конце мая самки копают умку на прогреваемых прибрежных участках. Вечером или ночью откладывают 8-16 овальных яиц (25-35 мм) в кожистой оболочке. Черепашата вылупляются через 80 дней (в августе -сентябре). Интересная особенность у этих черепах: соотношение полов вылупляющихся особей зависит от температуры окружающей среды: при 250 – самцы, 28 – наполовину самки и самцы, а при выше 30 – только самки. Черепашата держатся на берегу, в водоем придут на зимовку. Размножаться начинают в возрасте 6-8 лет. Живут черепахи в природе до 30 лет. Питаются в воде, их еда: водные улитки, насекомые, их личинки, головастики, реже рыба и водные растения. На взрослых черепах нападают хищные птицы и млекопитающие. Молодых поедают другие птицы (ворона, цапля), рыбы. Вид занесен в Международную Красную книгу.
                        """.trimIndent()
                    )
                )
            ),
            AnimalSection(
                title = "Отряд Ящерицы",
                items = listOf(
                    AnimalTopic(
                        title = "Прыткая ящерица",
                        imageResName = img("ph_0016_1"),
                        viewedId = "pres16",
                        text = """
Самый распространенный вид ящериц в Саратовской области. Встречаются в разных ландшафтах от лесных до полупустынных, в населенных пунктах. Длина тела с хвостом до 28 см. Окраска чаще коричневая, на спине хорошо видна темная полоса. Быстро бегает, может залезать на кусты и деревья, плавать. При опасности убегает, меняя направление, пойманная кусается, шипит, отбрасывает хвост. На зимовку в норы уходят в конце сентября, вход забивают листьями и землей. Выходят в начале апреля. В брачный период окраска у самца ярко-зеленая. В конце мая самки откладывают в выкопанные ямки овальные яйца (7-11 мм) в кожистой оболочке (6-12 шт.) Питаются разными насекомыми, в том числе с предупреждающей окраской, с резким запахом и жалящими. Враги прыткой ящерицы медянка, узорчатый полоз, гадюка Никольского, цапля и хищные птицы, грачи, сороки, лисицы, барсуки.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Живородящая ящерица",
                        imageResName = img("ph_0015_1"),
                        viewedId = "pres15",
                        text = """
Встречается только в Правобережье Саратовской области. Обитает на влажных лугах, зарослях около водоемов, в оврагах. Длина тела до 7 см (с хвостом до 19 см), лапы короткие, голова маленькая. Темные полосы на коричневом фоне на спине и по бокам. Брюхо у самцов кирпично-красное с черными пятнышками, у самок желтоватое, чаще без пятнышек. Бегают быстро, при опасности стремятся скрыться в убежище, в крайнем случае отбрасывают хвост. Хорошо плавает и ныряет, может передвигаться по дну. На зимовку уходят в конце сентября, прячутся под корнями, в дуплах лежащих деревьев. Выходят раньше других рептилий, в конце марта, можно встретить в течение дня, летом – утром и вечером. Брачный период в мае. Самки не откладывают яйца, а вынашивают внутри. Откладывают в августе, и почти сразу из них вылупляются маленькие ящерицы (28-33 мм), подрастают до 40 мм и уходят на зимовку. Еда живородящей ящерицы: пауки, бабочки и другие насекомые с нежестким хитиновым покровом. Враги: змеи, хищные птицы, сороки, сойки, цапли, лисицы и др. Вид внесен в Красную книгу Саратовской области.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Веретеница ломкая",
                        imageResName = img("ph_0007_1"),
                        viewedId = "pres7",
                        text = """
Единственная безногая ящерица в Саратовской области. Встречается в смешанных и лиственных лесах Правобережья, на вырубках, полянах, широких просеках. Длина тела до 40 см, окраска молодых серебристо-кремовая, с возрастом темнеет, с бронзовым отливом. У самцов темно-синие пятнышки, особенно в период размножения. Единственная ящерица с сумеречно-ночной активностью. Днем в укрытиях в лесной подстилке. Передвигаются довольно медленно, змеевидно извиваясь. При поимке, как и другие ящерицы, может отбросить хвост. На зимовку уходят в конце сентября (в норах, под гнилыми пнями, иногда группами). Выходят в конце апреля, брачный период в мае. У веретеницы яйцеживорождение, то есть яйца (6-16 штук) самка вынашивает до конца августа. Детеныши (около 8 см с хвостом) почти сразу появляются из отложенных яиц. Еда: черви, моллюски, личинки насекомых, многоножки (малоподвижные животные). Их поедают медянки, хищные птицы, вороны, сойки, лисицы, куницы. Вид внесен в Красную книгу Саратовской области.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Разноцветная ящурка",
                        imageResName = img("ph_0017_1"),
                        viewedId = "pres17",
                        text = """
Распространение вида в Саратовской области мозаично. Обитает на закрепленных песках, степных участках с полынной растительностью. Длина тела до 7 см и такой же хвост. Яркая окраска: продольные ряды светлых пятнышек, между ними темные пятна, брюхо белое. Ящурки очень быстрые, при опасности стараются убежать в укрытие, могут закапываться в песок. Норы копают сами. Летом активны в утренние и вечерние часы. На зимовку уходят во второй половине сентября. Выходят в конце марта. Брачный период в апреле. Самка откладывает яйца (2-7 штук) в норах и в ямки на прогреваемых участках (июнь-июль). Через 30-50 дней появляются маленькие ящерки (33-40 мм). Еда: разные насекомые, в том числе и жесткие жуки. Враги: узорчатый полоз, степная гадюка, хищные птицы, лисица, ушастый еж. Вид рекомендован к внесению в Красную книгу Саратовской области.
                        """.trimIndent()
                    )
                )
            ),
            AnimalSection(
                title = "Отряд Змеи",
                items = listOf(
                    AnimalTopic(
                        title = "Гадюка Никольского",
                        imageResName = img("ph_0008_1"),
                        viewedId = "pres8",
                        text = """
Синоним названия – лесостепная гадюка. Широко распространена в правобережье, обитают в смешанных лесах (поляны), в пойменных лугах, на зарастающих вырубках. В наиболее подходящих для них местах численность достигает 20 особей на гектар. Длина тела с хвостом до 90 см (самки крупнее). Окраска взрослых полностью черная, молодые гадюки коричневые с темной зигзагообразной полосой на спине (как у степной гадюки), чернеют после 4-6 линьки. К началу октября уходят на зимовку (в норы грызунов, иногда группами). Выходят массово во второй половине апреля. Весной и осенью пик активности в середине дня, летом – утром и к вечеру. Брачный период в мае. Гадюки не откладывают яйца, детеныши (7-17 штук) развиваются внутри яиц в теле самки. Молодь (в среднем 18 см) появляется во второй половине августа. После первой линьки (на 2-5 сутки) молодые гадюки начинают охотится. Размножаться начинают на 4 году. Основу питания составляют мышевидные грызуны, редко лягушки и ящерицы. Враги: хищные птицы и звери, человек. Ползает медленно, хорошо плавает. При опасности уползает или принимает угрожающую s-образную позу, шипит, совершает выпады. Яд опасен, но 4-6 дней человек выздоравливает. Гадюк часто убивают или отлавливают для сбора яда. Вид внесен в Красную книгу Саратовской области.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Степная гадюка",
                        imageResName = img("ph_0009_1"),
                        viewedId = "pres9",
                        text = """
Научное название вида – восточная степная гадюка. Длина тела с хвостом до 60 см. Распространена на юго-востоке Правобережья и Левобережья. В отличие от гадюки Никольского избегают увлажненных лесистых мест, предпочитают более сухие: поляны, овраги и балки с растительностью. Окраска серая или темно-серая с темной зигзагообразной полосой вдоль спины. Численность степных гадюк в Правобережье 2-5 на гектар, а в Заволжье до 10. Массово на зимовку уходят к концу сентября (в норы грызунов, подземные полости), иногда группами. Выходят в начале апреля, подолгу греются на солнце. Активны весь день, а летом утром и к вечеру. Брачный период в апреле. У степной гадюки яйцеживорождение: развитие зародышей происходит в яйцах, которые самка не откладывает, а носит в теле (до 100 дней). Основная еда маленьких гадюк – кузнечики и другие насекомые, взрослые весной питаются мышевидными грызунами, потом ящерицами, летом в основном кузнечиками и саранчой, к осени грызуны, рептилии, земноводные. При опасности старается скрыться в убежище или принимает угрожающую позу: лежит зигзагом, передняя часть приподнята, шипит, раздувается, делает выпады, а потом уже бросок-укус. Стараются зря не расходовать яд, который для человека малоопасен. Враги: хищные птицы и звери, чайки, реже змея медянка, человек. Вид внесен в Красную книгу Саратовской области.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Обыкновенный уж",
                        imageResName = img("ph_0013_1"),
                        viewedId = "pres13",
                        text = """
Вид распространен по всей Саратовской области, в Правобережье повсеместно, в Левобережье очагами. Встречается во влажных местах, по берегам водоемов, избегает сухих открытых пространств. Длина тела до 105 см. Окраска от темно-серой до черной. По бокам головы беловатые или желтые пятна, примерно у 10% ужей их нет. Окраска брюха – сочетание поперечных белых и черных полосок. На зимовку уходит к началу октября, выходит к началу апреля. Уж активен только в светлое время суток, весной и осенью пик активности в середине дня, летом утром и к вечеру. Брачный период в мае, откладка яиц к концу июня. Самки откладывают 8-18 яиц (размером 20-33 мм) в укромные места с гниющими растительными остатками (там образуется тепло). Вылупляются ужата (10-20 см длиной) во второй половине августа. Основная еда ужей – амфибии (75%), на втором месте мышевидные грызуны, редко мальки рыб и птенцы, насекомые. Враги: хищные птицы, цапля, лисица, корсак, барсук. Уж быстро ползает, поднимается на кусты, отлично плавает. Пищу добывает на суше, медленно заглатывает целиком. При опасности спасается бегством, выбрасывает из клоаки вонючую жидкость или лежит неподвижно, притворяется мертвым.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Водяной уж",
                        imageResName = img("ph_0012_1"),
                        viewedId = "pres12",
                        text = """
Обитание водяного ужа в Саратовской области приурочено к различным водоемам в долине верхней и средней зон Волгоградского водохранилища. Длина тела с хвостом обычно 80-90 см (редко до 120). От обыкновенного ужа отличается отсутствием желтых пятен и окраской: окраска верха от оливкового до темно-коричневого с темными пятнами примерно в шахматном порядке, на затылке темное V-образное пятно. Брюхо взрослых ужей желтое или оранжево-красное, у молодых светло-желтое. Выход из спячки в конце апреля. Брачный период в мае. В июне самка откладывает 8-17 яиц (20-40 мм). В августе вылупляются ужата длиной 14-17 см. Питаются в основном рыбой до 9 см длиной (90%), на втором месте амфибии и совсем редко насекомые. В случае опасности бросается в воду, прекрасно плавает, на суше сворачивается в клубок и пугает врага, выделяет из клоаки зловонную жидкость. Зимуют в норах грызунов. Враги: цапли, хищные птицы, лисица, мелких поедают хищные рыбы. Вид внесен в Красную книгу Саратовской области.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Обыкновенная медянка",
                        imageResName = img("ph_0010_1"),
                        viewedId = "pres10",
                        text = """
Медянка обитает в Правобережье Саратовской области в пойменных лесах, зарослях кустарников, на опушках и вырубках. Теплолюбивый и сухолюбивый вид. Длина тела с хвостом до 70 см. Окраска верха разнообразная: от серо-бурого до медно-красного с маленькими темными пятнами в 2-4 продольных ряда. От ноздри через глаз проходит темная полоса. У медянок яйцеживорождение: детеныши (от 2 до 15 штук) длиной 12-14 см появляются в августе. Основу питания составляют ящерицы, редко мышевидные и птицы. Добычу медянку душит, обвивая телом. Ползает медленно, хорошо лазает по кустам, плавает. При опасности сворачивается в плотный клубок, шипит, пытается укусить, выбрасывает вонючую жидкость. Враги: хищные птицы, вороны, ласки, ежи. Люди часто убивают медянок, думая, что это ядовитая змея. Вид внесен в Красную книгу Саратовской области.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Узорчатый полоз",
                        imageResName = img("ph_0011_1"),
                        viewedId = "pres11",
                        text = """
В Саратовской области полоз обитает в долине Волгоградского водохранилища в Правобережье и на юге Левобережья, на севере региона редок (опушки, заросли кустарников, склоны оврагов, встречаются в населенных пунктах, на дачных участках). Длина тела с хвостом до 100 см. Окраска верха буро-серая, вдоль тела 4 темные полосы. На голове характерный рисунок: поперечная темная дугообразная полоса, через глаз тоже полоса – бурая с черной каемкой. Брюхо сероватое. Активные полозы встречаются с первой половины марта до ноября. Суточная активность только в светлое время. Откладка яиц (3-14 штук) размером 16 мм в первой половине июля. Выход из яиц во второй половине августа. Через 6 дней, после первой линьки, молодые полозы (длина 27 см) начинают питаться. Взрослые полозы поедают мышевидных грызунов, птенцов птиц-норников и их яйца (скорлупа раздавливается в пищеводе), рептилий, редко амфибий. Полозы очень подвижные, хорошо лазают и плавают. В случае опасности у него вибрирует кончик хвоста, издавая треск, как у гремучей змеи. Враги: хищные птицы и млекопитающие. Люди убивают полозов, думая, что это ядовитые змеи.
                        """.trimIndent()
                    )
                )
            )
        )
    )

    return listOf(amphibians, reptiles)
}

// ------------------------------------
// ДАННЫЕ: Опасные животные
// ------------------------------------
fun buildDangerousAnimalsData(context: Context): List<AnimalClassBlock> {
    fun img(name: String) = name

    // Насекомые
    val insects = AnimalClassBlock(
        title = "Насекомые",
        sections = listOf(
            AnimalSection(
                title = "Кровососущие — переносчики болезней",
                items = listOf(
                    AnimalTopic(
                        title = "Блохи",
                        imageResName = img("ph_0022_1"),
                        viewedId = "danger_ph_0022_1",
                        text = """
Кровососущие внешние паразиты человека и животных, которые могут быть переносчиками некоторых инфекционных болезней. Блохи предпочитают обитать в подвалах и других укромных местах, размножаются там же. Питаются, кусая бездомных кошек, мышей. Но при возможности перепрыгивают на человека и кусают его. Укусы зудят, краснеют, воспаляются. Первая помощь: промыть водой с мылом, содой, приложить лед. Заражение человека блохами называется пуликоз (от латинского названия рода блох – Pulex). Можно пользоваться средствами, отпугивающими комаров и др. Но для полного избавления от блох необходимо обработать квартиру (мебель, ковры, постель, одежду), домашних животных, подвалы (!).                         """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Вши",
                        imageResName = img("ph_0023_1"),
                        viewedId = "danger_ph_0023_1",
                        text = """
Кровососущие внешние паразиты человека и животных, которые могут быть переносчиками некоторых инфекционных болезней. На человеке могут поселиться три вида вшей: головная, платяная и лобковая. Заболевание называется педикулез (от лат. pediculus «вошь»). В отличие от прыгающих блох, вши очень прочно держатся коготками за волоски и к ним же приклеивают яйца (гниды, светлые, овальные, до 1 мм). Развитие личинок, вышедших из яиц, происходит очень быстро. От вшей очень сложно избавиться чисто механическим путем, необходимо применение специальных средств. Укусы зудят и воспаляются. От человека к человеку взрослые вши и личинки передаются контактным способом. Педикулез – очень распространенное заболевание.                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Клоп постельный",
                        imageResName = img("ph_0028_1"),
                        viewedId = "danger_ph_0028_1",
                        text = """
Внешний паразит человека, питается кровью, размер взрослого 3-8 мм. Укусы зудят, воспаляются. Живут в укромных местах в квартирах, домах, дачах. Ведут ночной образ жизни, днем увидеть их сложно. В чистую квартиру попадают от соседей или из другого, зараженного, помещения с вещами (сумками, пакетами). Могут быть переносчиками заболеваний, но чаще приносят беспокойство своими укусами. Может быть индивидуальная аллергическая реакция. Требуется обработка всей квартиры инсектицидами (вещества, убивающие насекомых).                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Комары",
                        imageResName = img("ph_0024_2"),
                        viewedId = "danger_ph_0024_2",
                        text = """
Кто не знает этих надоедливых кровососов, писк которых уже портит настроение, потому что за этим может последовать неприятный укус, зуд. В Саратовской области обитают комары (из рода Culex, Anopheles, Aedes), которые могут переносить малярию и лихорадку Западного Нила. В области нет природных очагов малярии, диких животных-носителей, поэтому нет и малярии                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Кусачие мухи — жигалки",
                        imageResName = img("ph_0024_1"),
                        viewedId = "danger_ph_0024_1",
                        text = "Жигалка осенняя кусает очень больно: в период размножения – август-сентябрь самки активно нападают на животных и человека, чтобы напиться крови. Могут быть переносчиками болезней, но чаще просто очень досаждают своими болезненными укусами."
                    ),
                    AnimalTopic(
                        title = "Кусачие мухи — пестряки",
                        imageResName = img("ph_0025_1"),
                        viewedId = "danger_ph_0025_1",
                        text = "Синонимы: обыкновенный пестряк, обыкновенный златоглазик. Эти родственники слепней кусают очень больно, настойчиво нападают на человека и домашних животных (только самки). Места укусов опухают, зудят. Может быть индивидуальная аллергическая реакция."
                    ),
                    AnimalTopic(
                        title = "Слепни",
                        imageResName = img("ph_0026_1"),
                        viewedId = "danger_ph_0026_1",
                        text = "У этих похожих на мух двукрылых насекомых (1-2 см длиной) толстый ротовой аппарат, поэтому укус очень болезненный. Слепни чаще встречаются во влажных местах, активны в дневное время, кусают только самки, кровь нужна для созревания яиц, самцы питаются нектаром. Капли воды на теле человека, запахи и тепло очень привлекают слепней. Домашним животным на пастбищах слепни не дают нормально кормиться, уменьшаются удои молока."
                    ),
                    AnimalTopic(
                        title = "Дождёвки",
                        imageResName = img("ph_0027_1"),
                        viewedId = "danger_ph_0027_1",
                        text = "Относятся к семейству Слепней. Активны и в сумерки, в отличие от других слепней. Болезненные укусы воспаляются, особенно если у человека аллергия на слюну этих насекомых. Могут быть переносчиками туляремии и др. Помогают средства, отпугивающие насекомых, но надежнее защищает одежда."
                    ),
                    AnimalTopic(
                        title = "Кровососка оленья",
                        imageResName = img("ph_0027_2"),
                        viewedId = "danger_ph_0027_2",
                        text = "Синоним: лосиная муха. Кровососущее насекомое, очень похожее внешне на клеща. Ее можно отличить от клеща по количеству лап (у всех насекомых 6, у всех паукообразных - 8) и по наличию двух крыльев (у клещей крыльев нет). У этих насекомых кровь сосут и самцы, и самки. Наиболее активно кусают в августе-октябре. Нападают (днем) кровососки на крупных копытных, а также на человека. Жертву поджидают, сидя на деревьях, кустах, траве, реагируют на тепло и запах. Летают плохо, попав на тело животного, сбрасывают крылья и крепко цепляясь коготками, ползут вверх. Места укусов воспаляются и долго не заживают. Могут быть переносчиками боррелиоза, как и клещи."
                    )
                )
            ),
            AnimalSection(
                title = "Паразитирующие личинки",
                items = listOf(
                    AnimalTopic(
                        title = "Оводы",
                        imageResName = img("ph_0036_1"),
                        viewedId = "danger_ph_0036_1",
                        text = "В отличие от слепней вообще не могут укусить! У взрослых оводов не развит ротовой аппарат. Но являются опасными вредителями домашних животных. Самки откладывают яйца на шерсть (в глаза, уши), личинки, вышедшие из яиц, развиваются в полости глаз, носа, желудка и даже мозга, приводя к гибели животного."
                    )
                )
            ),
            AnimalSection(
                title = "Вызывающие раздражение кожи",
                items = listOf(
                    AnimalTopic(
                        title = "Клопы (растительноядные)",
                        imageResName = img("ph_0030_1"),
                        viewedId = "danger_ph_0030_1",
                        text = "В отличие от кровососущего постельного клопа множество видов питаются растительностью (или хищники). Для защиты от врагов выделяют едкие вещества, которые у человека вызывают раздражение кожи или слизистых оболочек (вспомним малинового клопа, попавшего в рот вместе с вкусной малиной с куста!)"
                    ),
                    AnimalTopic(
                        title = "Жуки-нарывники",
                        imageResName = img("ph_0029_1"),
                        viewedId = "danger_ph_0029_1",
                        text = "Выделяют едкую жидкость; яркая окраска как предупреждение."
                    ),
                    AnimalTopic(
                        title = "Божья коровка",
                        imageResName = img("ph_0031_1"),
                        viewedId = "danger_ph_0031_1",
                        text = "Может выделять резкий запах; не рекомендуется брать в руки."
                    )
                )
            ),
            AnimalSection(
                title = "Жалящие насекомые",
                items = listOf(
                    AnimalTopic(
                        title = "Пчела медоносная",
                        imageResName = img("ph_0033_1"),
                        viewedId = "danger_ph_0033_1",
                        text = "Жалящий аппарат находится на конце брюшка. Жало у пчел с зазубринами (может остаться в ранке) соединено с внутренней ядовитой железой. Это преобразованный яйцеклад не способных к размножению самок, рабочих особей у общественных насекомых. Яд пчел обладает полезными лечебными свойствами, но опасен, если аллергия. Попадание яда в под кожу вызывает резкую боль, покраснение, отек."
                    ),
                    AnimalTopic(
                        title = "Шмели",
                        imageResName = img("ph_0032_1"),
                        viewedId = "danger_ph_0032_1",
                        text = "Жалят редко; атакуют при угрозе гнезду."
                    ),
                    AnimalTopic(
                        title = "Оса обыкновенная",
                        imageResName = img("ph_0035_1"),
                        viewedId = "danger_ph_0035_1",
                        text = "Яд опасен, если у человека на него аллергия. Жало очень острое, легко выходит из ранки, поэтому оса может ужалить несколько раз. В периоды массового появления ос необходимо соблюдать осторожность (особенно на природе). Эти насекомые слетаются на запахи еды и напитков, настойчиво лезут в тарелки и стаканы, могут случайно попасть в рот, это наиболее опасный момент. Есть много видов одиночно живущих ос и пчел, которые не жалят, у них яйцеклад предназначен для откладывания яиц."
                    ),
                    AnimalTopic(
                        title = "Шершень",
                        imageResName = img("ph_0034_1"),
                        viewedId = "danger_ph_0034_1",
                        text = "Это самые крупные осы России. Их яд наиболее опасен для человека, вызывает резкую боль. Они могут быть смертельно опасными в зависимости от места укуса, количества и аллергической реакции на них. Если вы обнаружили гнездо шершней, немедленно отойдите на безопасное расстояние! Шершень активен днем, охотится на ос и других насекомых. Но есть одна опасная особенность поведения: в сумерках и ночью может прилететь на свет лампы или костра, может ужалить."
                    )
                )
            )
        )
    )

    // Паукообразные
    val arachnids = AnimalClassBlock(
        title = "Паукообразные",
        sections = listOf(
            AnimalSection(
                title = "Паразиты внутрикожные",
                items = listOf(
                    AnimalTopic(
                        title = "Клещ собачий",
                        imageResName = img("ph_0037_1"),
                        viewedId = "danger_ph_0037_1",
                        text = "Внешний паразит человека и животных (из группы иксодовых клещей). Идеально приспособлены к паразитическому образу жизни: сосущий ротовой аппарат в виде хоботка с шипиками, прочный хитиновый покров, плоское слитное тело, размеры 2-4 мм. Когда клещ попадает на тело человека, он какое-то время ползает, ищет место, где кожа более тонкая. Во время укуса он выделяет обезболивающие вещества, поэтому сам укус проходит незаметно. Если клеща не удалить, то за несколько дней он увеличится во много раз. Чаще всего напавший клещ не содержит в себе возбудителей заболеваний, но место укуса воспаляется и долго не заживает. Болезни, которые могут быть перенесены человеку клещами: туляремия, болезнь Лайма (боррелиоз), риккетсиоз, возвратный тиф и др. "
                    ),
                    AnimalTopic(
                        title = "Удаление клеща — как",
                        imageResName = img("ph_0037_3"),
                        viewedId = "danger_ph_0037_3",
                        text = "При обнаружении на теле присосавшегося паразита, необходимо вынуть его хоботок из ранки. Для этого можно воспользоваться щипчиками для бровей, пинцетом, петлей из нитки, выкручивая клеща, а не отрывая. Если хоботок не удалось сразу вытащить вместе с клещом, то его надо удалять как занозу. В Саратове есть специальная лаборатория, где могут исследовать клеща и сделать вывод, есть ли в нем возбудители заболевания (клещ должен быть живым, услуга платная). Отправляясь на природу, в места, где могут быть клещи, нужно соответствующим образом одеваться и обуваться, пользоваться отпугивающими спреями (распылять на одежду и обувь, не на голое тело!)."
                    )
                )
            ),
            AnimalSection(
                title = "Паразиты внутрикожные (прочие)",
                items = listOf(
                    AnimalTopic(
                        title = "Зудень чесоточный",
                        imageResName = img("ph_0038_1"),
                        viewedId = "danger_ph_0038_1",
                        text = "Не переносчик заболеваний, а сам является причиной болезни чесотки. Внутрикожный паразит (0,2-0,4 мм), который прогрызает ходы в клетках кожи, откладывает яйца, личинки, вышедшие из яиц, тоже начинают активно питаться клетками кожи. Деятельность клещей вызывает сильный зуд (отсюда название клеща и болезни). Чесотка довольно распространенное заболевание, заражение при прямом контакте с больным или через одежду и предметы."
                    ),
                )
            ),
            AnimalSection(
                title = "Опасные пауки и прочие",
                items = listOf(
                    AnimalTopic(
                        title = "Тарантул южнорусский",
                        imageResName = img("ph_0040_1"),
                        viewedId = "danger_ph_0040_1",
                        text = "Все пауки – хищники, убивают жертву своим ядом, но большинство из них безопасны для человека, силы их яда хватает, чтобы убить насекомое. Тарантулы – довольно крупные (4-6 см) мохнатые пауки-волки, которые нападают на насекомых, не делая паутину. Днем прячутся в норках, которые сами выкапывают и выстилают паутиной, туда могут упасть мелкие насекомые. Ночью выходят для активной охоты. Яд не опасен для человека, при укусе возможны болевые ощущения и местное воспаление, индивидуальная аллергическая реакция."
                    ),
                    AnimalTopic(
                        title = "Каракурт (черная вдова)",
                        imageResName = img("ph_0043_1"),
                        viewedId = "danger_ph_0043_1",
                        text = "В Левобережье Саратовской области зарегистрированы единичные (!) встречи этого очень опасного паука. Яд обладает нервнопаралитическим действием. "
                    ),
                )
            ),
                    AnimalSection(
                    title = "Другие паукообразные",
                    items = listOf(
                        AnimalTopic(
                            title = "Скорпионы",
                            imageResName = img("ph_0041_1"),
                            viewedId = "danger_ph_0041_1",
                            text = "В Саратовской области обитает 1 вид скорпионов. Этих небольших паукообразных (2-3 см) в природе можно встретить в Красноармейском районе на меловых склонах вдоль береговой линии да и то очень редко, они активны в ночное время. Рану скорпионы наносят своим жалом на конце брюшка, правильнее говорить «скорпион ужалил», по ощущениям похоже на пчелу или осу (болезненно, но неопасно). "
                        ),
                        AnimalTopic(
                            title = "Фаланги (сольпуги)",
                            imageResName = img("ph_0042_1"),
                            viewedId = "danger_ph_0042_1",
                            text = "Крупные мохнатые паукообразные (5-6 см), которые относятся к отдельному отряду – Сольпуги. Ведут ночной образ жизни, вооружены мощными челюстями, но не имеют ядовитых желез. При опасности фаланга часто не убегает, а пытается напугать: принимает угрожающую позу и даже пищит (звук из-за трения челюстей). У нее это очень хорошо получается, редко кто рискнет нападать. Активны ночью, потому что днем жарко. Питается насекомыми и другими беспозвоночными, в том числе падалью. Если фаланга недавно питалась мертвой добычей, то на ее челюстях множество бактерий, этим она и опасна для человека. В Саратовской области обитает 1 вид, в степных участках Красноармейского района."
                        )
                    )
        )
        )
    )

    // Моллюски
    val molluscs = AnimalClassBlock(
        title = "Моллюски",
        sections = listOf(
            AnimalSection(
                title = "Съедобные / осторожность",
                items = listOf(
                    AnimalTopic(
                        title = "Беззубки",
                        imageResName = img("ph_0044_1"),
                        viewedId = "danger_ph_0044_1",
                        text = "В Саратовской области нет ядовитых моллюсков, как в тропиках. Крупных речных двустворчатых моллюсков (беззубок, перловиц) можно употреблять в пищу, если правильно приготовить. Опасность для купальщиков могут представлять острые края расколотых раковин моллюсков на дне водоема."
                    ),
                    AnimalTopic(
                        title = "Улитки виноградные",
                        imageResName = img("ph_0044_2"),
                        viewedId = "danger_ph_0044_2",
                        text = "Сухопутные брюхоногие моллюски, все больше распространяющиеся по территории Саратовской области. В сухую погоду прячутся в укромных местах, после дождей выползают и активно питаются.  Считаются деликатесом, но нужно знать, как правильно их готовить."
                    )
                )
            )
        )
    )

    // Паразитические черви
    val worms = AnimalClassBlock(
        title = "Паразитические черви",
        sections = listOf(
            AnimalSection(
                title = "Паразиты внутренние",
                items = listOf(
                    AnimalTopic("Сосальщик печеночный", img("ph_0045_2"), "danger_ph_0045_2", "Гельминты (в народе «глисты») – это общее название паразитических червей, которые могут обитать внутри организма человека, вызывая заболевания разной степени тяжести. Заразные для скота (и человека) личинки находятся на траве около водоема."),
                    AnimalTopic("Цепень свиной", img("ph_0045_3"), "danger_ph_0045_3", "Как человек может заразиться: заразная стадия личинок находится в особой упаковке (называется финна) в мышцах свиней или коров (коровий цепень). При недостаточной термической обработке финнозного мясо в пищеварительную систему человека могут попасть живые личинки и превратиться (в тонком кишечнике) длинных плоских взрослых червей."),
                    AnimalTopic("Лентец широкий", img("ph_0057_1"), "danger_ph_0057_1", "Заражение из сырой/плохо термически обработанной рыбы."),
                    AnimalTopic("Аскарида", img("ph_0045_4"), "danger_ph_0045_4", "Заражение через загрязнённые руки и продукты."),
                    AnimalTopic("Острица", img("ph_0045_5"), "danger_ph_0045_5", "Небольшие (2-5 мм) круглые черви (глисты). Заражение человека происходит, когда в организм с грязными руками и продуктами попадают яйца остриц. Живут они в толстом кишечнике, самки ночью выползают и откладывают яйца, вызывая зуд. Если ребенок будет чесаться, то этими яйцами может происходить самозаражение.")
                )
            ),
            AnimalSection(
                title = "Паразиты внешние",
                items = listOf(
                    AnimalTopic("Пиявка медицинская", img("ph_0046_1"), "danger_ph_0046_1", "Пиявки живут в пресных водоемах. Разные виды пиявок нападают на определенных животных (рыбья – на рыбах и т.д.), они не внутренние, а внешние паразиты (эктопаразиты). На человека в водоемах Саратовской области может напасть только один вид – пиявка медицинская (очень редкая, занесена в Красную книгу СО). Острыми челюстями прогрызают покровы и пьют кровь, пока не наполнится их сильно разветвленный кишечник, только тогда они сами отваливаются. Во время питания пиявки выделяют вещество, не дающее крови свертываться – гирудин. Из-за этой особенности пиявок применяют в медицине: гирудотерапия – лечение пиявками. Если к купальщику прикрепилась пиявка, то ее нужно снять, а место укуса обработать антисептиками и плотно забинтовать впитывающей повязкой.")
                )
            )
        )
    )

    // Рыбы
    val fishes = AnimalClassBlock(
        title = "Рыбы",
        sections = listOf(
            AnimalSection(
                title = "Рыбы хищные",
                items = listOf(
                    AnimalTopic("Зубы (опасность при ловле)", img("ph_0047_1"), "danger_ph_0047_1", "Ядовитых рыб в водоемах Саратовской области нет. Опасность могут представлять крупные хищники, у которых множество острых зубов (щуки, судаки, сомы и др.). Сами рыбы не будут нападать на купальщиков, это не акулы. Опасность представляют пойманные рыбы."),
                    AnimalTopic("Плавники (колючие)", img("ph_0047_2"), "danger_ph_0047_2", "У некоторых рыб острые колючие плавники; травмы и риск инфекции.")
                )
            )
        )
    )

    // Земноводные (опасности)
    val amphibians = AnimalClassBlock(
        title = "Земноводные",
        sections = listOf(
            AnimalSection(
                title = "Раздражающая слизь",
                items = listOf(
                    AnimalTopic("Зеленая жаба", img("ph_0001_1"), "danger_ph_0001_1",
                        "В Саратовской области 6 видов амфибий. У представителей трех видов кожные железы выделяют слизь, обладающую довольно сильным раздражающим свойством. Если вы подержали в руках жабу, то обязательно тщательно вымойте руки, не трогайте ими слизистые оболочки рта, глаз. Жабу можно отличить от лягушки по крупным вздутым околоушным железам за глазами и по способу передвижения: жабы ходят, задние ноги у них короче, чем у прыгающих лягушек."),
                    AnimalTopic("Краснобрюхая жерлянка", img("ph_0018_8"), "danger_ph_0018_8",
                        "Если вы потрогали краснобрюхую жерлянку, то не нужно потом этими руками тереть глаза и слизистые оболочки (рта, например). Окраска спины у жерлянки защитная, а брюхо с яркими красно-оранжевыми пятнами, это предупреждающая окраска. Прежде чем ловить такую «лягушку», посмотрите на окраску живота. И подумайте вообще, а зачем она вам нужна."),
                    AnimalTopic("Чесночница", img("ph_0006_1"), "danger_ph_0006_1",
                        "Небольшая, похожая по форме тела на лягушку, амфибия обладает рядом интересных особенностей. Ее можно отличить от других земноводных Саратовской области по мелким красным крапинкам на спине. Она может глубоко закапываться в почву (роет пяточными шкпорами), выделяет защитные вещества, напоминающие по запаху чеснок. В руки ее не стоит брать. Просто понаблюдайте, сфотографируйте и оставьте в покое.")
                )
            )
        )
    )

    // Пресмыкающиеся
    val reptiles = AnimalClassBlock(
        title = "Пресмыкающиеся",
        sections = listOf(
            AnimalSection(
                title = "Ядовитые змеи",
                items = listOf(
                    AnimalTopic("Гадюка Никольского", img("ph_0008_1"), "danger_ph_0008_1",
                        "Два вида гадюк обитают в Саратовской области, еще один вид – гадюка обыкновенная под вопросом. На самом деле опасность их сильно преувеличена. Но для большинства людей, встретивших гадюк – это сигнал к уничтожению. А ведь все змеи питаются, в том числе, мышевидными грызунами, помогая регулировать их численность. Укусить гадюка может только в том случае, если на неё случайно наступить или взять в руки или просто пробовать поймать."),
                    AnimalTopic("Степная гадюка", img("ph_0009_1"), "danger_ph_0009_1",
                        "Этот вид легко отличить от черной гадюки Никольского и от других змей Саратовской области по темной зигзагообразной полосе вдоль всей спины. Увидев такую змею, постарайтесь отойти подальше, не пугайте ее, не пытайтесь убить, она сама уже вас испугалась и уползет. Все другие пресмыкающиеся нашей области: обыкновенный и водяной ужи, узорчатый полоз, обыкновенная медянка, веретеница ломкая, разноцветная ящурка, прыткая и живородящая ящерицы, болотная черепаха – не ядовиты. Их тем более не нужно убивать!")
                )
            )
        )
    )

    // Птицы
    val birds = AnimalClassBlock(
        title = "Птицы",
        sections = listOf(
            AnimalSection(
                title = "Переносчики заболеваний",
                items = listOf(
                    AnimalTopic("Городские голуби", img("ph_0048_1"), "danger_ph_0048_1",
                        "Казалось бы, какая опасность может быть от птиц? Одни радости: красивые, поют, щебечут, насекомых вредных поедают. Но у птиц могут быть свои, птичьи, болезни, некоторые из которых передаются человеку. Поэтому не стоит брать птиц в руки в дикой природе и тем более в городе, где больные встречаются чаще (еды в избытке, а хищников меньше). Кормление голубей около жилых домов может привести к размножению мышей и крыс. За это даже могут оштрафовать.")
                )
            )
        )
    )

    // Млекопитающие
    val mammals = AnimalClassBlock(
        title = "Млекопитающие",
        sections = listOf(
            AnimalSection(
                title = "Хищные",
                items = listOf(
                    AnimalTopic("Зубы (общее предупреждение)", img("ph_0048_2"), "danger_ph_0048_2",
                        "Ядовитые звери – это вообще явление чрезвычайно редкое в природе. Но, например, все хищные млекопитающие от мала до велика, обладающие специализированными для нападения зубами, могут представлять угрозу для человека. Это лисицы, куницы, ласки и др. Лисицы могут быть заражены бешенством. Это очень опасное заболевание передается через укусы от диких животных домашним, а в крайних случаях и человеку")
                )
            ),
            AnimalSection(
                title = "Копытные",
                items = listOf(
                    AnimalTopic("Лось", img("ph_0049_1"), "danger_ph_0049_1", "Лесной великан, народное название – сохатый (за сходство рогов с сохой, которой раньше пахали землю). Они могут быть опасны в силу своих размеров (особенно самцы в период гона или раненые охотниками, самки, защищающие лосят)."),
                    AnimalTopic("Кабан", img("ph_0052_1"), "danger_ph_0052_1", "Эти копытные, живущие стадами или семьями осторожны, как и все дикие лесные копытные, но часто проявляют агрессивность во период размножения и если придется защищать потомство. Особенно опасны кабаны, раненные охотниками (у самцов мощные клыки)."),
                    AnimalTopic("Олень благородный ", img("ph_0050_1"), "danger_ph_0050_1", "Восхитительные копытные саратовских лесов, специально завезены в Саратовскую область в середине прошлого века. У самцов мощные острые рога. Животные могут быть опасны во время гона (в период размножения, когда самцы соперничают между собой)."),
                    AnimalTopic("Олень пятнистый ", img("ph_0051_1"), "danger_ph_0051_1", "Красивые грациозные, завезены специально для акклиматизации в леса Саратовской области в середине прошлого века. Скорее всего вам не удастся подойти близко к этим осторожным животным, да и не стоит этого делать."),
                    AnimalTopic("Косуля", img("ph_0051_2"), "danger_ph_0051_2", "")
                )
            ),
            AnimalSection(
                title = "Грызуны",
                items = listOf(
                    AnimalTopic("Лесная мышь", img("ph_0053_1"), "danger_ph_0053_1",
                        "Этот вид и рыжая полевка – потенциальные распространители опасной болезни – геморрагической лихорадки с почечным синдромом. Возбудители заболевания – вирусы. Они содержатся в выделениях больных грызунов и могут попасть к человеку разными путями: воздушно-пылевым, контактным, через загрязненные продукты или воду. "),
                    AnimalTopic("Рыжая полевка", img("ph_0054_1"), "danger_ph_0054_1",
                        "Потенциальные распространители опасной вирусной болезни – геморрагической лихорадки с почечным синдромом. В годы массового размножения этих грызунов в местах, которые называются природные очаги ГЛПС, человек может заразиться разными способами. Необходимо знать про эти территории и постараться их не посещать или тщательно соблюдать личную гигиену."),
                    AnimalTopic("Еж белогрудый ", img("ph_0055_1"), "danger_ph_0055_1",
                    "Водится в лесах Саратовской области (в степной местности – ушастый). Эти милые животные, герои сказок и мультфильмов, которых дети частенько притаскивают домой и держат как домашних питомцев, на самом деле не такие уж и безопасные. Они просто напичканы различными паразитами внешними и внутренними. Если вы увидели ежа, не набрасывайтесь на него, просто понаблюдайте, сфотографируйте и оставьте в покое. Он принесет пользу вашему дачному или приусадебному участку, уничтожая насекомых вредителей и слизней.")
                )
            ),
            AnimalSection(
                title = "Городские бродячие",
                items = listOf(
                    AnimalTopic("Правила поведения", img("ph_0056_1"), "danger_ph_0056_1",
                        "Беспризорные животные – это вообще коллекция всякой «заразы», не говоря уже о том, что могут напасть, напугать или искусать (см. Правила). Если вы погладите бездомного котенка, то ему вы пользы не принесете, а для вас могут всякие неприятности – паразиты внешние и внутренние, лишаи и др. Любое животное может травмировать человека, если ему придется защищать себя или потомство. Поэтому помните, что в лесу, в степи мы только гости, и вести себя надо соответственно.")
                    )
            )
        )
    )

    return listOf(
        insects,
        arachnids,
        molluscs,
        worms,
        fishes,
        amphibians,
        reptiles,
        birds,
        mammals
    )
}

// ==================== ЧАСТЬ 2. ДАННЫЕ МЛЕКОПИТАЮЩИХ ====================

fun buildMammalsData(context: Context): List<AnimalClassBlock> {
    fun img(name: String) = name

    val mammals = AnimalClassBlock(
        title = "Класс Млекопитающие",
        sections = listOf(
            AnimalSection(
                title = "Отряд Насекомоядные",
                items = listOf(
                    AnimalTopic(
                        title = "Бурозубки и белозубки",
                        imageResName = img("ph_0078_1"),
                        viewedId = "mamm_ph_0078_1",
                        text = """
Бурозубки и белозубки. Отряд Насекомоядные, семейство Землеройковые. Они родственники ежей. Живут в лесах и около населенных пунктов, часто в самих постройках. Отличия от «мышей»: мордочка вытянута в хоботок, зубы примерно одинакового размера, шелковистая шерстка темного окраса, ушные раковины не видны, самые маленькие из млекопитающих. Живут в лесной подстилке, в естественных укрытиях, сами норы не роют. В спячку не впадают, рекордсмены по скорости жизненных процессов: пульс до 1500 ударов в минуту, частота дыхания 800 в мин. Все происходит очень быстро, поэтому и жизнь у них короткая – 12-18 месяцев. За это время они успевают 3-5 раз произвести потомство по 5-8 детенышей! Питание: черви, пауки, насекомые, в том числе вредители, поэтому эти зверьки приносят человеку пользу! Съедают за сутки свой вес, а то и в 2 раза больше. Но (!) природные носители внешних и внутренних паразитов.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Похожи на мышей, но не мыши. Чем бурозубка (белозубка) внешне отличается от обычной мыши? (Подсказка: присмотрись к её мордочке и ушам).
2. Жизнь в ускоренном режиме. Эти зверьки — настоящие рекордсмены! Какой у них невероятно высокий пульс, но почему же из-за этого жизнь у них такая короткая?
3. Маленький зверёк, но прожорливый. Сколько еды может съесть бурозубка за сутки по сравнению с собственным весом? Почему ей нужно так много пищи?
4. Полезный, но и опасный сосед. Чем бурозубки приносят большую пользу человеку в саду или огороде? И какая опасность для человека связана с этими зверьками, даже несмотря на их пользу?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Выхухоль русская",
                        imageResName = img("ph_0083_1"),
                        viewedId = "mamm_ph_0083_1",
                        text = """
Выхухоль русская. Отряд Насекомоядные, семейство Кротовые. Длина тела около 20 см и хвост 10 см. Удивительно приспособлена к водному образу жизни: не намокающий мех, плоский (с боков) со щетинками хвост, перепонки на пальцах, верх бурый, низ светлый. Голова заканчивается подвижным хоботком (у всех Насекомоядных мордочка с хоботком). Рот устроен так, что зверек может есть под водой. Нора с входом под водой. На суше этот медлительный зверек появляется редко, ночью. Еще в середине 20 века численность выхухоли была довольно высокая, но неумеренный отлов на шкурки, засухи и половодья, увеличивающаяся загрязненность водоемов, все это привело к резкому сокращению численности. Ученые сейчас спорят, есть ли вообще этот зверек на территории Саратовской области. Внесена в Международную (!) Красную книгу.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Супер-пловец с хоботком. Выхухоль — настоящий водный житель. Какие три особых приспособления есть у неё для жизни в воде? (Подсказка: посмотри на её мех, хвост и лапы).
2. С хоботом, но не слон. О чем говорит наличие хоботка на ее мордочке? 
3. Загадочный невидимка. Почему встретить выхухоль на суше — большая редкость? Когда и как она появляется на берегу?
4. Почему она исчезает? Ещё не так давно выхухолей было довольно много. Какие четыре основные причины привели к резкому сокращению их численности? (Одна из них — деятельность человека).
5. Живое сокровище мира (живое ископаемое). Какой важный международный документ свидетельствует о том, что выхухоль находится в большой опасности по всей планете? И что сейчас даже учёные не могут точно сказать про неё в Саратовской области?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Ёж белогрудый",
                        imageResName = img("ph_0086_1"),
                        viewedId = "mamm_ph_0086_1",
                        text = """
Ёж белогрудый. Отряд Насекомоядные, семейство Ежиные. Размер в среднем 25 см, за счет иголок кажется крупнее. Активный истребитель червей, слизней и насекомых, из позвоночных может напасть на мышь, ящерицу, змею, птенца. Охотится ночью. Сам может стать жертвой лисицы. На зиму впадает в спячку (пробуждение в апреле). Ежа услышать не сложно, передвигается шумно. Но не следует нести пойманного ежа домой: днем он будет спать, а ночью шуметь, на ежах огромное количество паразитов, и вообще «любое животное, в неволе, обречено на мучения и даже гибель». Период размножения растянут на все лето, ежата (3-8 шт.) рождаются слепыми и голыми, с мягкими зачатками игл в гнезде из растительных остатков. Обитает в Правобережье. В Левобережье живет ёж ушастый, отличающийся большими размерами ушей.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Надежная защита. Что покрывает его тело, но не спасет, например, от кого?
2. Что входит в «меню» ежа? Он не только ест насекомых, но и может охотиться на более крупную добычу. На кого он может напасть, и какую пользу приносит людям в саду или огороде?
3. Какой распорядок дня у ежа? Когда он выходит на охоту и что делает зимой, когда становится холодно?
4. Можно ли услышать ежа в лесу? Как он передвигается и почему его бывает слышно даже ночью?
5. Почему НЕЛЬЗЯ брать найденного в лесу ежа домой? Какие три серьёзные причины приводятся в тексте, объясняя, почему это плохая идея?
6. Степной «братец» Какой самый заметный признак выдает «ушастого» ежа, где он обитает?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Ёж ушастый",
                        imageResName = img("ph_0087_1"),
                        viewedId = "mamm_ph_0087_1",
                        text = """
Отряд Насекомоядные, семейство Ежиные. Он отличается крупными ушами, меньше по размеру, чем еж белогрудый. В Заволжье обитание его приурочено к деревьям и кустарникам (лесополосы, заброшенные сады). Питание: степные насекомые, молодые ящерицы, реже растения, может длительно голодать. Ежей поедают лисицы и крупные хищные птицы. Спячка октябрь-апрель.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Кутора",
                        imageResName = img("ph_0096_1"),
                        viewedId = "mamm_ph_0096_1",
                        text = """
Отряд Насекомоядные, семейство Землеройковые. Чаще встречается в северных районах Саратовской области. Небольшой околоводный зверек, но может встречаться и довольно далеко от водоема.  Длина тела до 9 см. Хвост до 7 см, плоский с боков, с редкими волосками, приспособленный к плаванию. Мех черный сверху и белый снизу, густой бархатистый, не намокающий, содержит воздух. Зверек хорошо плавает и ныряет, бегает по дну. Питание: насекомые, черви, раки, мальки, головастики, птенцы, мелкие грызуны. За сутки съедает больше своего веса. Долго не может обходиться без еды, в ловушках быстро погибает. В спячку не впадает. Копает норы, если грунт мягких, в которых несколько входов на суше и один под водой. Подчелюстная железа выделяет слабо ядовитое вещество, кутора делает запасы на зиму в норе в виде обездвиженных животных. Период размножения с апреля до сентября. За год самка может принести до 3 пометов по 5-9 детенышей в каждом. Продолжительность жизни у куторы до 19 мес.
                        """.trimIndent()
                    )
                )
            )
            ,
            AnimalSection(
                title = "Отряд Рукокрылые",
                items = listOf(
                    AnimalTopic(
                        title = "Вечерница рыжая",
                        imageResName = img("ph_0084_1"),
                        viewedId = "mamm_ph_0084_1",
                        text = """
Вечерница рыжая. Отряд Рукокрылые. Длина тела с хвостом около 12 см, а вот размах крыльев до 25 см. Верх рыжий, низ светлее. На охоту вылетает сразу после захода солнца (отсюда и название). Ловит на лету ночных жуков, бабочек и др. (за ночь может поймать до 30 майских жуков). Скорость в полете до 50 км/ч. Одна из самых удивительных способностей всех рукокрылых – ориентироваться и охотится с помощью издаваемых звуков – эхолокация. Летом поселяется в дуплах, за отставшей корой, на чердаках, осенью улетает на Кавказ. Иногда образует колонии по 20-30 особей. В июне два детеныша, которые в июле уже летают самостоятельно. Может встречаться везде, где есть старые деревья. Самый распространенный вид летучих мышей в нашей области. Под вопросом обитание вечерницы малой и гигантской.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Крылья-рекордсмены. Тело вечерницы маленькое (около 12 см), а размах её крыльев гораздо больше. Насколько широко она может распахнуть свои крылья? Почему такая большая площадь крыльев важна для этого животного?
2. Охотница в темноте. Как вечерница, которая охотится ночью, может находить и ловить маленьких быстрых насекомых? Какая удивительная способность всех летучих мышей ей в этом помогает?
3. Ночной аппетит. За одну ночь вечерница может съесть очень много насекомых. Сколько майских жуков она способна поймать? Как ты думаешь, полезна ли она для леса или сада?
4. Летний гость и путешественник. Где вечерница живёт летом в нашей области? Что происходит с ней осенью, когда становится холодно и насекомые исчезают?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Кожан поздний",
                        imageResName = img("ph_0097_1"),
                        viewedId = "mamm_ph_0097_1",
                        text = """
Отряд Рукокрылые. Длина тела до 8 см. Крылья крупные и широкие. Типичный синантропный вид, связанный с поселениями человека (в городах и поселках). Вылет на кормежку около 21 ч. Кормятся в течение часа (жуками, бабочками, комарами), второй вылет в 2 часа ночи. Полет небыстрый, ровный, без резких поворотов. Чаще на высоте 3-5 м, но иногда поднимается высоко. Отмечены случаи зимовки этого вида в Саратовской области.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Кожан двухцветный",
                        imageResName = img("ph_0098_1"),
                        viewedId = "mamm_ph_0098_1",
                        text = """
Отряд Рукокрылые. Длина тела до 6,5 см. Мех густой, на спине рыже-бурый, на брюхе светлый. Места обитания в природе приурочены к водоемам. Чаще встречается в поселениях человека, в том числе и в Саратове. Питаются ночными бабочками и комарами. Детеныши (1-2) появляются в середине июня. Возможно этот вид зимует в пределах области.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Нетопырь-карлик",
                        imageResName = img("ph_0111_1"),
                        viewedId = "mamm_ph_0111_1",
                        text = """
Отряд Рукокрылые. Перелетный вид, известный по находкам колоний в нескольких районах Правобережья и в Ровенском. Длина тела до 5 см, вес до 8 г. Мех короткий коричнево-рыжей окраски. Уши и мордочка темные. Большинство встреч связано с постройками, обязательны близость лесных массивов и водоемов. Весной появляются в мае. Детеныши в середине июня. В августе зверьки покидают места, где проходило размножение. Обнаружены отдельные находки вида-двойника – нетопыря малого. В пойме р.Саратовки, р.Чардым и р. Терешки. Внешний вид и биология схожи.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Нетопырь лесной",
                        imageResName = img("ph_0112_1"),
                        viewedId = "mamm_ph_0112_1",
                        text = """
Отряд Рукокрылые. Перелетный, широко распространенный вид. В лесостепном Правобережье довольно многочисленный, в Левобережье встречается вдоль долины Волги и малых рек, каналов. Здесь чаще поселяется в постройках. Длина тела до 6 см, масса до 12 г. Окраска недлинного меха рыжевато-бурая, снизу серая. Пища: комары и мелкие бабочки. Весной появляются в апреле. В июне-июле детеныши (обычно 2 у одной самки) начинают самостоятельно вылетать из убежищ. Улетают в южные широты в сентябре. Происхождение названия: от старославянского «нето» - ночь и «пырь» -летающий.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Нетопырь средиземноморский",
                        imageResName = img("ph_0113_1"),
                        viewedId = "mamm_ph_0113_1",
                        text = """
Нетопырь средиземноморский. Отряд Рукокрылые. Обычный, местами многочисленный, оседлый вид. В Правобережье по долине Волги, центральное и южное Заволжье. Часто в постройках. Длина тела до 4,8 см, вес до 10 г. Мех средней длины, палево-бурый. Отличительный признак – широкая светлая кайма или пятно по заднему краю крыловой перепонки. Убежища исключительно в постройках. Пища: комары и мелкие бабочки-моли. Детеныши (обычно 2) рождаются в конце июня. Зимняя спячка начинается в середине октября. Нетопырь на фото был найден зимой в подъезде 5-этажного дома около Театра драмы (зимовал, видимо, на чердаке), выкормлен до весны (учителем биологии) и отпущен к своим собратьям там же, около театра. Происхождение названия: от старославянского «нето» - ночь и «пырь» -летающий.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Не птица, а летает. Он – млекопитающее, носит пушистую шубку, но от большинства зверей отличается, чем же?
2. Бесплатная доставка на дом! Этот зверек каждый вечер выходит на работу и съедает огромное количество… (кого?). Если бы нетопырь был шеф-поваром, какое «фирменное блюдо» было бы у него в ресторане для летучих мышей?
3. Квартирный вопрос, или «Где летучие мыши зимуют?» Он не строит гнёзда на деревьях, а селится в постройках: на чердаках, в щелях. А где же зимует?
4. История спасения: нетопырь в подъезде.
Однажды зимой нетопыря нашли в подъезде пятиэтажного дома у Театра драмы. Его выкормила и выпустила весной учительница биологии. Как думаешь, почему летучая мышь оказалась зимой в подъезде, а не в лесу? И что бы ты сделал, если бы нашёл такого полусонного зверька?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Ночница водяная",
                        imageResName = img("ph_0115_1"),
                        viewedId = "mamm_ph_0115_1",
                        text = """
Отряд Рукокрылые. Обычный, относительно оседлый вид. Длина тела до 6 см, вес до 10 г. Морда почти без волос. Мех густой ровный длинный. Спина бурая, брюшко почти белое. Поселяются в убежищах естественного происхождения (дупла, отставшая кора), редко в постройках, обязательно рядом с водоемом, потому что кормятся околоводными насекомыми (комары, веснянки, поденки) в воздухе и с поверхности воды. Размножение схожее с видом ночница степная. Зимовки на территории области не подтверждены, но вероятны. Зарегистрированы также находки еще двух видов ночниц: Брандта и прудовой. Встречаются редко, зимовки не подтверждены.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Ночница степная",
                        imageResName = img("ph_0116_1"),
                        viewedId = "mamm_ph_0116_1",
                        text = """
Отряд Рукокрылые. Обычный, местами многочисленный, относительно оседлый вид, в Право и Левобережье вдоль долины р.Волги. Длина тела до 5 см, вес до 8 г. Передняя часть морды покрыта темными волосами. Мех густой длинный, всклокоченный. Окраска спины от бурой до золотистой, брюхо светло-серое. Большинство находок колоний связано с постройками. Численность колоний 20, максимум 30 особей. Охотятся на летающих насекомых невысоко над землей или водой. Период размножения растянут: весна-лето. В колониях летом наблюдались разновозрастные детеныши. Возможно, что эти зверьки зимуют в нашей области.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Ушан бурый",
                        imageResName = img("ph_0141_1"),
                        viewedId = "mamm_ph_0141_1",
                        text = """
Ушан бурый. Отряд Рукокрылые. Малочисленный оседлый вид. Отдельные встречи в Правобережье, связан с лесами и водоемами, поэтому в Левобережье обитание маловероятно. Длина тела до 5 см, предплечье до 4,3 см. Крылья широкие. Уши до 4 см, почти как тело, во время отдыха складываются. Верх буроватый, низ светлый. Выводковые колонии (состоят из особей одного вида; из 3-12 самок) устраивает за отставшей корой деревьев или в постройках. Охотятся в кронах деревьев, используя характерную для всех летучих мышей эхолокацию. Полет у них порхающий, могут зависать в воздухе, используют «присады» - места, где поедают насекомых (часто собирает насекомых с веток), пьют в ближайшем водоеме. Детеныши (по 1) появляются к концу июня. Через 1,5-2 мес. переходят к самостоятельной жизни. Встречаются до наступления ночных заморозков, зимняя спячка в области возможна, но не подтверждена находками.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Гигантские локаторы. Какая часть тела у ушана почти равна длине всего его тельца и куда он это прячет во время отдыха?
2. Бережливая архитектор. Где самки ушана предпочитают устраивать свои летние «детские сады» и из скольких мам может состоять такая колония?
3. Воздушный фуршет. Какой хитрый способ охоты использует ушан, чтобы не есть насекомых прямо в воздухе? Что ушан может делать как колибри?
4. Миниатюрный размер. Какого размера взрослый ушан бурый – сравним с ладонью человека, с книгой или с кошкой?
5. Зимняя загадка: что может происходить с ушанами зимой в Саратовской области, но никто точно этого не видел?
                        """.trimIndent()
                    )
                )
            )
            ,
            AnimalSection(
                title = "Отряд Зайцеобразные",
                items = listOf(
                    AnimalTopic(
                        title = "Заяц-русак",
                        imageResName = img("ph_0089_1"),
                        viewedId = "mamm_ph_0089_1",
                        text = """
Заяц-русак. Отряд Зайцеобразные, семейство Зайцевые.  Длина тела до 70 см, вес до 6-7 кг. Уши длиннее, чем у беляка, кончики темные. Зимой окраска более светлая, чем летом, но голова и верх тела остается темным. Вокруг глаз белые кольца. Лапы короче и уже, чем у беляка. Обитают в степных районах области, но предпочитают лесополосы, осиновые колки, заросли кустарников.  (особенно в зимнее время). Питание: предпочитают зеленые корма (бобовые и злаки), зимой кора деревьев, всходы озимых, сухие травы. Наносят существенный вред культурным плодовым деревьям, обгрызая зимой кору. Первый помет появляется ранней весной (обычно 2 зайчонка), поздней весной или летом, то 4-6. Места размножения богаты укрытиями, далеки от с/х угодий и выпасов скота. Особенность поведения зайчат: они затаиваются в укрытиях и даже при близкой опасности не выбегают. Нор зайчихи не делают.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Мастер маскировки. Зимой шубка у зайца-русака светлеет, но не полностью. Какая часть его тела остаётся тёмной? 
2. Следопыт. Чем заяц-русак внешне отличается от зайца-беляка? Обрати внимание на длину и цвет ушей, а также на форму лап.
3. Не только капуста. Чем отличается меню русака зимой от летнего? Какой вред он может нанести садам и огородам зимой?
4. Секретная стратегия зайчат. Какое поведение зайчат помогает им выживать?
5. Без своего дома. Многие звери роют норы или строят гнёзда. А делает ли норы зайчиха? Где рождаются и живут маленькие зайчата?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Заяц-беляк",
                        imageResName = img("ph_0090_1"),
                        viewedId = "mamm_ph_0090_1",
                        text = """
Отряд Зайцеобразные, семейство Зайцевые. Немного мельче, чем русак. Хвост круглый, снизу белый. По краю уха идет светлая полоса. Зимой весь белый, только кончики ушей и хвоста темные. Предпочитает селиться в смешанных лесах, держится по окраинам и опушкам, где больше освещено. Может жить в лесополосах или зарослях кустарников. На открытой местности не встречается. Более оседлый, чем русак. Постоянных логовищ нет, делает временные лежки, зимой под снегом. Активен утром и вечером. Размножение начинается в феврале-апреле, чаще два помета (всего до 8 шт.) Зайчата рождаются зрелыми: зрячие и покрытые шерстью. В возрасте 9 дней начинают питаться самостоятельно. Летом едят травы, зимой кору. В природе встречаются гибриды с русаком (тумаки).
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Пищуха малая",
                        imageResName = img("ph_0122_1"),
                        viewedId = "mamm_ph_0122_1",
                        text = """
Пищуха малая. Отряд Зайцеобразные, семейство Пищуховые. Редкий вид, на юго-востоке саратовского Заволжья, нераспаханные земли на склонах балок, заросли степных кустарников. Большой урон численности пищух наносит распашка земель в местах ее обитания. Длина до 20 см, уши короткие округлые до 2 см. Хвост мал и снаружи не виден. Окраска темно-серая или буроватая, с продольной светлой «струйчатостью» по спине. Зимний мех в два раза выше и более светлый. Подошвы покрыты длинными темными волосами, закрывающими подушечки пальцев. Живут колониями, строят два типа убежищ: временные и постоянные норы с гнездовой камерой и несколькими входами и отнорками. Зимой делает ходы под снегом до 40 м. Активность дневная, но может быть и ночная. Детеныши появляются с мая по август. Выводков за год может 2-4 по 3-12 детеныша в каждом (слепые, голые, через 7-8 дней прозревают и покрываются шерстью). Звонкой мелодичной песней сообщают, что территория занята (поэтому названы пищухами). Пища: степные кустарники и разнотравье. На зиму запасают много сухой травы (до 7 кг), которую срезают и складывают в стожки (поэтому второе название зверьков – сеноставки. Сушат чаще на поваленных ветках, низких кустах. Красная книга Саратовской области.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Мастер подземных городов. Какие норы по роет этот зверек! Какой длины они могут достигать?
2. Родственники зайцев. Эти ближайшие родственники зайцев отличаются от них. Чем?
3. Летние "домики" для сена. Какое второе, очень интересное название дали пищухе за её привычку сушить траву на зиму и где она это делает?
4. Невидимый хвост и песни-объявления. Почему этого зверька сложно поймать за хвост и как он оповещает соседей, что территория занята?
5. Малыши-рекордсмены. Как быстро детёныши пищухи становятся самостоятельными: когда они прозревают и покрываются шерстью? Сколько может быть братьев и сестер в семье?
                        """.trimIndent()
                    )
                )
            )
            ,
            AnimalSection(
                title = "Отряд Грызуны",
                items = listOf(
                    AnimalTopic(
                        title = "Белка обыкновенная",
                        imageResName = img("ph_0077_1"),
                        viewedId = "mamm_ph_0077_1",
                        text = """
Белка обыкновенная.  Отряд Грызуны, семейство Беличьи. Кроме Городского парка (в 1999 было выпущено 40 особей), некоторых других мест Саратова и Кумысной поляны белки редко, но встречаются в природе, в лесистых районах области. Чаще заселяют сосновые, смешанные и липово-дубовые леса с примесью орешника. Всё у белок приспособлено к обитанию на деревьях: они прекрасные древолазы, могут совершать планирующие прыжки до 15 м. Используют готовые укрытия или искусно строят шарообразные гнёзда – гайно. Основа – ветки, внутри мох, лишайник, в нем тепло даже в морозы. Вход снизу. Питание: семена хвойных, орехи, желуди, ягоды, грибы, насекомые яйца и птенцы. Да, голодная белка (грызун) хищничает! На зиму запасает орехи, желуди, грибы (умеет сушить). Прекрасное обоняние помогает белке найти запасы под толстым слоем снега. Активны утром и вечером, в спячку не впадают. В начале лета появляется 4-5 бельчат (может быть второй помет). Самка строит запасное гнездо, в случае разрушения старого быстро перетаскивает потомство. Белки – природные носители инфекционных болезней.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Акробаты леса. Белки – прекрасные древолазы и могут совершать планирующие прыжки. А как ты думаешь, для чего им такие умения: чтобы спасаться от врагов, быстрее находить еду или, может, для чего-то ещё?
2. Уютный домик-шар. Белки строят шарообразные гнёзда – гайно. Вход в него всегда расположен снизу. Как ты считаешь, почему именно снизу? Что это даёт белке и её детёнышам? 
3. Неожиданный хищник. Обычно мы думаем, что белки едят только орешки и грибы. Но в тексте говорится, что голодная белка может хищничать. Кого именно она может съесть, проявляя свои хищные повадки?
4. Кладовая под снегом. Как белка находит свои запасы орехов и желудей под толстым слоем снега зимой? И почему она не просто прячет их в одном месте, а делает много тайников?
5. Мама-белочка и переезд. Зачем самка белки строит запасное гнездо? Что она делает, если с её основным домом что-то случилось?
6. Сон зимой и летом. Многие животные зимой впадают в спячку. А что делает белка? Когда она наиболее активна в течение суток?
7. Осторожно! Милая белочка! В конце текста есть важное предупреждение. Какое? Почему, даже если белка кажется очень милой и ручной, нужно соблюдать осторожность и не пытаться её трогать или кормить с рук.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Бобр речной",
                        imageResName = img("ph_0079_1"),
                        viewedId = "mamm_ph_0079_1",
                        text = """
Бобр речной. Отряд Грызуны, семейство Бобровые. Обычный, повсеместно встречающийся вид. Предпочитает небольшие реки с протоками и пойменными озерами, встречается по водохранилищам, каналам, затопленным карьерам. Если ручей недостаточно глубок, бобры строят плотины, чтобы входы в норы были затоплены водой. Самый крупным грызун нашего региона: длина тела с хвостом до 120 см, а масса тела до 30 кг. Животное хорошо приспособлено к полуводному образу жизни: может грызть под водой, закрываются ноздри и уши, специальное вещество для смазывания меха и др. Живут бобры поодиночке или семьями. Активны обычно в ночное время или в вечерних и утренних сумерках. Зимой активность дневная. В морозы из убежищ не выходят, питаются заготовленным с осени древесным кормом. Типы жилища бобра – хатки (из веток) и норы роют в крутых берегах (выходы под водой). Детеныши (3-4) появляются в апреле-мае, в возрасте 2 мес. начинают питаться растениями. Пища: кора и тонкие ветви деревьев «мягких» пород – осины, ив, водные и прибрежные растения. Там, где на реках бобров много, они вредят: поваленными деревьями делают реку и берега непроходимыми пешком и на лодках.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Строители плотин. Бобры строят плотины на речках и ручьях. Как ты думаешь, зачем они это делают? Зачем им глубокая вода у входа в жилище? (Подсказка: подумай о безопасности этих зверей).
2. Чемпион по размерам. Бобр — самый крупный грызун в Саратовской области. Насколько он большой? Сравни его размер и вес с каким-нибудь знакомым тебе предметом или животным (например, с кошкой).
3. Супер-способности ныряльщика. Бобр отлично приспособлен к жизни в воде. Какие особые «устройства» помогают ему нырять и грызть ветки под водой, не захлебываясь? (Подсказка: посмотри на его нос, уши, хвост и мех).
4. Дом под водой и над водой. У бобров бывает два типа жилищ. Какие? В чем главное отличие хатки от норы, и почему выход из любого дома бобра всегда находится под водой?
5. Ночной труженик. Когда бобры обычно выходят на работу (грызть деревья, чинить плотину)? А как меняется их режим дня зимой, в сильные морозы?
6. Меню лесного инженера. Что составляет основу еды бобра? Какие деревья они особенно любят и что делают с ними осенью, чтобы не голодать зимой?
7. Польза и… не только. Бобры в больших количествах могут создавать проблемы для человека. Какой вред они приносят своими постройками и вырубкой деревьев? Какое важное правило безопасности нужно помнить, если встретишь бобра или его хатку?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Емуранчик",
                        imageResName = img("ph_0088_1"),
                        viewedId = "mamm_ph_0088_1",
                        text = """
Отряд Грызуны, семейство Тушканчиковые. Редко встречается на юго-востоке Заволжья. Длина тела до 12 см, хвост до 15 см, с кисточкой на конце. Мех густой, на спине охристый, снизу белый. На ступнях «щетка» из волос, которая не дает проваливаться в песок. Питается зелеными частями травянистых растений и семенами. На зиму впадает в спячку.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Крыса серая",
                        imageResName = img("ph_0095_1"),
                        viewedId = "mamm_ph_0095_1",
                        text = """
Крыса серая. Отряд Грызуны, семейство Мышиные. Типичный синантропный вид, то есть, тесно связанный с человеком. Обычно зимой концентрируются в населенных пунктах, летом частично уходят в природные места. Часто это совпадает с переводом домашнего скота на свободный выпас. Вне населенных пунктов крысы обитают по берегам водоемов. Длина тела до 27 см, хвост короче. Окраска грязно-охристая, бурая. На лапах небольшие перепонки. Всеядна, но предпочитает животный корм. Наибольшая активность грызунов наблюдается в местах скопления корма: склады, подвалы, мусоросборники, фермы, жилища домашнего скота, а также частные и многоквартирные дома. Размножается круглый год: до 4 пометов в каждом по 7-9 детенышей. Агрессивна. Портит продукты и оборудование. Является переносчиком около 20 заболеваний и носителем гельминтов. Поэтому люди вынуждены постоянно проводить дератизацию (меры по уничтожению грызунов). Среди них химические (яды), механические (ловушки) и биологические (кошки и др.) Но в связи с высокой плодовитостью в местах с большим количеством пищи крысы выигрывают эту борьбу. Белые крысы, которых часто содержат как домашних питомцев и лабораторных животных – это белая форма не крысы серой, а другого вида – крысы черной.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Спутник человека. Почему она так тесно связана с жизнью человека? Что помогает ей выживать?
2. Хищник в обличье грызуна. Крыса — всеядный грызун, но у неё есть предпочтение. Какую пищу она выбирает чаще: растительную (зерно, овощи) или животную (мясо, яйца)?
3. Мал, да удал. Какой вред наносит серая крыса человеку? (Найди в тексте не менее трёх примеров).
4. Опасный сосед. Почему люди вынуждены постоянно бороться с крысами? Какая самая страшная опасность от них исходит для нашего здоровья? (Подсказка: речь не просто о порче вещей).
5. Мастер выживания. Почему, несмотря на все ловушки, яды и кошек, крысам часто удаётся выигрывать борьбу с людьми? Какая их суперспособность позволяет им так быстро восстанавливать свою численность? (Посчитай, сколько детёнышей может родить одна крыса за год!).
6. Защита и нападение. Какие три основных способа люди используют для борьбы с крысами? Что означает красивое слово «дератизация»?
7. Загадка белой крысы. У кого-то дома живут милые белые крыски. Являются ли они просто белой формой одомашненной серой крысы?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Мышовка лесная",
                        imageResName = img("ph_0105_1"),
                        viewedId = "mamm_ph_0105_1",
                        text = """
Отряд Грызуны, семейство Мышовковые. Водится на севере и западе Саратовской области, в широколиственных лесах. Длина тела до 7 см, хвост до 12 см (подвижный цепляющийся). Верх рыжевато-коричневый, низ сероватый, вдоль спины до начала хвоста темная полоса без светлой каймы (этим отличается от мышовки степной). Убежища делает в гнилой древесине, в прикорневых пустотах, иногда короткие норки. Спячка может длиться до 8 месяцев. Пища: в основном насекомые, также семена и ягоды. В период создания пар издают высокие трели. Размножение 1 раз в год (в начале июня), 5-6 детенышей в гнезде из меха и травы. Носитель возбудителей некоторых болезней. На территории Саратовской области еще два похожих внешне и по образу жизни видов: мышовка степная и мышовка Шрандта. Виды довольно редкие и недостаточно изученные.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Мышь домовая",
                        imageResName = img("ph_0106_1"),
                        viewedId = "mamm_ph_0106_1",
                        text = """
Мышь домовая. Отряд Грызуны, семейство Мышиные. Распространена повсеместно в поселениях человека. Встречаются и «дикие» популяции, часто в давно заброшенных деревнях, в Заволжье в степных местностях с достаточным количеством травянистой растительности. Длина тела до 9 см, хвост примерно такой же. Большие уши. Мех темно-серый сверху и от пепельного до совсем белого снизу. Имеет специфический «мышиный» запах. В домах активны и в зимнее время, в природе строят норы и впадают в спячку. Летом перемещаются на природу, на поля, засеянные яровыми культурами. Активна в сумерках и ночью, в домах круглые сутки. Основа питания – семена злаковых, из сочных кормов свекла и капуста, редко животная пища. В домах поедают любые продукты и даже что-то не очень съедобное. Очень плодовиты: до 10 приплодов за год, в каждом 3-12 детенышей, которые через месяц становятся половозрелыми! В периоды массового размножения численность может увеличиваться в десятки раз. Сильно вредят человеку, поедая и загрязняя продукты, запасы, корма домашних животных в домах и на складах. Люди вынуждены постоянно с ними бороться самыми разными способами: химическими (яды), механическими (ловушки), биологическими (кошки). Могут быть переносчиками около 20 болезней.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Жилец без справки и прописки. В какое время суток (года) этот зверек может хозяйничать на нашей кухне?
2. Мышиная соцсеть. Какое хобби эти мыши указали бы в графе «любимое занятие»? (Подсказка: к какому отряду они относятся?)
3. Куда с чемоданами? Летом могут массово перемещаться, куда и зачем?
4. Пара мышей – стихийное бедствие. Попробуй объяснить почему. (Подсказка: ты знаешь, что означает слово «плодовитость»?
5. Преступление – наказание. За что люди особенно не любят домовых мышей?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Мышь лесная желтогорлая",
                        imageResName = img("ph_0107_1"),
                        viewedId = "mamm_ph_0107_1",
                        text = """
Мышь желтогорлая. Отряд Грызуны, семейство Мышиные. Распространена в Правобережье, в пойменных широколиственных лесах, лесополосах. Длина тела до 14 см, хвост такой же или длиннее. В отличие от малой лесной имеет желтое пятно на горле и груди. Живет в норах (до 5 м), в дуплах и искусственных гнездовьях на деревьях. Два пика размножения: весенний и осенний, 2-3 приплода по 5-6 детенышей. Пища: семена дуба, клена, лещины, шиповника, зеленые части растений. Желуди иногда уничтожает полностью: съедает и делает большие запасы. На зиму может перебираться в дома. Является носителем возбудителей нескольких болезней.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Мастер маскировки с фирменным знаком! По какому «знаку» можно отличить эту мышь от других.
2. Лесной строитель и квартирант. Где прячется желтогорлая?
3. Кто главный по желудям? Эта мышь – настоящий чемпион по сбору желудей! Она их и ест, и заготавливает. Что будет с дубовой рощей, если в ней поселится много таких мышей?
4. Сезонный переезд в гости к людям. Представь, что эта лесная мышь пишет объявление на сайте аренды жилья. «Ищу тёплую квартиру на зиму. Опыт проживания в дуплах и норках имеется. Люблю семена и ... (что ещё из текста?). Временно, до весны!»
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Мышь лесная малая",
                        imageResName = img("ph_0108_1"),
                        viewedId = "mamm_ph_0108_1",
                        text = """
Отряд Грызуны, семейство Мышиные. Обычный, местами многочисленный вид по всей области. Предпочитает редколесную местность, иногда встречается и в домах. Длина тела до 10 см, хвост примерно такой же. Серый тон спины, белесое брюшко. Активна в сумерках и ночью, прячется в естественных укрытиях, редко делает норы. Дает 2-4 приплода в год по 5-7 детенышей. Количество потомства зависит от плотности популяции и кормовой базы: семена древесных пород, ягоды, насекомые и зеленые части растений, могут подгрызать кору плодовых деревьев.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Мышь-малютка",
                        imageResName = img("ph_0109_1"),
                        viewedId = "mamm_ph_0109_1",
                        text = """
Отряд Грызуны, семейство Мышиные. Редкий, мозаично распространенный вид, встречается в пойменных лугах, редких лесах и кустарниках. Зимует в стогах, трухлявых пнях, в норах с запасом семян, иногда в домах. Длина тела до 7 см, хвост до 6, хватательный. Окраска рыжая, низ белый, граница резкая. Характерен мускусный запах. Активна почти круглые сутки, кроме жарких часов. Для потомства строит гнездо (шар 6-13 см) из травы на кустах или крепких травинках на высоте до 1 м. За лето до 3 выводков по 3-10 детенышей. Пища: семена злаков, бобовых некоторых деревьев, насекомые, зеленые части растений. Носитель возбудителей некоторых болезней.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Мышь полевая",
                        imageResName = img("ph_0110_1"),
                        viewedId = "mamm_ph_0110_1",
                        text = """
Отряд Грызуны, семейство Мышиные. Обитает по всему Правобережью и на севере Левобережья, в разреженных лесах, предпочитает кустарниковые заросли и луга. Зимуют в стогах или жилищах человека. Длина тела до 12 см, хвост короче. Окраска серо-охристая, вдоль спины темная полоса, низ светлый. Питание: семена, ягоды, насекомые, зеленые части растений. Плодовиты: до 4 приплодов в год по 5-7 детенышей, которые через 2-3 мес. становятся половозрелыми. Вредитель зерновых культур, лесопитомников, садов, бахчей. Является носителем возбудителей некоторых заболеваний.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Ондатра",
                        imageResName = img("ph_0119_1"),
                        viewedId = "mamm_ph_0119_1",
                        text = """
Ондатра. Отряд Грызуны, семейство Хомяковые. Обычный, местами многочисленный вид. Впервые была завезена в Саратовскую область в 1936 г. (родом из Северной Америки). Встречается почти по всей области, на реках, прудах и водохранилищах, избегает водоемов с быстрым течением и где отсутствует рогоз и тростник. Длина тела до 35 см, хвост 80% от тела, уплощен с боков, голый, с чешуйками. Мех длинный, подпушь густая, не намокает. Окраска от светло-коричневой до почти черной. На водоемах с обрывистыми берегами роет норы с выходом в воду, на низких берегах строит хатки и растительности. Во время половодья, выгрызает норы в сплавинах (сбившихся в кучу деревьев). На водоеме должно быть достаточно корма: рогоз, тростник, камыш, ежеголовник, стрелолист, плавающие кувшинки и рдесты. Устраивает «столовые» в укромных местах, где часто ест, в том числе и моллюсков, раков, мелкую рыбу, лягушек. Детеныши рождаются в начале мае (7-8), голые слепые, но растут очень быстро, на 10 день могут плавать, на 20 сами едят растения. Могут быть еще выводки. На зиму в спячку не впадает, делает ходы под снегом, отдушины во льду. Промысловый вид ради меха, может быть переносчиком возбудителей ряда заболеваний.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Чужестранка из далёких краёв. Откуда родом ондатра, и как она оказалась в Саратовской области?
2. Приспособления водные. Как и чем ондатра приспособлена к жизни в воде?
3. Архитектор на выбор. Какие два типа домов строит ондатра в зависимости от берега водоёма? 
4. Меню подводного гурмана. Что входит в рацион ондатры, кроме водных растений?
5. Зимние приключения подо льдом. Впадает ли ондатра в спячку и как она дышит, когда водоём покрыт льдом?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Пеструшка степная",
                        imageResName = img("ph_0121_1"),
                        viewedId = "mamm_ph_0121_1",
                        text = """
Отряд Грызуны, семейство Хомяковые. В степях Правобережья и Заволжья. Малочисленный вид. Часто селится на залежах, выгонах, по обочинам дорог. Длина 12 см, хвост 2 см. Верх буро-охристый, вдоль спины тонкая черная полоска, низ желтоватый. Лапы сильно опушенный. От полевок, обитающих вместе с ней отличается полоской на спине и коротким хвостом. Живет колониями, роет разветвленные норы трех типов: гнездовые, кормовые и временные с двумя выходами, на поверхности появляется на короткое время. Плодовита: до 7 выводков в год по 6-7 детенышей. Питается зелеными частями растений и корневищами, делает запасы сухой травы. Природный носитель возбудителей туляремии.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Полевка водяная",
                        imageResName = img("ph_0123_1"),
                        viewedId = "mamm_ph_0123_1",
                        text = """
Отряд Грызуны, семейство Хомяковые. В Саратовской области обычный широко распространенный вид. В Правобережье в поймах Медведицы и Хопра (была обычна в пойме Волги до создания водохранилища), в Левобережье Б.Иргиза, Узеня, Еруслана, в искусственных прудах испытывает конкуренцию со стороны ондатры. В Александровогайском районе редко встречается полевка общественная, еще один редкий вид полевка-экономка (в пойме среднего Хопра, в Заволжье в пойме Еруслана, Иргиза.) Длина до 25, хвост до 12 см. Окраска однотонная, две формы: более темная и светлая. Плодовита: в год 4-6 выводков, общее потомство за год до 60 шт. Роют норы или наземные жилища, кормовые столики. Питание: растения, насекомые, мелкая рыба, моллюски. На зиму откочевывает от берегов из-за паводков. На зверьках много эктопаразитов. Называют еще «водяной крысой» за некоторое сходство.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Полевка общественная",
                        imageResName = img("ph_0124_1"),
                        viewedId = "mamm_ph_0124_1",
                        text = """
Отряд Грызуны, семейство Хомяковые. Редкий вид, в Заволжье на востоке и юго-востоке (сухие злаково-полынные степи, полупустыни). Длина до 11 см, хвост до 2 см. Окраска светло-песчаная до охристо-серой. Живет обширными колониями, норы неглубокие, но разветвленные (до 40 входов и 10 гнездовых камер), делает запасы. Размножение с марта по октябрь, 2-3 выводка по 6-8 детенышей. В малоснежные зимы колонии вымерзают полностью. Пища: травы (злаки и бобовые), зимой ветки кустарников, семена. В периоды вспышек размножения могут вредить с/х культурам. Носитель возбудителей некоторых заболеваний.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Полевка обыкновенная и восточноевропейская",
                        imageResName = img("ph_0125_1"),
                        viewedId = "mamm_ph_0125_1",
                        text = """
Полевки обыкновенная и восточноевропейская. Отряд Грызуны, семейство Хомяковые. Распространенный вид области. В Правобережье и Заволжье встречается на лугах, по окраинам лесов, в агроценозах, иногда в парках, садах. В зимнее время могут перемещаться в населенные пункты. Тело до 14, хвост до 5 см (характерно для полевок). Темно-серая окраска. Вид-двойник – полевка восточноевропейская (их можно различить только по хромосомам, внешний вид и биология очень схожие). Живут колониями, роют неглубокие норы с многими входами, делают небольшие запасы. Зимой ходы под снегом. Весной и летом по 3-4 выводка (по 5 шт), зимой могут еще 2-3 раза вывести потомство, если много корма (в зернохранилище, например). Питание: летом сочные травы, зимой подземные части растений и семена. Носители возбудителей некоторых болезней.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Природные двойники. Какой вид похож на полевку обыкновенную и почему их называют двойниками?
2. Дом на скорую руку. Почему полевок нельзя назвать мастерами по рытью, как выглядят их норы?
3. Зимний переезд в город. Почему зимой обыкновенную полевку можно встретить не только в полях, но и в деревнях или городах?
4. Семейные рекорды Сколько раз за год обыкновенная полевка может стать мамой, сколько малышей может быть в одном выводке?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Полевка рыжая",
                        imageResName = img("ph_0126_1"),
                        viewedId = "mamm_ph_0126_1",
                        text = """
Отряд Грызуны, семейство Хомяковые. Во всех типах лесов Правобережья и на северо-западе Заволжья. Длина до 12, хвоста до 6 см (редкие волоски). Верх ржаво-коричневый, низ серый. Размножение начинается еще под снегом, 3-4 выводка (иногда до 6) по 3-10 детенышей. Пища: зеленые части растений (до 170 видов), семена, редко личинки насекомых. Носитель возбудителя ГЛПС (геморрагической лихорадки с почечным синдромом) и некоторых других болезней.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Полевка-экономка",
                        imageResName = img("ph_0127_1"),
                        viewedId = "mamm_ph_0127_1",
                        text = """
Отряд Грызуны, семейство Хомяковые. Редкий, мозаично распространенный вид, в Правобережье в поймах Волги, Хопра, Медведицы, в Заволжье в поймах Б.Иргиза, Еруслана. Длина тела до 14, хвоста до 6 см. Окраска от темно-шоколадной до светлой, низ пепельно-серый. Надежное определение только по зубной системе. Активна круглые сутки, круглый год. Роет норы с разветвленной системой ходов, делает запасы. Размножение с апреля, по 2-3 выводка по 6-8 детенышей Пища: летом сочная трава, зимой ветки и кора кустарников. Носитель возбудителей некоторых болезней.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Слепыш обыкновенный",
                        imageResName = img("ph_0130_1"),
                        viewedId = "mamm_ph_0130_1",
                        text = """
Слепыш обыкновенный. Отряд Грызуны, семейство Слепышовые. Обычный вид со стабильным ареалом и численностью. Местами грызун многочислен, что видно из-за обилия выбросов земли. Предпочитаемые места обитания – увлажненные участки с развитой злаково-разнотравной растительностью. Длина тела до 29 см.  Глаза не развиты и скрыты под кожей. Губы плотно смыкаются позади резцов; при рытье резцами земля в ротовую полость не попадает. Норы состоят из одной или нескольких жилых камер, расположенных на глубине 1.5–2 м с сетью горизонтальных кормовых ходов. Длина норы может быть до 250 м. На поверхность слепыш выталкивает горки земли, похожие на кротовины, поэтому часто думают, что это сделали кроты. Каждая особь имеет индивидуальную нору. Прокладывая кормовые ходы, подгрызает корни растений, повреждает клубни, луковицы, корнеплоды. В кладовые натаскивает большие запасы корма. Зимой активен, так как в спячку не впадает. Приносит определенный вред посевам сельскохозяйственных культур, а также вредит лесопосадкам, портит сенокосные угодья, посевы многолетних трав.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Незамерзающий кладовщик. Чем слепыш занимается зимой и почему ему не нужно спать?
2. Дом-лабиринт. Если бы слепыш решил устроить забег по своему подземному дому, какое расстояние ему бы пришлось пробежать от начала до конца?
3. Очки не в моде. Почему слепыш может не беспокоиться о том, чтобы протереть свои глаза от пыли?
4. Чистюля-копатель. Что помогает слепышу не наесться земли, когда он роет норы своими большими передними зубами?
5. Фермерский кошмар. Что случается с огородом или полем, с растениями на них, когда под ним начинает хозяйничать слепыш?
6. Холмики-обманки Если ты увидишь на поле в ряд кучки выброшенной земли, кто их сделал (в Саратовской области) и почему это точно не крот?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Слепушонка обыкновенная",
                        imageResName = img("ph_0131_1"),
                        viewedId = "mamm_ph_0131_1",
                        text = """
Слепушонка обыкновенная. Отряд Грызуны, семейство Хомяковые. Обычный вид в Заволжье, в Правобережье в поймах Хопра, Медведицы, Терешки. Тело вальковатое, до 15 см, хвост очень короткий. На передних конечностях острые когти для копания. Глаза очень маленькие, ушных раковин нет. Мех короткий густой буроватый, низ светлее. В северной части Правобережья встречаются зверьки меланисты, с черной окраской. Подземный образ жизни, на поверхности появляется редко, ночью. В спячку не впадает, зимой менее активна. У семьи (около 10 особей) выводковая нора с гнездовой камерой на глубине 1-4 м, туалеты, кладовые. Сложная система ходов с входами, закрытыми земляными пробками, которые зверьки открывают после дождей и в жару. Может быть до 4 выводков в год, в каждом 3-5 детенышей. Пища: клубни, луковицы, корневищами, делает небольшие запасы. Вред: повреждает подземные части огородных и бахчевых культур, саженцев кустарников. Носитель возбудителей некоторых болезней.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Крошечные лопаточки. Какой специальный инструмент есть у слепушонки для копания? Чем она в этом отличается от слепыша?
2. Ночной невидимка. В какое время суток и как часто эта зверушка рискует выйти на поверхность из своей норы?
3. Многодетная семья. Сколько может быть детенышей у самки за год? 
4. Закрыто-открыто. Чем слепушонка закрывает вход в свой дом и в какую погоду она решает его «откупорить»? 
5. Подземный кладовщик. Что любит есть слепушонка, и делает ли она запасы на черный день?
6. Зимний режим. Меняется ли жизнь слепушонки, когда наступает зима, и засыпает ли она крепким сном, как сурок?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Соня лесная",
                        imageResName = img("ph_0133_1"),
                        viewedId = "mamm_ph_0133_1",
                        text = """
Отряд Грызуны, семейство Соневые. Обычный, повсеместно встречающийся в лесах вид со стабильной численностью. Длина тела до 12 см, хвост до 11. От носа к основанию уха проходит черная полоса. Обитает в широколиственных и смешаных лесах, кустарниковых зарослях по балкам, в садах, лесопитомниках, полезащитных лесополосах. Бегает по тонким веткам в любом направлении, так же проворна и на земле. Активность преимущественно ночная и сумеречная. Селится в дуплах деревьев, иногда строит открытые шарообразные гнезда из веток и листьев. Настоящих запасов на зиму не делает. В зимнее время переселяется в норы с гнездовой камерой, выстланной по стенкам сухой травой, где впадает в спячку. Питание: желуди, орехи, ягоды, плоды, семена; весной преобладают почки и молодые побеги, велика роль животных кормов – насекомых и их личинок, птенцов и яиц птиц, а также детенышей мышей и полевок. Название «соня» связано с длительной спячкой (8 мес. в году).
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Соня-полчок",
                        imageResName = img("ph_0134_1"),
                        viewedId = "mamm_ph_0134_1",
                        text = """
Соня-полчок. Отряд Грызуны, семейство Соневые. Многочисленный вид, в Правобережье по пойме Волги, в лесах. Из сонь самая крупная, до 20 см, хвост до 15 см. Уши короткие, округлые, могут двигаться независимо. Пища: ягоды и другие поды, орехи лещины, весной плюс животные корма, летом листья. Мех густой, длинный, пушистый (раньше заготавливали шкурки), буровато-серый, низ белый. Нет черной маски через глаза, как у лесной сони. Вибриссы (усы, органы осязания) до 6 см, собраны в пучки. Кисти и пальцы цепкие, для передвижения по тонким веткам. Активна в сумерках и ночью. Прыгает в кронах до 10 м! В году активна 4 месяца, остальное в спячке. Укрывается в дуплах, в прикорневых пустотах, иногда по нескольку особей из разных семей. Размножение с июня по август, один выводок (2-10 детенышей). Вред садам, поедает плоды. Красная книга Саратовской области, необходим запрет на отлов зверьков для домашнего содержания. Название «соня» связано с длительной спячкой (до 8 мес. в году), а «полчок», возможно, из-за щелкающих звуков, которые она издает.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Точно соня. Сколько длится спячка у этого зверька (плюс еще дневной сон)? Хотел бы ты столько спать? Почему?
2. Белка-летяга? На сколько может прыгнуть, перемещаясь по ветвям деревьев в темноте? Что помогает ей лазать?
3. Чувство в темноте. Что такое вибриссы, для чего, и какой длины они у сони-полчка?
4. Трапеза по сезону. Что ест? Почему ее не любят садоводы?
5. Дружная квартира. Где ее укрытия? Почему эти места можно назвать «коммунальной квартирой»?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Сурок степной (байбак)",
                        imageResName = img("ph_0135_1"),
                        viewedId = "mamm_ph_0135_1",
                        text = """
Сурок степной (байбак). Отряд Грызуны, семейство Беличьи. В Правобережье обитает на Приволжской возвышенности, в Левобережье в восточных районах. Вид, которому в настоящее время не угрожает уменьшение численности благодаря принятым мерам охраны (исключен из Красной книги). Промысел его не разрешен, но браконьеры продолжают добывать. Сурки гибнут также от собак, хищных птиц (беркут, орел-могильник и степной), которые охотятся на молодняк. Сурки – активные копатели. Вокруг входа образуется высокий холмик из вынутого грунта – сурчина. Глубина норы до 3-4 м, общая длина десятки метров, гнездовая камера, туалеты, несколько входов. На каждую гнездовую нору 2-5 временных, в 15-50 м от главной норы и соединены с ней заметными тропками. Зверьки привязаны к своим норма и не отходят более чем на 100 м. В норе живет семья до 8 особей. Из спячки выходят в апреле, сначала вялые, на поверхности проводят не более 2 часов, к концу апреля активность до 12 ч. Начинается линька, которая длится до августа. В мае молодые сурчата (3-6) выходят из нор, сразу начинают есть растения. Пища: растения сем Злаки, Бобовые, маревые. Культурные растения поедают крайне редко, если только их поселения рядом с посевами. Количество пищи за сутки до 500 г. К концу августа количество накопленного жира до 20% от веса тела. В сентябре начинается уход в спячку, сурки закупоривают входы пробками из земли и растений. Название «байбак» на тюркском языке «сурок», отсюда латинское название Marmota bobak и английское Bobak marmot.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Суперстроитель. Какую квартиру они себе строят?
2. Тяга к дому. На какое расстояние сурок обычно прогуливается от своего жилища? Почему в поселениях сурков так много тропинок?
3. Выход в свет (и обратно). Когда можно увидеть сурков, вышедших из спячки? Какие они, что делают? И когда уходят в зимнюю спячку?
4. Детский сад на лужайке. Когда и сколько маленьких сурчат впервые выходят из норы? Чем питаются?
5. Летний гардероб. Какая важная и долгая процедура происходит с апреля по август, связанная с их шубкой?
6. Внимание! Враги! Кого суркам надо бояться?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Суслик большой (рыжеватый)",
                        imageResName = img("ph_0136_1"),
                        viewedId = "mamm_ph_0136_1",
                        text = """
Суслик большой (рыжеватый). Отряд Грызуны, семейство Беличьи. Широко распространен в Заволжье, островками в Правобережье. Обитатель разнотравных и луговых степей, на легких почвах (его называют еще луговой суслик). Длина тела до 34 см, хвоста до 11 см. Окраска коричнево-охристая с белой струйчатостью из-за светлых кончиков остевых волос. Верх головы серебристый, над глазами светлая полоса. Пища: злаки, кроме ковыля, разнотравье, всего около 50 видов растений. Выводковые норы глубиной от 40 см до 150 см, несколько выходов и камер, временные проще. Зимняя спячка 7-8 мес. Выход из спячки в апреле, уход в августе. Могут в поисках более кормных мест проходить расстояния более 500 м и даже переплывать реки. В выводке 3-16 детенышей. Являются переносчиками возбудителей некоторых болезней.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Разнообразное меню. Суслик, хоть и вегетарианец, но меню разнообразит. Чем он питается?
2. Дом для малышей. Чем отличается «квартира», в которой мать выращивает потомство, от её временного убежища?
3. Вынужденный путешественник. Что заставляет суслика отправиться в дальнюю дорогу (на сколько далеко)? Какую преграду он может преодолеть?
4. Рекордный выводок. Сколько братьев и сестер может быть у маленького суслёнка?
5. Сезонный график. В какие месяцы суслик ведет наиболее активную жизнь?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Суслик желтый",
                        imageResName = img("ph_0137_1"),
                        viewedId = "mamm_ph_0137_1",
                        text = """
Отряд Грызуны, семейство Беличьи. Распространен в Заволжье, на островах Волгоградского водохранилища. Самый крупный суслик нашей фауны, длина тела до 40 см, хвоста до 12 см.  спина и верх головы песчано-желтые, оттененные темными окончаниями остевых волос, без крапчатости. Живот и горло светлые. Живут компактно только в благоприятных местах, чаще норы разбросаны по большой территории. Норы на возвышениях, не заливаемых водой, гнездовая камера (с сухой растительностью) на глубине 2 м, ходы могут быть до 16 м. Активность утром и вечером, днем в норе, закрытой пробкой из влажного песка. Из спячки выходит в апреле, но уже через 3-4 месяца впадает в тепловой оцепенение (спасается от жары). Детеныши (5-12) появляются в мае. Пища: зеленые и подземные части растений, весной ветки кустарников и сухие растения. Переносчики возбудителей некоторых болезней.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Суслик малый и крапчатый",
                        imageResName = img("ph_0138_1"),
                        viewedId = "mamm_ph_0138_1",
                        text = """
Отряд Грызуны, семейство Беличьи. Распространен в Заволжье, островками в Правобережье, тенденция к снижению численности. Обитатель пустынных степей. Мелкий, длина тела до 26, хвоста до 4 см. окраска буроватая, мелкие крапинки, низ светлый, хвост со светлой каймой. Живут колониями много лет на одном месте, норы разных типов. На месте многолетних нор формируется возвышение – сусликовина из вытащенного грунта, используют как наблюдательный пункт. Выход из спячки в апреле. Детенышей 3-8. Пища: зеленые части и семена злаков. В спячку уходит в сентябре, запасов не делает. Вредитель посевов и лесопосадок. Переносчик возбудителей чумы и других заболеваний. В Саратовской области еще один суслик крапчатый – редкий вид с сокращающейся численностью. До 26 см, по светло-коричневому фону светлые пятна до 6 мм, вокруг глаз белые круги. Детенышей 3-7, питание степными растениями, из-за малочисленности вреда с/х не наносит. Переносчик возбудителей туляремии.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Тушканчик большой",
                        imageResName = img("ph_0139_1"),
                        viewedId = "mamm_ph_0139_1",
                        text = """
Тушканчик большой. Отряд Грызуны, семейство Пятипалые тушканчики. В области распространен повсеместно, даже в черте г. Саратова, в Заволжье по оврагам и балкам. Самый крупный тушканчик, длина тела до 26 см, хвост на треть длиннее. Мех буровато-серый, низ и нижние части лап белые. На кончике хвоста хорошо развито «знамя». Уши длинные. Живет в одиночных норах, гнездовых (глубина до 2,5 м) и временных (летних) укрываться в случае дождя или опасности. Активен ночью. Детеныши (3-4) рождаются в конце мая. Спячка с середины ноября до середины марта. Пища: весной подземные части растений, летом – семена злаков и насекомые. Может вредить посевам. Носитель возбудителей нескольких болезней. Сто лет назад был промысловым видом, заготавливали в больших количествах шкурки для меховых изделий. В настоящее время численность резко снизилась, мех уже не нужен. Данный вид в Красной книге Саратовской области.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Великан среди сородичей. Какой длины тело и хвост у этого зверька?
2. Флаг на мачте. Что за «знамя» есть у тушканчика и зачем оно нужно ему? 
3. Архитектор-индивидуал. Сколько разных квартир есть у тушканчика, и для чего ему нужна летняя временная норка?
4. Ночной гурман. Чем отличается весеннее меню тушканчика от летнего? Почему фермеры их не любят?
5. «Мех тушкана» Почему сейчас мех тушканчиков не используют?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Тушканчик малый и тарбаганчик",
                        imageResName = img("ph_0140_1"),
                        viewedId = "mamm_ph_0140_1",
                        text = """
Отряд Грызуны, семейство Пятипалые тушканчики. В отличие от большого, тушканчик малый – редкий слабоизученный вид, тарбаганчик (земляной заяц) еще более редкий и малочисленный. Малый: длина до 12, хвост до 17 см. Мех короткий, знамя двухцветное, уши длиннее, чем у других тушканчиков. Тарбаганчик: длина до 13 см, хвост на треть длиннее, уши короткие, направлены вперед. Размножение два раза в год, в выводке по 3-6 детенышей. Питание характерное для тушканчиков (выкапывают подземные части растений, насекомые, семена). Вредители огородных и бахчевых культур.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Хомяк обыкновенный",
                        imageResName = img("ph_0142_1"),
                        viewedId = "mamm_ph_0142_1",
                        text = """
Хомяк обыкновенный. Отряд Грызуны, семейство Хомяковые. По Правобережью Саратовской области распространен повсеместно, в Левобережье локально. Крупный грызун, длина до 35 см. Верх охристо-бурый, низ черный. На боках передней части два белых пятна, разделанные черной вставкой. Защечные мешки могут вмещать до 50 г корма. Образует группы только в период размножения. Роет сложные глубокие норы (до 10 выходов). В год 2-3 выводка, по 10 детенышей (рекорд 20!). С октября уходит в спячку, которая часто прерывается. Питание: растения. На зиму большие запасы семян и клубней, каждый вид запасов отдельно. Существует много версий происхождения названия. Дальний родственник хомячкам, которых содержат как питомцев.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Сумки-кладовки. Какие особые «карманы» есть у хомяка, и сколько вкусняшек он может в них унести за раз?
2. Одинокий хозяин. Когда хомяк соглашается жить не один, а в компании себе подобных?
3. Подземное убежище. Сколько выходов может быть из норы? Что и как хранит в кладовой?
4. Рекордсмен по деторождению. Сколько раз в год появляются детеныши и сколько их может быть в одном выводке (а в рекордном)?
5. Сон с пробуждениями. В какое время года хомяк уходит в спячку, и спит ли он всю зиму без перерывов?
6. Летнее меню. Что хомяк ест летом и для чего делает запасы?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Хомячок серый",
                        imageResName = img("ph_0143_1"),
                        viewedId = "mamm_ph_0143_1",
                        text = """
Хомячок серый. Отряд Грызуны, семейство Хомяковые. В Правобережье в большинстве районов, в Левобережье встречается гораздо реже, по участкам сохранившихся степей, лесополосам. Длина тела до 12 см, хвоста до 3,5. Окраска спины от темно до светло-пепельной, низ светлый. Уши без белой каймы, в отличие от других видов. На передних лапах шесть подушечек. Живет одиночно, активен в сумерках. Норы роет сам, простые с одной камерой, двумя входами, отнорками для запасов. До 5 выводков в год (по 5-6 детенышей). В настоящую спячку не впадает, только активность сильно снижается. Пища: весной зеленые части, летом – семена диких и культурных растений, плюс моллюски и членистоногие, делает запасы семян. Вред незначительный, потому что мала численность.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Норный житель. Живет в компании или в одиночку, и какие апартаменты он для себя роет?
2. Феномен плодовитости. Сколько раз за год у самки может появиться потомство и сколько в каждом выводке?
3. Зимний режим тишины. Как живется хомячку зимой? Делает ли запасы?
4. Всеядный гурман. Как меняется его меню от весны к лету, и включает ли оно что-то кроме растений?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Хомячок Эверсманна",
                        imageResName = img("ph_0144_1"),
                        viewedId = "mamm_ph_0144_1",
                        text = """
Отряд Грызуны, семейство Хомяковые. Обитатель сухих степей в Заволжье. Длина тела до 16 см, хвост до 3. Верх темно-бурый, низ белый, с резкой границей. На груди охристое пятно. На передних лапах пять подушечек, шестая не развита. Агрессивный, одиночно живущий, активен в сумерках и ночью. Норы роет простые или использует чужие. Отличительная черта нор – вертикальный вход 4 см в диаметре. Выводков 2-3 в год по 3-4 детеныша. Питание растительно-животное: семена, зеленые части, насекомые.
                        """.trimIndent()
                    )
                )
            )
            ,
            AnimalSection(
                title = "Отряд Хищные",
                items = listOf(
                    AnimalTopic(
                        title = "Барсук обыкновенный",
                        imageResName = img("ph_0080_1"),
                        viewedId = "mamm_ph_0080_1",
                        text = """
Барсук обыкновенный. Отряд Хищные, семейство Куньи. Размером со среднюю собаку, коротколапый, за счет густой шерсти тело мешковатое, серого цвета. Голова белая, через глаза идут две черные полосы. По окраске и размерам барсука можно опознать сразу. Хотя относится к хищникам, но всеядный: мыши, ящерицы, птенцы, лягушки, черви, ягоды, орехи. Живут в норах, в которых есть спальня, кладовая и туалет. Охотится ночью, днем в норе, с октября по март спит в норе. Близко к человеческому жилью не поселяется. В настоящее время количество барсуков в Саратовской области резко снизилось. Браконьеры убивают их ради жира, который считается целебным (к осени зверь может накопить жира до половины своего веса!). Внесен в Красную книгу области. Название, возможно, переводится как «лесная собака», а латинское Meles от слова «мед» (разоряет гнезда пчел и шмелей).

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Визитная карточка. По какому самому заметному признаку на морде барсука его можно сразу узнать, даже если он быстро промелькнет в лесу?
2. Хищник или вегетарианец? Барсук относится к отряду Хищные. Всегда ли он питается только мясом? Что ещё входит в его меню, и как такое животное правильно называть?
3. Роскошная квартира с удобствами. Барсук — отличный архитектор. Как устроена его нора? Какие отдельные «комнаты» или помещения в ней есть, и для чего они предназначены?
4. Соня и толстяк. Почему барсук так привлекает браконьеров осенью? Как его умение готовиться к спячке связано с серьёзной угрозой для его жизни?
5. Редкий сосед.  К чему привела деятельность браконьеров? Из-за чего этот зверь теперь находится в Красной книге Саратовской области?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Волк серый",
                        imageResName = img("ph_0081_1"),
                        viewedId = "mamm_ph_0081_1",
                        text = """
Волк серый. Отряд Хищные, семейство Псовые. Самый крупный хищник наших лесов. Основа питания – крупные копытные, но могут поедать и мышей. Изредка нападают на домашний скот. Нападение на людей в последнее время не отмечалось. При наличии пищи съедает до 2 кг мяса в день, но зимой может выдержать голодовку до 15 дней. Летом добавляет в рацион растительный корм. Преследуя добычу, волк может пробежать до 70 км! Единственный хищник наших краев, который может охотиться коллективно. Живут стаями по 5-10 особей. Постоянного места проживания не имеет, в конце весны волчица оборудует нору, где появляется 3, редко больше, детеныша, через 5 месяцев они покидают нору. Поголовье волков в области постепенно увеличивается из-за ослабленной охоты на них. В Заволжье приходят из Казахстанских степей. Название, возможно, от слова «волочь», так как зверь часто тащит свою добычу.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Чемпион по бегу на выносливость. Волк может пробежать очень большое расстояние, преследуя добычу. Сколько километров он способен преодолеть за одну погоню? Попробуй представить, сколько это — от твоего дома до ближайшего леса или города?
2. Уникальный охотник. В тексте говорится, что волк — единственный хищник в наших краях, который охотится определенным образом. Как именно? Почему такой способ охоты делает его очень успешным при добыче крупных животных, например, лосей?
3. Строгий режим питания. Волк может съесть до 2 кг мяса за день, но также способен долго обходиться без пищи. Сколько дней зимой он может выдержать голодовку? Почему выживание при нехватке пищи важно для жизни в дикой природе?
4. Кочевая жизнь и семейный очаг. Говорят, что у волка нет постоянного дома. Так ли это всегда? Зачем строится нора, когда она опустеет?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Выдра речная",
                        imageResName = img("ph_0082_1"),
                        viewedId = "mamm_ph_0082_1",
                        text = """
Выдра речная. Отряд Хищные, семейство Куньи. Длина тела до 70 см и хвост 50 см. Верх тела темный, а низ светлый. Между пальцами перепонки, это приспособление к полуводному образу жизни (100 м под водой преодолевает за 1 мин!). Очень осторожна, из норы выходит в темноте, далеко от водоема не уходит. Вход в нору под водой, для вентиляции прокапывает вертикальный ход на поверхность, маскирует его. Можно спутать с норкой. Питание: в основном рыба, но ест и лягушек, мышей, птиц, раков. Ловит чаще больную рыбу, потому считается «речным санитаром». Название «выдра» («поречня») у наших предков означало «водяной зверь». Очень редкий зверь, отмечены встречи на Медведице и Большом Иргизе. Внесена В Красную книгу области.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Прирождённый пловец. Между пальцами у выдры есть специальное приспособление, которое помогает ей быть отличной пловчихой. Что это такое и как это помогает ей в жизни?
2. Секретная квартира с подводным входом. Выдра очень осторожна и строит своё жилище с умом. Где находится вход в её нору и зачем она прокапывает дополнительный, вертикальный ход, который тщательно маскирует?
3. Речной санитар. Выдру называют «речным санитаром». Как ты думаешь, почему она заслужила такое уважительное прозвище? Что в её питании делает её полезной для реки?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Горностай",
                        imageResName = img("ph_0085_1"),
                        viewedId = "mamm_ph_0085_1",
                        text = """
Горностай. Отряд Хищные, семейство Куньи. Длина тела до 25 см, хвоста до 10 см. Лесной зверек, довольно часто встречающийся в нашей области. Очень похож на ласку, только в полтора раза крупнее, кончик хвоста черный. Иногда поселяется рядом с хозяйственными постройками или прямо в них. Еда: мыши, птицы, лягушки, крупные насекомые, иногда ягоды. Прожорливый, за сутки может съесть до 1/3 своего веса! За год горностай уничтожает до 2000 мышей! В старину их даже держали вместо кошек, а из шкурок горностая шили дорогие королевские мантии. Охотится ночью, зимой чаще под снегом, увидеть его трудно. Самка приносит до 15-18 детенышей. Красная Книга Саратовской области.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Похож, но не совсем. Горностай очень похож на ласку. По какому самому заметному признаку, кроме размера, их можно отличить друг от друга, даже если они быстро промелькнут?
2. Маленький, но очень прожорливый. Насколько прожорлив горностай? Сколько еды он может съесть за сутки относительно своего веса и сколько мышей может уничтожить за целый год? Как ты думаешь, это полезно для человека?
3. Исторический факт. В тексте сказано, что в старину горностаев даже держали вместо кошек. Как ты думаешь, какую работу они выполняли для людей?
4. Невидимый зимний охотник. Горностай охотится ночью, а зимой – особенно скрытно. Где и как он ищет добычу в холодное время года, и почему его так сложно увидеть?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Куница лесная",
                        imageResName = img("ph_0093_1"),
                        viewedId = "mamm_ph_0093_1",
                        text = """
Куница лесная. Отряд Хищные, семейство Куньи. Обычный лесной вид, распространена в лесах Правобережья, в Левобережье по долине р. Большой Иргиз. Длина тела до 50 см, хвост до половины тела. Окраска одинаковая по всему туловищу, каштановая, кончик хвоста и лапы темные. Округлые ушки с белой каемкой. На груди желтоватое светлое пятно, из-за него эту куницу называют «желтодушка». Подушечки пальцев опушенные, когти длинные загнутые, это приспособление к лазанию по деревьям. Прячется в дуплах живых или сухих деревьев, в кучах валежника. Охотится чаще в кронах деревьев, рано утром и вечером, зимой днем. Основа питания грызуны (белки, сони), птицы-дуплогнездники, реже беспозвоночные, орехи и сочные плоды. Детеныши (обычно 3-5) рождаются в июле.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Прозвище «желтодушка». Из-за чего же её так называют?
2. Прирождённый верхолаз. Охотится часто в кронах деревьев. Подумай, что ей в этом помогает и кого она может там поймать?
3. Распорядок дня лесной охотницы. Когда лесная куница обычно выходит на охоту? А зимой?
4. Неожиданное в меню. Хищник, а какие растительные «десерты» она иногда употребляет в пищу?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Куница каменная",
                        imageResName = img("ph_0094_1"),
                        viewedId = "mamm_ph_0094_1",
                        text = """
Куница каменная. Отряд Хищные, семейство Куньи. По размерам и окраске очень похожа на куницу лесную. Различаются горловым пятном. У каменной пятно подковообразное, белое, поэтому ее называли «белодушкой». Охотится на земле, зимой избегает чащи леса, лапы опушены меньше, чем у лесной, поэтому она больше проваливается в снег. Основа питания птицы, гнездящиеся на земле, плоды (падалица). Основа питания в городе – голуби, а также мыши и крысы.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Сестрицы-куницы. Лесную куницу называют «желтодушкой», а как называют каменную? Из-за чего?
2. Почему она не любит глубокий снег? Охотится на земле. Какая особенность её лап (по сравнению с лесной куницей) мешает ей зимой ходить по рыхлому снегу?
3. Городская охотница. Чем каменная куница питается в городе?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Кот степной",
                        imageResName = img("ph_0099_1"),
                        viewedId = "mamm_ph_0099_1",
                        text = """
Кот степной. Отряд Хищные, семейство Кошачьи. Этот вид – недавний вселенец в Саратовское Заволжье (зарегистрированы встречи с 90-х годов прошлого века). В тростниковых зарослях тростника и кустарников по берегам, в широких лесополосах. Длина тела до 70, хвоста до 30 см (тонкий, заостряющийся на конце). Окраска желтоватая, светлая, по всему телу пятнистость. Предок домашней кошки, гибриды встречаются вблизи населенных пунктов. Одиночный образ жизни, детеныши (2-7) в апреле-мае, слепые, глухие, в окраске полосы и пятна. Питание: мышевидные, мелкие птицы, молодняк зайцев и сусликов. Случайно добывается при охоте на других зверей. Должен быть полный запрет на добычу. Красная книга Саратовской области.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Кошка, кто твой предок? От кого произошла домашняя кошка? Где в Саратовской области он редко, но встречается?
2. Мастер маскировки. Какая окраска помогает коту оставаться невидимым? Зачем ему это?
3. Охотник в тростниках. Где именно в природе поселяется степной кот? На кого чаще всего охотится?
4. Надо ли этого кота охранять? Что может случиться, если люди будут на него охотиться? Можно ли его поймать и держать дома как обычную кошку?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Ласка",
                        imageResName = img("ph_0100_1"),
                        viewedId = "mamm_ph_0100_1",
                        text = """
Ласка. Отряд Хищные, семейство Куньи. Самый маленький хищник нашей области. Длина тела самцов до 26 см, хвост до 7 см, самки на треть меньше. Тонкое вытянутое тело на очень коротких ногах. Голова маленькая, такая же как толстая шея, мех короткий густой. Зимой полностью белая, летом верх коричневый, низ белый. Обитает в пойменных лесах, довольно многочисленна, исключена из Красной книги. Может обитать рядом с жилищем человека. Охотится с перерывами, чаще в утренние и вечерние часы, зимой и днем. Сама нор не роет, прячется у грызунов или в других разнообразных укрытиях. Детенышей рождается до 8. Основа питания – мыши. Но иногда охотится и на более крупных зверей и птиц. За сутки может съесть больше, чем весит сама. Часто меняют охотничьи участки, когда мышей там становится мало.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Крошечный, но грозный. Самый маленький хищник Саратовской области. Как ты думаешь, какой примерно длины её тело, с кем сравнить? Несмотря на свой размер, на кого она может охотиться?
2. Зимний и летний камуфляж. Какая у нее окраска летом? А зимой? Почему ей выгодно быть такой? 
3. Большой аппетит в маленьком теле. Ласка очень прожорлива. Сколько она может съесть по сравнению со своим весом? 
4. Бездомный охотник. Ласка не роет собственных нор. Где она может прятаться для отдыха? И что заставляет её часто менять место жительства? (Подсказка: связано с её главной пищей).
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Лисица корсак",
                        imageResName = img("ph_0101_1"),
                        viewedId = "mamm_ph_0101_1",
                        text = """
Отряд Хищные, семейство Псовые. Вид локально распространен в правобережье Саратовской области, численность снижается. Ранее встречался в левобережных районах. Меньше обыкновенной лисицы, длина до 60 см, вес до 3 кг. Хвост в половину тела. Зимний мех палево-серый, с охристым оттенком, вдоль спины коричневый, летом более тусклый. Ноги коротковатые, голова с узкой длинной мордой. Норы может рыть сам, но охотно занимает чужие. Детеныши (2 и до 10) появляются в марте. Выводок остается с самкой до осени, иногда до весны. Питание: насекомые, степные грызуны, птицы, редко растения. Молодых особей ловят другие хищники.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Лисица обыкновенная",
                        imageResName = img("ph_0102_1"),
                        viewedId = "mamm_ph_0102_1",
                        text = """
Лисица обыкновенная. Отряд Хищные, семейство Псовые. Длина тела до 90 см. Мех пушистый. Окраска с разной степенью «рыжести». Основной корм – мышевидные грызуны, дополнительный – птицы, насекомые и плоды. Для потомства самка делает нору простой формы, без боковых ходов (обязательно в сухом месте). В мае рождается 4-6 лисят, 1,5 месяца питаются молоком, еще 1,5-2 месяца в логове, а потом покидают его. Распространенный вид в нашей области, чаще встречаются в лесах или лесополосах, селятся по окраинам городов и поселков, дач. В зимнее время могут проникать в курятники. Возможные переносчики бешенства, поэтому их специально отстреливают, уменьшая численность и потенциальную опасность.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Рыжая плутовка. Мех пушистый, особенно зимой. А зачем ей такой пушистый хвост?
2. Меню от мышки до малины. Хищник, но её меню довольно разнообразно. На кого она чаще всего охотится? А что служит лишь вкусным дополнением в разное время года? 
3. Дом для семейства. Для своих малышей лисица роет специальную нору. Какое место она выбирает, насколько сложное это временное жилище? 
4. Детский сад в подземелье. Сколько лисят обычно появляется в лисьей семье, когда? Как долго они живут под присмотром матери, прежде чем начать самостоятельную жизнь?
5. Соседка-воровка. Почему лиса иногда поселяется рядом с населенными пунктами, дачами? Чем может навредить человеку?
6. Опасность! Почему этих красивых животных иногда приходится специально отстреливать?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Медведь бурый",
                        imageResName = img("ph_0104_1"),
                        viewedId = "mamm_ph_0104_1",
                        text = """
Отряд Хищные, семейство Медвежьи. С давних времен медведи водились на территории Саратовской губернии. Последний был застрелен на охоте в 1898 году. На фото медведь, сфотографированный в заповеднике, а не в Саратовской области.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Норка американская",
                        imageResName = img("ph_0114_1"),
                        viewedId = "mamm_ph_0114_1",
                        text = """
Норка американская. Отряд Хищные, семейство Куньи. Вид завезен в Саратовскую область 1950-х годах. Акклиматизация прошла успешно, вид многочисленный, но пострадали норки европейские, которые были вытеснены пришельцами в менее выгодные условия обитания и численность их резко сократилась. Норки двух этих видов очень похожи внешне: окраска темная, низ только немного светлее. Белый только подбородок и нижняя губа (у норки европейской еще белая верхняя губа). Хвост почти черный. По размеру американская немного больше. Детеныши (1-9) появляются в мае. Пища: моллюски, ракообразные, рыба, амфибии, рептилии, мелкие млекопитающие (обычно водяные полевки). Летом охотится чаще в сумерках. Зимой утром охотится в воде, а вечером на берегу. Активна круглосуточно, в спячку не впадает.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. «Миссис Белый Подбородок». Норка американская почти вся тёмная, но по этому светлому «знаку» можно отличить её от очень похожей на неё европейской норки. Что это? Какая окраска тела у норок?
2. «План «Захват»: история одного переселения. Этот вид был завезен в Саратовскую область в XX веке, но «поприветствовала» она местную норку не очень дружелюбно. Как ты думаешь, что случилось с европейской норкой после этого? 
3. Супер-меню водного хищника. Представь, что норка составляет меню для своего ресторана. Что в нём будет? Это будет очень разнообразное ассорти. Перечисли, кого она может поймать на обед.
4. Чемпион по бессоннице. В отличие от сурков, норка не знает, что такое спячка. Какой у нее график охоты летом и зимой, почему отличается?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Норка европейская",
                        imageResName = img("ph_0114_2"),
                        viewedId = "mamm_ph_0114_2",
                        text = """
Отряд Хищные, семейство Куньи. Редкий исчезающий вид. Тело до 43 см, хвост до 19, не пушистый. Короткие лапы с перепонками, более крупными на задних (приспособление к полуводному образу жизни). Мех густой и плотный, подпушь не намокает в воде. Окраска темно-коричневая, белая передняя часть морды, верхняя губа и подбородок (в отличие от американской норки). Одна постоянная нора с подводным и надводным входом и от 2 до 10 временных убежищ. Чаще использует бобровые норы. Детеныши (3-7) появляются в мае. Пища: мелкая рыба, лягушки, мыши. Активна круглосуточно, в спячку не впадает.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Перевязка",
                        imageResName = img("ph_0120_1"),
                        viewedId = "mamm_ph_0120_1",
                        text = """
Отряд Хищные, семейство Куньи. Редко встречающийся, исчезающий вид. Отдельные встречи на юго-востоке саратовского Заволжья. Размеры и пропорции как у хоря. Длина тела до 38 см, хвоста до 22 см.  Отличительная окраска: на темной морде над глазами и у носа белые полоски (перевязи), по бокам тела на темном фоне белые и желтые пятна. Предупреждающая окраска и выгибание спины и закидывание хвоста за спину при опасности – защита от крупных хищников. Свои норы не строит, доделывает норы грызунов. Размножение: февраль-март, детенышей 3-8. Активна в сумерках или рано утром. Пища: суслики, хомячки, тушканчики, мелкие птицы, ящерицы, лягушки. Распашка земель в местах обитания этого зверька – основная причина малой численности и угрозы исчезновения. Красная книга РФ и Саратовской области.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Рысь",
                        imageResName = img("ph_0128_1"),
                        viewedId = "mamm_ph_0128_1",
                        text = """
Рысь. Отряд Хищные, семейство Кошачьи. Редкий, локально распространенный вид. В Саратовской области рысь появилась и стала размножаться со второй половины 1980-х годов в северных районах Правобережья (Хвалынский, Вольский, Воскресенский, Базарно-Карабулакский, Балтайский и Петровский). В 1992 г. и 2003 г. самцов рыси добывали в Дьяковском лесу в Заволжье. Длина тела до 105 см, хвоста – до 20 см (кажется «обрубленным»), масса до 23 кг. Ведет одиночный образ жизни и обитает в высокоствольных лесах с густым подлеском и чащобными участками. Логово для детенышей устраивает в чащобе, валежнике, под корнями. Детеныши (почти голые, слепые) рождаются в логове в апреле-июне, чаще их 2-3, редко до 5. Зрячими становятся примерно через 2 недели, через месяц могут есть мясо, из логова начинают выходить через 2 месяца. С матерью остаются в течение года. Заботится о рысятах мать. Рысь активна ночью, ранним утром и в сумерках, питается преимущественно зайцами, лесными птицами и мышевидными грызунами. Может добывать также косуль и телят более крупных копытных, иногда нападает на домашних животных. Запасов не делает. Красная книга Саратовской области.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Привередливый квартирант. В каких лесах рысь обычно устраивает свой дом? 
2. Большая кошка с малышами. Сколько рысят (и какие они) обычно рождается в одном выводке, какие изменения происходят в их жизни в течение месяца?
3. Семейные традиции. Кто заботится о малышах? Сколько времени рысята живут с мамой? 
4. Ночной охотник. В какое время суток рысь выходит на прогулку и обед, и что у неё в меню?
5. Ловкость в лесу. Какой длины хвост у рыси и зачем он ей такой?
6. Неожиданный гость. В каком веке рысь впервые появилась в Саратовской области? Почему в Красной книге?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Собака енотовидная",
                        imageResName = img("ph_0132_1"),
                        viewedId = "mamm_ph_0132_1",
                        text = """
Собака енотовидная. Отряд Хищные, семейство Псовые. Редкий акклиматизированный вид (родина – Северо-Восточная Азия). Данные о численности в настоящее время не публикуются, разрешен отстрел в связи с тем, что они являются переносчиками бешенства, как и лисицы. Длина тела до 80 см, масса до 6 кг (к началу зимы жир составляет 30% массы тела, больше, чем у других псовых). Ноги довольно короткие. Хвост – четверть туловища. Голова с короткой острой мордой, уши мало выступают из меха. На морде черно-белая «маска». По бокам морды пушистые «баки» из длинной шерсти, торчат в стороны. Мех такой густой и длинный, что ноги почти не видны, косматый, жесткий, с густым пухом. Спина и блока темные, брюхо светлее. Сами нор не роют, занимают готовые или делают убежища в кучах валежника, в тростниковых зарослях. Пары образуют на один сезон, детеныши в мае (6-8, может быть и больше), слепые, с мягкой черной шерстью. На третьей неделе начинают питаться самостоятельно, к 5 мес. достигают размеров взрослых. Всеядна. Пища летом: мелкие позвоночные, насекомые и моллюски, к осени плюс плоды и корневища. Впадает в спячку, которая в оттепели может прерываться. При опасности затаивается или притворяется мертвой.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Маскировочный грим. Что делает эту собаку похожей на другого известного зверя? Какие украшения есть на голове?
2. Меню принесите! Чем питается эта собака? Чем ее меню отличается от волчьего?
3. Лентяй-строитель. Любит ли енотовидная собака рыть норы? Где рождаются щенки?
4. Зима? Это хорошо! Как ведет себя этот зверь зимой?
5. Хитрый артист. Какие две уловки может применять в случае опасности?
6. Сезонная семья. Надолго ли образуют пару самец и самка, и сколько у них обычно рождается пушистых щенков?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Хорь черный (лесной)",
                        imageResName = img("ph_0145_1"),
                        viewedId = "mamm_ph_0145_1",
                        text = """
Отряд Хищные, семейство Куньи. В настоящее время редкий вид. На территории Саратовской области в Правобережье и в Заволжье только в пойме Волги, численность везде низкая. Вытянутое гибкое туловище (до 36 см), хвост около трети тела, ноги короткие, очень длинные остевые темные волосы, подшерсток желтый. На голове «маска»: область глаз черная, лоб и морда белые. В начале 20 века в большом количестве добывали шкурки. В зимнее время концентрируется вблизи населенных пунктов. Поселяются в подвалах, на чердаках, в сараях. Весной самки с детенышами (в среднем 4-6) остаются в поселках, а самцы уходят в поймы рек. Питание: преобладают мышь домовая и крыса, зимой могут нападать на домашнюю птицу.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Хорь степной",
                        imageResName = img("ph_0146_1"),
                        viewedId = "mamm_ph_0146_1",
                        text = """
Хорь степной. Отряд Хищные, семейство Куньи. В Правобережье распространен по степных участкам, к северу численность снижается, в Заволжье обитает на степных участках, по окраинам населенных пунктов. Похож на лесного хоря, но крупнее, до 56 см. Хвост около трети туловища, с прилегающими волосами. Зимний мех пушистый, мягкий, светло-охристый, бурые остевые создают тонкий темный налет. На белой морде «маска» - черная полоса по глазам, на лбу темное пятно и затылок тоже темный. Уши белые. Ведет полукочевой образ жизни, перемещаясь за грызунами. Детеныши появляются в мае (3-6, рекорд 18!). Летом основа питания – суслики (зимой может выкапывать их из нор) и мыши. Сумеречная активность, в жару в норах. Хоря ради меха добывают браконьеры. Вид внесен в Красную книгу Саратовской области.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Летний охотник. На кого хорь охотиться летом, почему его называют полукочевым хищником?
2. Зимнее преследование. Что ест хорь зимой, ведь суслики спят?
3. Домосед в жару. Где он прячется в жару и когда выходит на охоту?
4. Рекордный выводок. Сколько детёнышей обычно рождается в мае (а рекорд)?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Шакал",
                        imageResName = img("ph_0147_1"),
                        viewedId = "mamm_ph_0147_1",
                        text = """
Шакал. Отряд Хищные, семейство Псовые. Редкий вид, отдельные встречи в Заволжье. Похож на волка, но длина тела до 75 см, хвост короткий. Окраска грязно-рыжевато-серая, спина черная. Край губ, подбородок, щеки, горло беловатые. Мех грубый. Держатся парами, редко семьями. Создают пары на всю жизнь, охотятся вместе. Норы (2 м с гнездовой камерой) роют сами или используют от лисиц и барсуков. Детеныши появляются в апреле-мае, 2-3 месяца на молочном вскармливании. Оба родителя выкармливают потомство мясом с 3 недельного возраста, отрыгивая пищу. Щенки держатся с матерью до осени, к зиме семьи распадаются. Пища: грызуны, птицы, реже лягушки, ящерицы, падаль, посещают свалки. Могу наносить вред животноводству зимой. Могут быть переносчиками возбудителя бешенства.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Верный романтик. Что можно сказать про семейные пары шакалов?
2. Генеральная уборка в природе. Почему шакалов можно назвать «санитарами»?
3. Разнообразное меню. Чем питаются шакалы? 
4. Сезонный детский сад. Как долго щенки шакала остаются с матерью, когда начинают есть мясо? 
5. Серый и «серый» Чем похожи и чем отличаются шакал и серый волк?
                        """.trimIndent()
                    )
                )
            )
            ,
            AnimalSection(
                title = "Отряд Парнокопытные",
                items = listOf(
                    AnimalTopic(
                        title = "Кабан",
                        imageResName = img("ph_0091_1"),
                        viewedId = "mamm_ph_0091_1",
                        text = """
Кабан. Отряд Парнокопытные, семейство Свиные. Длина тела в среднем 150 см, высота до 100 см. Жесткий волосяной покров чаще темного цвета, черный пятачок и копыта. Уши длинные и широкие. На следах кабанов видны 4 копытца, причем боковые хорошо развиты. У самцов верхние и нижние клыки торчат изо рта вверх. Всеядны: подземные части растений (основа питания), реже наземные части, мелкие животные, даже падаль. Могут приносить вред с/х угодьям, раскапывая и вытаптывая посадки. Летом для них обязательно наличие водоема для водопоя. Могут совершать дальние кочевки, переплывать на острова. Обычно кабаны устраивают себе временные лежки: просто углубление в земле летом и с настилом из растений в снегу. Логово для потомства имеет толстые стенки из растительности и крышу из веток. Поросята появляются в марте-мае (чаще 4-6, редко до 10), покрыты жесткой щетиной, полосатые. Летом стадо выходит на кормежку после заката и до рассвета. С наступлением холодов кормежка сдвигается на дневные часы. Зрение и слух развиты слабо, а обоняние очень хорошо. Зимой много кабанов гибнет из-за недостатка корма, особенно в снежные зимы. В охотхозяйствах их подкармливают. Является объектом промысла и спортивной охоты.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Хищник с копытами. Может ли такое быть? Какая особенность доказывает, что кабан парнокопытное, а что делает его похожим на хищника? 
2. Малыши в камуфляже. Маленькие кабанчики, в отличие от взрослых, имеют очень необычную окраску. Какая она? Почему это помогает им выживать в лесу?
3. Ночной житель, ставший дневным. Летом кабаны выходят кормиться ночью, а зимой меняют свои привычки. Когда они активны в холода и почему? С чем связана такая перемена?
4. Вредный «пахарь». Кабаны могут наносить вред полям и огородам, но не тем, что съедают урожай. Как именно они его портят? 
5. Почти слепой, но еду находит. У кабанов очень слабое зрение, но есть одно чувство, развитое невероятно хорошо. Какое? И как это помогает ему находить пищу под землёй и чувствовать опасность?
6. Зимняя столовая. Почему многие кабаны не могут пережить снежную зиму самостоятельно? И что делают люди в охотничьих хозяйствах, чтобы помочь им выжить в это трудное время?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Косуля европейская",
                        imageResName = img("ph_0092_1"),
                        viewedId = "mamm_ph_0092_1",
                        text = """
Косуля европейская и сибирская. Отряд Парнокопытные, семейство Оленьи. Редкий, малочисленный, слабо изученный вид. Встречи на западе области, в поймах Медведицы и Хопра. В разных типах лесов, предпочитает молодые сосновые посадки. Причины резкого снижения численности: вытесняется завезенным видом – косуля сибирская, перепромысел, браконьерство. Длина до 135 см, высота в холке у самцов до 90 см. Рога у самцов до 25 см, розетки боковых рогов растут из одной точки, а у сибирской косули через промежуток, более бугристые. Окраска летом-серо-бурая, зимой рыжая. Хвост 3 см. Подхвостовое пятно беловатое, меньше, чем у сибирской. Зимой держатся группами до 30 особей, гон в августе, самцы дерутся, издают лающие звуки. Детеныши в мае-июне, 1-2, окраска пятнистая, первую неделю прячутся лежа, потом могут ходить за матерью. Через месяц едят траву. Зимой веточный корм. Объект охоты. Более крупная косуля сибирская прекрасно прижилась, численность ее увеличивается, не смотря на отстрел охотниками.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Загадка двух сестёр. В лесах Саратовской области живут два вида косуль.  Какая из них всегда жила в Саратовской области? Откуда взялся другой вид?
2. Тихие малыши-невидимки. Новорождённые косулята первые дни жизни ведут себя не так, как многие другие детёныши. Что они делают и как их пятнистая шубка помогает им в это время?
3. Почему их стало мало?  Какие три основные причины привели к резкому сокращению численности коренного жителя – европейской косули? 
4. Кто побеждает в лесу? Какая косуля – европейская или сибирская – лучше прижилась в нашей области и чья численность увеличивается?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Косуля сибирская",
                        imageResName = img("ph_0149_1"),
                        viewedId = "mamm_ph_0149_1",
                        text = """
Отряд Парнокопытные, семейство Оленьи. Обычный, широко распространенный вид. По Саратовской области встречается повсеместно, в 2025 г насчитывали 24 тыс. особей. Более крупная, чем европейская, длина до 150 см, высота в холке до 100 см. Рога до 48 см. Летняя окраска серо-голубая, зимой рыжая. Летом держатся одиночно, зимой группами. Размножение и питание так же как у европейской косули.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Лось",
                        imageResName = img("ph_0103_1"),
                        viewedId = "mamm_ph_0103_1",
                        text = """
Лось. Отряд Парнокопытные, семейство Оленьи. В настоящее время населяет все обширные лесные массивы и крупные полезащитные полосы Саратовской области. Предпочитает смешанные и широколиственные (пойменные) леса с обилием полян, просек, болот, гарей и вырубок. В 2025 г насчитывалось 7,3 тыс. особей. Самое крупное млекопитающее Саратовской области. Длина тела до 3 м, высота в холке до 2,3 м (холка с горбом). Хвост 12 см. верхняя губа нависает над нижней, снизу на горле меховой кожный вырост – «серьга», хорошо развитый только у взрослых самцов. У самцов огромные чашеобразные рога (из-за их формы его в народе называют «сохатый», за сходство рогов с сохой для вспашки земли). Период размножения начинается в сентябре. Самцы устраивают поединки, ревут, агрессивны. В апреле рождаются 1-2 лосенка весом до 15 кг, длиной тела до 90 см. Почти сразу могут вставать на ноги. На 10-ый день ходят за матерью. Рога появляются к концу 1 года жизни. Окраска меха очень темная.  Лоси прекрасно плавают, могут нырять за едой, в воде спасаются от укусов комаров и других насекомых. Питается травянистыми растениями, ветками кустарников и деревьев. Объект промысловой и спортивной охоты, добывается ради мяса, рогов, шкуры. Основные враги – волки. Предпринимались попытки одомашнить и использовать лося как ездовое и молочное животное.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Горбун, да не верблюд, широколобый, нос-лопатой, а зовут его… Кто это? Почему такое народное название у лося?
2. Лесная дискотека. Представь, что лось идёт на дискотеку для зверей. Зачем ему может понадобиться его длинная мохнатая «серьга» на шее? (А если серьги почти нет, значит это...)
3. Пловец с копытами. Лоси — отличные пловцы и даже ныряльщики. Как ты думаешь, зачем такой гигант, ростом с микроавтобус, ныряет под воду? Чтобы найти клад или по более «вкусной» причине?
4. Драчливый сезон.  Осенью у лосей-пап начинается «брачный сезон». Они становятся драчунами и крикунами, почему они себя так ведут?
5. Ребёнок-лосёнок. Он рождается весом как два больших арбуза. Почти сразу встаёт на ножки, а через 10 дней уже путешествует по лесу с мамой. А когда у него, как у папы, появятся первые рожки-пуговки?
6. Домашний гигант. Люди пробовали сделать из лося домашнее животное, как корову или лошадь. Как ты думаешь, сложно ли доить лосиху или запрячь лося в сани? Возможно ли это? Какие «проблемы» могут возникнуть? 
7. И у великана есть враги. Это люди, волки и… назойливые насекомые! Какой умный способ придумал лось, чтобы спастись от комаров и мошек летом? (Подсказка: он не мажется кремом, а использует целое озеро!)
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Олень благородный",
                        imageResName = img("ph_0117_1"),
                        viewedId = "mamm_ph_0117_1",
                        text = """
Олень благородный. Отряд Парнокопытные, семейство Оленьи. В Саратовской области часто встречается в пойме Хопра, Медведицы, Волги. В 2025 г. их было 2,5 тыс. шт. Питание травой и ветками летом, в зимнее время съеденные ветки пережевывают на отдыхе вторично (как коровы). Требуется подкормка веточным кормом и солью. Крупнее пятнистого оленя (его завезли специально), высота в холке у самцов до 1,5 м. Окраска однотонная, коричневая, только молодые со светлыми пятнами. Околохвостовое светлое пятно распространяется наверх. Ноги длинные. У самцов рога обычно имеют до 5 отростков (сбрасывает в апреле-мае). Зимой стада смешанные (до нескольких десятков особей) лидер – самка. Перед появлением потомства (июнь, обычно по одному детенышу) олени держатся одиночно. Страдают от браконьеров.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Загадка светлого «зеркала». Шубка у взрослых оленей коричневая и одноцветная, а сзади околохвостовое светлое пятно («зеркало»). Очень оно нужно оленям, а для чего?
2. Семейные порядки в стаде. Зимой олени собираются в большие стада. Кто становится главным и ведёт за собой всех — большой самец с рогами или, может быть, кто-то другой? Когда олени живут одиночно?
3. Рога не навсегда. У самцов есть огромные красивые рога с пятью отростками. Но эти рога — как зимняя шапка: их носят только в холодный сезон! Что происходит с ними весной? А зачем вообще рога самцам оленей?
4. Оленья радость. Люди помогают оленям зимой, подкармливая их. Кроме веток, им обязательно нужна ещё одна необычная «вкуснятина». Какая?
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Олень пятнистый",
                        imageResName = img("ph_0118_1"),
                        viewedId = "mamm_ph_0118_1",
                        text = """
Отряд Парнокопытные, семейство Оленьи. Дальневосточный (уссурийский) пятнистый олень. Впервые в области появился в 1938 г. В 2025 г. их было 4 тыс. шт. Средние размеры: высота в холке самцов до 112, самок до 98 см. У самцов небольшие рога с 4 отростками. Летом: темная полоса на спине, светлая пятнистость на каштановом фоне, зимой: темно-коричневый, пятна блеклые. Обитает в водораздельных широколиственных лесах с полянами и речками, а также в пойменных лесах. Пища: трава, ветки кустарников и деревьев. В снежные зимы нуждается в подкормке. Олени полигамные, на одного самца до 10-20 самок. Гон в октябре, оленята (4-6 кг) рождаются в июне-июле, обычно по одному. Первые дни беспомощные, на 10 день начинают пастись. Самцы в апреле-мае сбрасывают рога, начинают расти молодые – панты. Хорошо плавает. Объект разведения для получения пантов и спортивной охоты.
                        """.trimIndent()
                    ),
                    AnimalTopic(
                        title = "Сайгак (сайга)",
                        imageResName = img("ph_0129_1"),
                        viewedId = "mamm_ph_0129_1",
                        text = """
Сайгак. Отряд Парнокопытные, семейство Полорогие. Древнее животное, пережившее шерстистых носорогов и мамонтов. Длина тела до 145 см, хвоста до 10 см, высота в холке до 80 см. Ноги тонкие, короткие, может высоко подпрыгивать (при опасности, чтобы осмотреться) и очень быстро бегает. Нос в виде толстого вздутого горбатого хоботка. Он помогает очищать воздух от пыли, согревать его или охлаждать (зимой). Рога только у самцов, до 30 см, полупрозрачные, лирообразные. Мех желтовато-рыжий, зимой серый. Детенышей (куралай по-казахски) чаще 2, весом 3 кг. Они рождаются в мае, через неделю могут ходить, через 2 месяца есть траву (но молоком питаются до 3 месяцев). Сайгаки собираются в огромные стада, совершают кормовые миграции, питаются степной растительностью. В 2025 г произошло нашествие десятков тысяч сайгаков из Казахстана в саратовское Заволжье, сильно пострадали фермерские хозяйства. В связи с резким увеличением численности предлагается исключить этот вид из Красной книги РФ и Саратовской области и разрешить отстрел.

а теперь, прочитав этот текст, попробуй ответить на данные вопросы

1. Нос-хобот. Зачем сайгаку такой необычный нос?
2. Семейные рога. У кого в семье сайгаков рога? Какие они?
3. Древний путешественник. С какими известными древними животными жил когда-то на планете сайгак?
4. Малыш-куралай. Что вы знаете о детенышах сайгака?
5. Скоростные ножки. Какие ноги сайгака, как он передвигается?
6. Массовый поход. Что необычного, связанного с сайгаками, произошло в 2025 г. в Саратовской области?
                        """.trimIndent()
                    )
                )
            )
        )
    )

    return listOf(mammals)
}


//-------------------------Птицы------------------------------

fun buildBirdsData(context: Context): List<AnimalTopic> {
    fun img(name: String) = name

    return listOf(
        AnimalTopic(
            title = "Самая большая и тяжелая",
            imageResName = img("ph_0150_1"),
            viewedId = "bird_ph_0150_1",
            text = """
Дрофа. Длина тела самцов до 110 см, размах крыльев до 2,5 м, вес до 16 кг. Самки почти вдвое меньше. Это самая тяжелая летающая птица. В Саратовской области вторая по численности популяция гнездящейся дрофы в Европе. Количество этих удивительных птиц сокращается из-за браконьерства и распашки земель (самки часто устраивают гнезда на пашне, птенцы гибнут, отравление химикатами через поедаемых насекомых). «Дрофа – дневная королева степей, невероятно осторожная и уязвимая птица-«вегетарианец», чья жизнь полностью зависит от сохранения целинных участков. Увидеть её токование или тяжёлый взлёт в саратовской степи – редчайшая удача и признак того, что здесь ещё жива дикая природа» (ИИ). Орнитологи предлагали Правительству сделать дрофу живым символом Саратовской области, пока вопрос не решен. Красная книга РФ и СО. Название в переводе с праславянского означает.
            """.trimIndent()
        ),
        AnimalTopic(
            title = "Самая большая дневная",
            imageResName = img("ph_0151_1"),
            viewedId = "bird_ph_0151_1",
            text = """
Беркут. Длина тела этой хищной птицы до 90 см, размах крыльев до 2,5 м. Таких же размеров может достигать орлан-белохвост и почти таких же степной орел (самки, самцы меньше). Все три вида редкие в Саратовской области, занесены в Красную книгу РФ и СО. Название: слово тюркского происхождения, в переводе – «большой орел». «Орёл» от праславянского слова «orьlъ» - «большая хищная птица».
            """.trimIndent()
        ),
        AnimalTopic(
            title = "Самая большая ночная",
            imageResName = img("ph_0152_1"),
            viewedId = "bird_ph_0152_1",
            text = """
Филин. Длина тела этого ночного хищника до 70 см, размах крыльев до 1,5 м. Невероятно богатое меню: из крупных зайцы, ежи, рыба до 1 кг, ящерицы, другие хищные птицы, падаль, может делать тайники с запасами. Охотится из засады, бесшумно пикируя на жертву. Естественных врагов у этой крупной птицы нет. «Филин – ночной властелин леса и степей, универсальный охотник» (ИИ). Сокращение численности из-за деятельности человека. Красная книга РФ и СО. Название: от славянского “хвилин» - издающий жалобные или пугающие звуки.
            """.trimIndent()
        ),
        AnimalTopic(
            title = "Самая большая на фермах",
            imageResName = img("ph_0153_1"),
            viewedId = "bird_ph_0153_1",
            text = """
Страус африканский. Эту самую крупную нелетающую птицу на Земле можно встретить в Саратовской области, но в природе, конечно, а в нескольких частных фермерских хозяйствах (Лысые горы, Бобылевка Романовского района, под Энгельсом). Страусоводство в области – редкая, экзотическая форма птицеводства. Название: в переводе с немецкого Straub и древнегреческого – «птица-верблюд» из-за внешнего сходства и обитания в засушливых местах.
            """.trimIndent()
        ),
        AnimalTopic(
            title = "Самая высокая",
            imageResName = img("ph_0154_1"),
            viewedId = "bird_ph_0154_1",
            text = """
Журавль серый. Продолжает список самых крупных птиц: высота до 120 см (длинные ноги). Вес до 7 кг. Размах крыльев до 2 м. Птицы удивительны тем, что часто танцуют, парами или группами (пик в период размножения). Пары создают на всю жизнь, вместе выводят потомство (2 птенца). Мастера маскировки и обманных действий для спасения птенцов. Перед отлетом могут собираться в стаи до сотни особей. Во время перелетов строгий порядок в летящей стае: летят «клином», у каждого свое место. Численность сокращается. Красная книга СО. Название: от старославянского, звукоподражательного слова «жеравль». Позже слово «журавль» перешло к колодцу с длинным рычагом (внешне напоминающим эту птицу).
            """.trimIndent()
        ),
        AnimalTopic(
            title = "Самая маленькая",
            imageResName = img("ph_0155_1"),
            viewedId = "bird_ph_0155_1",
            text = """
Королёк желтоголовый. Одна из самых маленьких птиц Европы: тело до 10 см, размах крыльев до 15, вес до 8 г. «Птица-шарик»: постоянно двигается в поисках корма в кронах деревьев. Маленькое гнездо – архитектурное чудо, подвешенное высоко, на конце веточки. Яркое пятно на голове служит для общения. В холод сбиваются в стайки, часто вместе с синицами и др. В области редкий, малочисленный вид, обитающий только в хвойных лесах. Красная книга СО. «Существование его полностью зависит от сохранения островков хвойных массивов. Увидеть или услышать эту «летающую самоцветную бусинку» — большая удача для натуралиста (ваш ИИ). Название: перевод с латинского – «маленький король» из-за золотистой короны на голове.
            """.trimIndent()
        ),
        AnimalTopic(
            title = "Самая маленькая хищная",
            imageResName = img("ph_0156_1"),
            viewedId = "bird_ph_0156_1",
            text = """
Воробьиный сыч. Длина до 19 см, размах крыльев до 40 см, вес до 80 г. В отличие от других сов охотится и днем (пища: часто птицы, мыши, летучие мыши, то есть добыча по размеру почти такая же как он сам). Еще одно отличие – делает на зиму запасы: замороженные тушки мышей. Гнездится всегда в дуплах дятлов. Часто мелодично тонко свистит, обозначая территорию. Крайне редкий вид, обитает в хвойных и смешанных лесах на северо-западе области, где есть дуплистые деревья. Красная книга СО. Название: слово «сыч» старославянское, подражает звукам, которые издают эти совы. Так еще называют нелюдимого, угрюмого человека.
            """.trimIndent()
        ),
        AnimalTopic(
            title = "Самая рыбоядная",
            imageResName = img("ph_0157_1"),
            viewedId = "bird_ph_0157_1",
            text = """
Зимородок. Длина тела 16 см, размах крыльев до 25 см. Отличный летун и ныряльщик. До 80% еды – рыба, остальное водные беспозвоночные. Удивительно яркая тропическая окраска, но у воды его трудно заметить, только когда срывается с ветки и летит вдоль реки. Терпеливый рыболов: сидит неподвижно, пикирует на рыбку, хватает, на ветке оглушает ее и проглатывает головой вперед. Если у зимородка в клюве рыба «хвостом к нему», то это значит он приготовился скормить ее птенцам. Гнездо устраивает в норе (!), которую роет сам. Редкий вид в области. «Сохранение зимородка напрямую зависит от бережного отношения к речным ландшафтам. Его присутствие говорит о здоровой экосистеме водоёма» (ИИ). Красная книга СО. Название:  птенцы появляются в начале лета, а не зимой. Скорее всего от слова «землеродок» - потомство ведь появляется в норе.
            """.trimIndent()
        ),
        AnimalTopic(
            title = "Самая длинноносая",
            imageResName = img("ph_0158_1"),
            viewedId = "bird_ph_0158_1",
            text = """
Вальдшнеп. В отличие от других куликов, живущих около воды, вальдшнеп освоил густые леса (перевод с немецкого – «лесной кулик»). При длине тела до 35 см, клюв его до 10 см. Это инструмент для добычи червей, насекомых из лесной подстилки. Мастерски прячется за счет своей пестрой окраски. Этих куликов можно увидеть весной и осенью, когда они пролетают через Саратовскую область. Объект охоты, но строго в определенное время. Как помочь? «Беречь леса, никогда не поджигать траву и рассказывать друзьям об этой удивительной «лесной звезде» с длинным клювом!» (ИИ)
            """.trimIndent()
        ),
        AnimalTopic(
            title = "Самая длиннохвостая",
            imageResName = img("ph_0159_1"),
            viewedId = "bird_ph_0159_1",
            text = """
Фазан. Дикий фазан приходит сам в Саратовскую область с более южных территорий или его специально вселяют в охотхозяйствах. Это ценный объект охоты. В природе живут скрытно в густых зарослях, чаще около воды. Питание очень разнообразно: от ягод, семян до насекомых и ящериц. Самец окрашен очень ярко («ходячая радуга»), с длинными золотистыми хвостовыми перьями, это нужно для привлечения самок в период размножения. Самки неяркие, пестрые (сидят в гнезде на земле). Своей окраской самец может соперничать с сизоворонкой, щуркой золотистой и зимородком. Название: от реки Фазис (в Древней Греции), оттуда в Европу привезли этих «золотых птиц». Страдает от хищников, браконьеров, поджогов травы, сильных морозов (можно организовать подкормку зерном в тех местах, где они точно водятся). «Встреча с переливающимся всеми цветами фазаном, который вдруг с шумом взлетает из-под ног, - это настоящее маленькое чудо нашей саратовской природы!» (ИИ)
            """.trimIndent()
        ),
        AnimalTopic(
            title = "Самая многодетная",
            imageResName = img("ph_0160_1"),
            viewedId = "bird_ph_0160_1",
            text = """
Серая куропатка. Дикие куриные птицы. Осенью и зимой живут стайками (так легче найти корм, увидеть опасность). Зимой держатся в лесополосах вокруг полей. Гнезда на земле, яиц до 20. Птенцы вылупляются в одно время и следуют за матерью. Еда взрослых: семена, плоды, птенцы едят насекомых. Объект охоты. Численность снижается из-за: хищников, охотников, морозов, химикатов на полях (птицы могут съесть семена с химией). В местах их обитания можно устроить (вместе со взрослыми) подкормку зерном. Рекорд по птенцам может побить большая синица: у них обычно два выводка за лето, до 12 яиц в каждом! Название: перевод с латинского «серая птичка».
            """.trimIndent()
        ),
        AnimalTopic(
            title = "Самая незаметная",
            imageResName = img("ph_0161_1"),
            viewedId = "bird_ph_0161_1",
            text = """
Выпь большая. В случае опасности птица вытягивается вертикально, клюв вверх и сливается своей окраской с окружающими тростниковыми зарослями. Терпеливо стоит в воде или около (как цапля), быстро хватает добычу длинным клювом. Ее еда: мелкая рыба, лягушки, головастики, жуки, редко мелкие грызуны. Прилетает весной, выводит птенцов и улетает в Африку. Редкий вид, Красная книга СО. Как помочь: не шуметь у воды в тех местах, где водится выпь, рассказывать о вреде «палов» - специальных поджогов для освобождения берегов от сухой растительности. Сделай проект и расскажи одноклассникам об этой интересной и редкой птице наших водоемов. Название: от древнерусского «выть» за громкие звуки, похожие на рев быка, слышные за 1 км. Народные названия «речной бык или бугай»
            """.trimIndent()
        ),
        AnimalTopic(
            title = "Самая ныряющая",
            imageResName = img("ph_0162_1"),
            viewedId = "bird_ph_0162_1",
            text = """
Баклан большой. Все у него приспособлено для подводной охоты: сильный клюв, шея, обтекаемое тело, лапы отставляет назад, все четыре пальца соединены перепонкой, чтобы грести. Может нырять до 3-4 м и быть под водой около 1 минуты. Рыбу ест, вынырнув из воды. После ныряния намокшее оперение сушит, растопырив крылья (типичная его поза). Живут большими колониями, охотятся стаями, летят клином (как гуси). Рыбаки высказывают недовольство, считая, что бакланы съедают много рыбы и в местах массового гнездования замусоривают территорию своим пометом. Название: в переводе с тюркского – «дикий гусь». «Баклан – удивительный ныряльщик, которого природа создала для жизни в воде. Он не враг, а важная часть речной экосистемы.» (ИИ)
            """.trimIndent()
        ),
        AnimalTopic(
            title = "Самая странно гнездящаяся",
            imageResName = img("ph_0163_1"),
            viewedId = "bird_ph_0163_1",
            text = """
Чомга (большая поганка). Красивая, грациозная птица, скользящая по воде («речная фея»). В период размножения у самца и самки отрастают на голове черные перья-рожки и рыже черный воротник. По земле ходит плохо, лапы приспособлены для плавания. Самое удивительное, что чомга строит плавучее гнездо, в котором высиживает птенцов. Отплывая от берега, спасается от наземных хищников. Долгое время птенцы плавают на спине матери. Еда: рыба, головастики, водные насекомые. Заглатывает свои перья, чтобы рыбьи кости не ранили желудок. Угрозы: беспокойство от людей на водоемах, особенно на моторных лодках, загрязнение. Редкий вид, Красная Книга СО. «Чтобы она не исчезла, нам нужно просто дать ей тихое место («зону покоя») для жизни.» (ИИ) Название: «поганка» из-за «поганого, плохо пахнущего мяса», «чомга» от тюркского «нырять, погружаться».
            """.trimIndent()
        ),
        AnimalTopic(
            title = "Самая норная",
            imageResName = img("ph_0164_1"),
            viewedId = "bird_ph_0164_1",
            text = """
Щурка золотистая. Еще один претендент на 1 место по яркости окраски. Но удивительна тем, что роет в песчаных обрывах норы длиной до 2 м для выведения птенцов. Поселяется большими, шумными колониями по нескольку десятков пар. Летает стремительно, может быстро менять направление, потому что охотится на насекомых в воздухе. Ее еда: пчелы, осы, шмели, стрекозы. Схватив пчелу, она садится и бьет насекомое об ветку. Редкий вид, Красная книга СО. Угрозы: уничтожение птиц пчеловодами, разрушение колоний, гибель от отравленных насекомых. Щурок очень интересно наблюдать в бинокль, если обнаружишь неподалеку их колонию. Подготовь сообщение и и расскажи о них в школе на уроке биологии. Название: подражание звукам, которые издает эта птица «щур-щур», в народе – «пчелоедка» (ест пчел».
            """.trimIndent()
        ),
        AnimalTopic(
            title = "Самая дальняя",
            imageResName = img("ph_0165_1"),
            viewedId = "bird_ph_0165_1",
            text = """
Мухоловка-белошейка. Размером примерно с воробья. Действительно ловит мух и других насекомых: выслеживает их с ветки-«присады», срывается с места, хватает и возвращается. Это птица-дуплогнездник, но сама гнездо не делает, занимает сделанные дятлами или людьми. Осенью покидает наши края (летит в Африку, на юг Сахары, это 5 тысяч км(!), а весной прилетает, чтобы вывести потомство. Так же, как и соловей.  Вид немногочисленный. Если там, где ты живешь, водятся мухоловки (этот вид или мухоловка малая), то можно сделать правильный домик и привлечь этих полезных для сада-огорода птичек. Название: «мухоловка», потому что ловит насекомых на лету, «белошейка» из-за белой полосы на шее.
            """.trimIndent()
        ),
        AnimalTopic(
            title = "Самая лазающая",
            imageResName = img("ph_0166_1"),
            viewedId = "bird_ph_0166_1",
            text = """
Поползень. Размером с воробья. Древесный акробат. Единственная наша птичка, которая может быстро перемещаться вниз по коре дерева, цепляясь только когтями. Дятлы и пищухи опираются на лапы и хвост. Если вход в дупло большой, птица частично замазывает его глиной, смешанной со слюной. Летом ест насекомых. Осенью делает запасы семян, желудей, прячет и помнит эти места. Его мощный клюв помогает раскалывать твердые орехи. Помощь зимой: организовать подкормку: кормушка или просто сало на веревочке. Название: от слова «ползать» (передвигается по стволу в любом направлении).  «Поползень — шустрый, умный и очень полезный обитатель наших лесов» (ИИ).
            """.trimIndent()
        ),
        AnimalTopic(
            title = "Самый искусный строитель",
            imageResName = img("ph_0167_1"),
            viewedId = "bird_ph_0167_1",
            text = """
Синица-ремез. Меньше воробья, длина до 10 см. Строитель самого сложного архитектурного сооружения: шарообразное гнездо из растительного пуха, травинок (склеивает это паутиной!), с входом-туннелем, подвешенное на конце тонкой веточки. Это спасает от хищников. Гнездится только по берегам водоемов. По веткам не прыгает, а ловко лазает, склевывая насекомых и семена. Перелетный вид. «Нарисуй удивительное гнездо ремеза, сделай в школе небольшой доклад об этой птице» (ИИ). Название: от славянских слов, означающих «птица, которая строит гнездо-варежку»
            """.trimIndent()
        ),
        AnimalTopic(
            title = "Самая умная",
            imageResName = img("ph_0168_1"),
            viewedId = "bird_ph_0168_1",
            text = """
Ворон. Отличается от обычной вороны тем, что оперение все черное. Крупная птица, длина тела до 60 см. Ученые считают это птицу самой умной, наравне с обезьянами. В полете может переворачиваться и лететь вниз спиной, долго парить. Пару создают на всю жизнь, вместе строят гнездо (используют его много лет)), охраняют территорию. Питается чаще падалью, но может есть всё: мелких животных, насекомых, ягоды, семена, рыбу и др. Ворон может издавать разные звуки, имеющие важное смысловое значение. Редкий оседлый вид. Название: подражание звукам «вор, вар» или связано цветом – «воронОй» - черный цвет.
            """.trimIndent()
        ),
        AnimalTopic(
            title = "Самая городская",
            imageResName = img("ph_0169_1"),
            viewedId = "bird_ph_0169_1",
            text = """
Сизый голубь. Самые обычные городские обитатели произошли от диких скалистых голубей, поэтому селятся на зданиях. Издавна была известна способность голубей находить дорогу к дому за сотни км (использовали как почтовых). Голубь, в отличие от других птиц, пьет воду, втягивая, как через соломинку. В Саратове до сих пор развито голубеводство (любители содержат их в голубятнях, выводят новые породы). В городе едят семечки, зерна, пищевые отходы (хлеб, особенно с плесенью, для них вреден). Быстро размножаются, выводя потомство до 2-3 раз в год. Из-за чего гибнут: вороны, кошки, машины, болезни, паразиты. Если хочешь помочь голубям – подкармливай правильно (зерновая смесь и не жаренного подсолнечника, овса, пшеницы), хлеб не давай! «Не гоняйся за птицами и не пытайся ловить птенцов. Испуг — большой стресс для них. Наблюдай за их интересной жизнью издалека» (ИИ). Название: от старославянских слов, означающих «светлая, сизая (голубая) птица»
            """.trimIndent()
        ),
        AnimalTopic(
            title = "Самая вокальная",
            imageResName = img("ph_0170_1"),
            viewedId = "bird_ph_0170_1",
            text = """
Соловей. Немного крупнее воробья, но более изящная. Окраска невзрачная, маскировочная, а вот пение удивительное, воспетое в стихах и песнях. Раньше этих птиц держали как домашних питомцев и устраивали выставки и соревнования по «пению». В природе песни соловьев можно услышать в мае-июне, вечером и ночью, это сигнал о том, что территория для выведения птенцов уже занята. Гнездо-чаша из травинок на земле или нижних ветках, хорошо замаскировано. Насекомоядная птица, приносящая пользу садам и огородам. Название: от древнеславянского слова «солОвый», которое означает «буроватый или серый» цвет.
            """.trimIndent()
        ),
        AnimalTopic(
            title = "Самая яркая лесная",
            imageResName = img("ph_0171_1"),
            viewedId = "bird_ph_0171_1",
            text = """
Иволга. Длина тела до 25 см. Яркая лесная птица: самец желтый с черными крыльями, у самки более блеклая окраска. Иволга может петь мелодично («владимирский соловей») и громко кричать как кошка, которой наступили на хвост (при опасности). Ест волосатых гусениц, любит сладкие ягоды и другие спелые сочные плоды. Обитает в наших краях недолго, выводит птенцов и рано улетает. Гнездо-корзинку подвешивает к веткам высоко на дереве. Название: от славянских слов «влага, волга», дословно «птица, предвещающая дождь»
            """.trimIndent()
        ),
        AnimalTopic(
            title = "Самая разноцветная",
            imageResName = img("ph_0172_1"),
            viewedId = "bird_ph_0172_1",
            text = """
Сизоворонка. Длина тела 30 см, размах крыльев до 60 см. «Летающая радуга», «европейский попугай» - это все про нее. Охотится с «присады» (сидит на ветке и пикирует вниз на добычу). Еда: насекомые, реже ящерицы, мелкие мыши. Прилетает в мае, роет норы в обрывах с гнездовой камерой, выстланной сухой травой. Улетает в начале сентября в Африку. Редкий вид, занесена в Красную книгу РФ и СО. Название: «сизо-зеленый, голубой окрас» и «воронка» из-за некоторого сходства с вороной, в том числе звуками. «Расскажи о ней друзьям, одноклассникам. Чем больше людей знает, что эта яркая птица редкая и нуждается в защите, тем лучше.» (ИИ) Соперники у нее по яркости и разноцветью окраски: зимородок, щурка золотистая, фазан дикий (самец).
            """.trimIndent()
        ),
        AnimalTopic(
            title = "Самая \"зимняя\"",
            imageResName = img("ph_0173_1"),
            viewedId = "bird_ph_0173_1",
            text = """
Снегирь. Птица в наших краях зимующая, на лето улетает в более северные районы. Еда летом: семена деревьев, зимой ягоды, почки. Пары создают на всю жизнь, заботятся друг о друге и о птенцах. Зимой могут пострадать от мороза, оледенения веток. В городах посещают кормушки. Популярный объект для изображения на открытках, картинах, герой стихов и сказок. Название: от слова «снег», потому что появляется зимой или от тюркского «сниг» - «красногрудый». Еще одна красивая зимующая птичка в наших краях – свиристель. Стайки быстро перемещаются от дерева к дереву в поисках висящих плодов, при этом характерно «свиристят». Часто в населенных пунктах. «Снегирь — это яркий символ русской зимы. Подкармливая его, ты не просто помогаешь птице выжить, но и делаешь свой город или село красивее и добрее!» (ИИ)
            """.trimIndent()
        ),
        AnimalTopic(
            title = "Самая хохлатая",
            imageResName = img("ph_0174_1"),
            viewedId = "bird_ph_0174_1",
            text = """
Удод. Красивая пестро окрашенная птичка, с полосками, как у зебры, а на голове еще и корона из перьев (рыжих с черными кончиками). Для гнезда использует старые норы и разнообразные убежища, достраивая их для себя. Самка удода, защищая гнездо, может «выстрелить» жидким, сильно пахнущим пометом. Клюв-пинцет для добывания насекомых из земли. Очень полезная птица для садов и огородов. Зимовать улетает в Африку. Название: от подражания звукам, которые издает птица – «уд-уд».
            """.trimIndent()
        )
    )
}

data class SearchFilterResult(
    val items: List<SearchIndexItem>,
    val hasMore: Boolean
)

fun filterSearchItems(
    allItems: List<SearchIndexItem>,
    query: String,
    limit: Int = 15
): SearchFilterResult {
    val q = query.trim().lowercase()
    if (q.isBlank()) return SearchFilterResult(emptyList(), hasMore = false)

    val matched = allItems.filter { item ->
        item.topic.title.lowercase().contains(q)
    }

    return SearchFilterResult(
        items = matched.take(limit),
        hasMore = matched.size > limit
    )
}

//-----------------Функция поиска-------------------------

fun buildSearchIndex(
    mainData: List<AnimalClassBlock>,
    dangerousData: List<AnimalClassBlock>,
    mammalsData: List<AnimalClassBlock>,
    birdsData: List<AnimalTopic>
): List<SearchIndexItem> {
    val result = mutableListOf<SearchIndexItem>()

    // 1) Пресмыкающиеся / земноводные
    mainData.forEachIndexed { blockIndex, block ->
        block.sections.forEachIndexed { sectionIndex, section ->
            section.items.forEachIndexed { topicIndex, topic ->
                result.add(
                    SearchIndexItem(
                        source = CatalogSource.MAIN,
                        classTitle = block.title,
                        sectionTitle = section.title,
                        topic = topic,
                        blockIndex = blockIndex,
                        sectionIndex = sectionIndex,
                        topicIndex = topicIndex
                    )
                )
            }
        }
    }

    // 2) Опасные животные
    dangerousData.forEachIndexed { blockIndex, block ->
        block.sections.forEachIndexed { sectionIndex, section ->
            section.items.forEachIndexed { topicIndex, topic ->
                result.add(
                    SearchIndexItem(
                        source = CatalogSource.DANGEROUS,
                        classTitle = block.title,
                        sectionTitle = section.title,
                        topic = topic,
                        blockIndex = blockIndex,
                        sectionIndex = sectionIndex,
                        topicIndex = topicIndex
                    )
                )
            }
        }
    }

    // 3) Млекопитающие
    mammalsData.forEachIndexed { blockIndex, block ->
        block.sections.forEachIndexed { sectionIndex, section ->
            section.items.forEachIndexed { topicIndex, topic ->
                result.add(
                    SearchIndexItem(
                        source = CatalogSource.MAMMALS,
                        classTitle = block.title,
                        sectionTitle = section.title,
                        topic = topic,
                        blockIndex = blockIndex,
                        sectionIndex = sectionIndex,
                        topicIndex = topicIndex
                    )
                )
            }
        }
    }

    // 4) Птицы (без section/class)
    birdsData.forEachIndexed { topicIndex, topic ->
        result.add(
            SearchIndexItem(
                source = CatalogSource.BIRDS, // временно заменим ниже (см. примечание)
                classTitle = "Птицы",
                sectionTitle = null,
                topic = topic,
                blockIndex = null,
                sectionIndex = null,
                topicIndex = topicIndex
            )
        )
    }

    return result
}

@Composable
fun SearchResultsBlock(
    items: List<SearchIndexItem>,
    hasMore: Boolean,
    onItemClick: (SearchIndexItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Результаты поиска",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        items.forEach { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onItemClick(item) },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val ctx = LocalContext.current
                    val resId = ctx.resources.getIdentifier(
                        item.topic.imageResName,
                        "drawable",
                        ctx.packageName
                    )

                    if (resId != 0) {
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = item.topic.title,
                            modifier = Modifier.size(56.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Нет", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.topic.title, fontWeight = FontWeight.Medium)

                        val pathText = buildString {
                            when (item.source) {
                                CatalogSource.MAIN -> append("Пресмыкающиеся / земноводные")
                                CatalogSource.DANGEROUS -> append("Опасные животные")
                                CatalogSource.MAMMALS -> append("Млекопитающие")
                                CatalogSource.BIRDS -> append("Птицы")
                            }

                            item.classTitle?.let {
                                append(" • ")
                                append(it)
                            }

                            item.sectionTitle?.let {
                                append(" • ")
                                append(it)
                            }
                        }

                        Text(
                            pathText,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        if (hasMore) {
            Text(
                "И так далее… (показаны первые 15)",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
        }
    }
}