package com.example.second_try

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Composable
fun InvisibleSecretButton(
    secretKey: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val dbRef = FirebaseDatabase.getInstance(
        "https://mental-health-72105-default-rtdb.europe-west1.firebasedatabase.app"
    ).getReference("Users").child(user!!.uid)

    var alreadyFound by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    // Проверяем, найден ли уже секрет
    LaunchedEffect(Unit) {
        dbRef.child(secretKey).get().addOnSuccessListener { snap ->
            alreadyFound = snap.getValue(Boolean::class.java) == true
        }
    }

    // Диалог при нахождении секретки
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Ура!")
                }
            },
            title = { Text("Ты нашёл секрет!") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Получаешь +30 малинок 🎉")
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        painter = painterResource(id = R.drawable.prize),
                        contentDescription = "Награда",
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
        )
    }

    // Невидимая зона клика
    Box(
        modifier = modifier
            .size(80.dp)
            .clickable(enabled = !alreadyFound) {
                if (!alreadyFound) {
                    showDialog = true
                    alreadyFound = true
                    dbRef.child(secretKey).setValue(true)
                    dbRef.child("cones").get().addOnSuccessListener { snap ->
                        val current = snap.getValue(Int::class.java) ?: 0
                        dbRef.child("cones").setValue(current + 30)
                    }
                }
            }
    )
}
