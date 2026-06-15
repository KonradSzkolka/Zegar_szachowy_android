package com.example.zegar_szachowy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zegar_szachowy.ui.theme.Zegar_szachowyTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Zegar_szachowyTheme {
                ChessClockApp()
            }
        }
    }
}

@Composable
fun ChessClockApp() {
    var selectedTimeMillis by remember { mutableStateOf<Long?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (selectedTimeMillis == null) {
            TimeSelectionMenu(onTimeSelected = { millis ->
                selectedTimeMillis = millis
            })
        } else {
            ChessClockScreen(
                totalMillis = selectedTimeMillis!!,
                onBack = { selectedTimeMillis = null }
            )
        }
    }
}

@Composable
fun TimeSelectionMenu(onTimeSelected: (Long) -> Unit) {
    var isCustomMode by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableIntStateOf(5) }
    var customSeconds by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Wybierz czas gry",
            fontSize = 28.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (!isCustomMode) {
            val options = listOf(
                "Bullet" to 3,
                "Blitz" to 5,
                "Rapid" to 10,
                "Klasyczne" to 60
            )

            options.forEach { (label, minutes) ->
                Button(
                    onClick = { onTimeSelected(minutes * 60 * 1000L) },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(vertical = 4.dp)
                ) {
                    Text(text = "$label: $minutes min", fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { isCustomMode = true },
                modifier = Modifier.fillMaxWidth(0.7f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text(text = "Custom", fontSize = 18.sp)
            }
        } else {
            // Panel Custom z "Spinboxami"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                TimeSpinBox(
                    label = "Min", 
                    value = customMinutes, 
                    onValueChange = { customMinutes = it },
                    range = 0..999
                )
                Text(":", fontSize = 32.sp, modifier = Modifier.padding(horizontal = 8.dp))
                TimeSpinBox(
                    label = "Sek", 
                    value = customSeconds, 
                    onValueChange = { customSeconds = it },
                    range = 0..59,
                    wrap = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row {
                Button(
                    onClick = { isCustomMode = false },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Anuluj")
                }
                Button(
                    onClick = { 
                        val total = (customMinutes * 60L + customSeconds) * 1000L
                        if (total > 0) onTimeSelected(total)
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("Start")
                }
            }
        }
    }
}

@Composable
fun TimeSpinBox(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange? = null,
    wrap: Boolean = false
) {
    val plusInteractionSource = remember { MutableInteractionSource() }
    val isPlusPressed by plusInteractionSource.collectIsPressedAsState()
    
    val minusInteractionSource = remember { MutableInteractionSource() }
    val isMinusPressed by minusInteractionSource.collectIsPressedAsState()

    // Używamy rememberUpdatedState, aby pętle widziały aktualne wartości
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    // Logika auto-repeat dla plusa
    LaunchedEffect(isPlusPressed) {
        if (isPlusPressed) {
            var delayTime = 400L
            while (isPlusPressed) {
                var next = currentValue + 1
                if (range != null && next > range.last) {
                    next = if (wrap) range.first else range.last
                }
                currentOnValueChange(next)
                delay(delayTime)
                delayTime = (delayTime * 0.8).toLong().coerceAtLeast(50L)
            }
        }
    }

    // Logika auto-repeat dla minusa
    LaunchedEffect(isMinusPressed) {
        if (isMinusPressed) {
            var delayTime = 400L
            while (isMinusPressed) {
                var prev = currentValue - 1
                if (range != null && prev < range.first) {
                    prev = if (wrap) range.last else range.first
                } else if (range == null && prev < 0) {
                    prev = 0
                }
                currentOnValueChange(prev)
                delay(delayTime)
                delayTime = (delayTime * 0.8).toLong().coerceAtLeast(50L)
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 14.sp)
        Button(
            onClick = {}, // Logika obsłużona w LaunchedEffect
            interactionSource = plusInteractionSource,
            modifier = Modifier.size(50.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("+", fontSize = 24.sp)
        }
        
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { 
                val newVal = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                val constrained = range?.let { r -> newVal.coerceIn(r) } ?: newVal
                onValueChange(constrained)
            },
            modifier = Modifier.width(80.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        Button(
            onClick = {}, // Logika obsłużona w LaunchedEffect
            interactionSource = minusInteractionSource,
            modifier = Modifier.size(50.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("-", fontSize = 24.sp)
        }
    }
}

@Composable
fun ChessClockScreen(totalMillis: Long, onBack: () -> Unit) {
    var timeLeft1 by remember { mutableLongStateOf(totalMillis) }
    var timeLeft2 by remember { mutableLongStateOf(totalMillis) }
    
    var moveCount1 by remember { mutableIntStateOf(0) }
    var moveCount2 by remember { mutableIntStateOf(0) }
    
    var activePlayer by remember { mutableStateOf<Int?>(null) }
    var isPaused by remember { mutableStateOf(true) }
    
    var player1Color by remember { mutableStateOf<Color>(Color.Blue) }
    var player2Color by remember { mutableStateOf<Color>(Color.Blue) }

    LaunchedEffect(activePlayer, isPaused) {
        while (activePlayer != null && !isPaused) {
            delay(100)
            if (activePlayer == 1) {
                timeLeft1 = (timeLeft1 - 100).coerceAtLeast(0)
                if (timeLeft1 == 0L) isPaused = true
            } else if (activePlayer == 2) {
                timeLeft2 = (timeLeft2 - 100).coerceAtLeast(0)
                if (timeLeft2 == 0L) isPaused = true
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ClockButton(
            timeMillis = timeLeft2,
            moveCount = moveCount2,
            backgroundColor = player2Color,
            isActive = activePlayer == 2,
            onClick = {
                if (activePlayer == null) {
                    // Kliknięcie góry na starcie -> Góra staje się Biała (wykonała ruch), ruch przechodzi na dół
                    player2Color = Color.White
                    player1Color = Color.Black
                    moveCount2 = 1
                    activePlayer = 1
                    isPaused = false
                } else if (activePlayer == 2 && !isPaused) {
                    // Kliknięcie swojego (aktywnego) zegara -> Ruch przechodzi na przeciwnika
                    activePlayer = 1
                    moveCount2++
                }
            },
            modifier = Modifier.weight(1f).rotate(180f)
        )

        Row(
            modifier = Modifier.fillMaxWidth().background(Color.DarkGray).padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack) { Text("Menu") }
            Button(onClick = { isPaused = !isPaused }, enabled = activePlayer != null) {
                Text(if (isPaused) "Start" else "Pauza")
            }
            Button(onClick = {
                timeLeft1 = totalMillis
                timeLeft2 = totalMillis
                moveCount1 = 0
                moveCount2 = 0
                activePlayer = null
                isPaused = true
                player1Color = Color.Blue
                player2Color = Color.Blue
            }) { Text("Reset") }
        }

        ClockButton(
            timeMillis = timeLeft1,
            moveCount = moveCount1,
            backgroundColor = player1Color,
            isActive = activePlayer == 1,
            onClick = {
                if (activePlayer == null) {
                    // Kliknięcie dołu na starcie -> Dół staje się Biały (wykonał ruch), ruch przechodzi na górę
                    player1Color = Color.White
                    player2Color = Color.Black
                    moveCount1 = 1
                    activePlayer = 2
                    isPaused = false
                } else if (activePlayer == 1 && !isPaused) {
                    // Kliknięcie swojego (aktywnego) zegara -> Ruch przechodzi na przeciwnika
                    activePlayer = 2
                    moveCount1++
                }
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ClockButton(
    timeMillis: Long,
    moveCount: Int,
    backgroundColor: Color,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (backgroundColor == Color.White) Color.Black else Color.White
    val borderModifier = if (isActive) Modifier.border(8.dp, Color.Red) else Modifier

    Surface(
        onClick = onClick,
        color = backgroundColor,
        modifier = modifier.fillMaxWidth().then(borderModifier)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Licznik ruchów w prawym górnym rogu (relatywnie do gracza)
            Text(
                text = "Ruchy: $moveCount",
                fontSize = 20.sp,
                color = contentColor,
                modifier = Modifier.align(Alignment.TopEnd)
            )
            
            // Czas na środku
            Text(
                text = formatTime(timeMillis),
                fontSize = 72.sp,
                color = contentColor,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val min = totalSeconds / 60
    val sec = totalSeconds % 60
    return "%02d:%02d".format(min, sec)
}

@Preview(showBackground = true)
@Composable
fun MenuPreview() {
    Zegar_szachowyTheme {
        TimeSelectionMenu(onTimeSelected = {})
    }
}
