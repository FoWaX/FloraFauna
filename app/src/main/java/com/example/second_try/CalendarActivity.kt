package com.example.second_try

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.second_try.ui.components.AppTopBar
import com.example.second_try.ui.theme.Second_tryTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CalendarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Second_tryTheme {
                CalendarScreen(onNavigateBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val dbRef = FirebaseDatabase.getInstance(
        "https://mental-health-72105-default-rtdb.europe-west1.firebasedatabase.app"
    ).getReference("Users").child(user!!.uid).child("calendar")

    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val today = remember { LocalDate.now() }

    var selectedDate by remember { mutableStateOf(today) }
    var notes by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var isCreatingNote by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Загрузка заметок при запуске
    LaunchedEffect(Unit) {
        dbRef.get().addOnSuccessListener { snapshot ->
            val loaded = snapshot.children.mapNotNull { snap ->
                val dateStr = snap.key
                val note = snap.getValue(String::class.java)
                if (dateStr != null && note != null) dateStr to note else null
            }.toMap()
            notes = loaded
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Календарь",
                onBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                selectedDate = today
                isCreatingNote = true
                showNoteDialog = true
            }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Добавить заметку")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            DatePicker(selectedDate = selectedDate, onDateSelected = {
                selectedDate = it
                isCreatingNote = false
                showNoteDialog = true
            })

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Заметка на ${selectedDate.format(dateFormatter)}:",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Text(
                text = notes[selectedDate.toString()] ?: "Нет заметки",
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    if (showNoteDialog) {
        NoteDialog(
            date = selectedDate,
            existingNote = notes[selectedDate.toString()],
            isCreating = isCreatingNote,
            onDismiss = { showNoteDialog = false },
            onSave = { note ->
                scope.launch {
                    // Сохраняем в Firebase
                    dbRef.child(selectedDate.toString()).setValue(note)
                    notes = notes.toMutableMap().also { it[selectedDate.toString()] = note }
                    showNoteDialog = false
                }
            }
        )
    }
}

@Composable
fun DatePicker(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val dateDialogState = rememberMaterialDialogState()

    Button(onClick = { dateDialogState.show() }) {
        Text("Выбрать дату: ${selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}")
    }

    MaterialDialog(dialogState = dateDialogState, buttons = {
        positiveButton("OK")
        negativeButton("Отмена")
    }) {
        datepicker(initialDate = selectedDate, title = "Выберите дату") {
            onDateSelected(it)
        }
    }
}

@Composable
fun NoteDialog(
    date: LocalDate,
    existingNote: String?,
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(existingNote ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
        title = {
            Text(if (isCreating) "Добавить заметку" else "Заметка")
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Текст заметки") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}
