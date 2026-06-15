package com.example.zegar_szachowy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zegar_szachowy.ui.theme.Zegar_szachowyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Zegar_szachowyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ChessClockApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun ChessClockApp(modifier: Modifier = Modifier) {
    // Stan przechowujący wybrany czas w minutach. null oznacza, że jesteśmy w menu.
    var selectedTimeMinutes by remember { mutableStateOf<Int?>(null) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (selectedTimeMinutes == null) {
            TimeSelectionMenu(onTimeSelected = { minutes ->
                selectedTimeMinutes = minutes
            })
        } else {
            // Tymczasowy ekran zegara
            ChessClockScreen(
                minutes = selectedTimeMinutes!!,
                onBack = { selectedTimeMinutes = null }
            )
        }
    }
}

@Composable
fun TimeSelectionMenu(onTimeSelected: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Wybierz czas gry",
            fontSize = 28.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        val options = listOf(
            "Bullet" to 3,
            "Blitz" to 5,
            "Rapid" to 10,
            "Klasyczne" to 60
        )

        options.forEach { (label, minutes) ->
            Button(
                onClick = { onTimeSelected(minutes) },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .padding(vertical = 8.dp)
            ) {
                Text(text = "$label: $minutes min", fontSize = 18.sp)
            }
        }

        Button(
            onClick = { /* Tu będzie logika dla Custom */ },
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text(text = "Custom", fontSize = 18.sp)
        }
    }
}

@Composable
fun ChessClockScreen(minutes: Int, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Wybrano: $minutes minut", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "Tu pojawi się zegar", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(64.dp))
        Button(onClick = onBack) {
            Text("Powrót do menu")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MenuPreview() {
    Zegar_szachowyTheme {
        TimeSelectionMenu(onTimeSelected = {})
    }
}
