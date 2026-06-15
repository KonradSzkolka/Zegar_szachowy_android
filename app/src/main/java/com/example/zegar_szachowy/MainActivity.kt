package com.example.zegar_szachowy

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.zegar_szachowy.ui.theme.Zegar_szachowyTheme
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

data class ClockSettings(
    val time1Millis: Long,
    val time2Millis: Long,
    val incrementMillis: Long = 0
)

data class GameState(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val timeLeft1: Long,
    val timeLeft2: Long,
    val moveCount1: Int,
    val moveCount2: Int,
    val activePlayer: Int?,
    val player1Color: Color,
    val player2Color: Color,
    val initialSettings: ClockSettings,
    val timestamp: Long = System.currentTimeMillis()
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

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
    var settings by remember { mutableStateOf<ClockSettings?>(null) }
    var loadedState by remember { mutableStateOf<GameState?>(null) }
    var showSavedMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            showSavedMenu -> {
                SavedGamesMenu(
                    onGameSelected = {
                        loadedState = it
                        showSavedMenu = false
                    },
                    onBack = { showSavedMenu = false }
                )
            }
            settings == null && loadedState == null -> {
                TimeSelectionMenu(
                    onSettingsSelected = { settings = it },
                    onOpenSavedGames = { showSavedMenu = true }
                )
            }
            else -> {
                ChessClockScreen(
                    initialSettings = settings ?: loadedState!!.initialSettings,
                    savedState = loadedState,
                    onBack = {
                        settings = null
                        loadedState = null
                    },
                    onSaveAndExit = { state ->
                        saveGame(context, state)
                        settings = null
                        loadedState = null
                    }
                )
            }
        }
    }
}

@Composable
fun TimeSelectionMenu(onSettingsSelected: (ClockSettings) -> Unit, onOpenSavedGames: () -> Unit) {
    var isCustomMode by remember { mutableStateOf(false) }
    var linkTimes by remember { mutableStateOf(true) }
    var hasIncrement by remember { mutableStateOf(false) }
    
    var min1 by remember { mutableIntStateOf(5) }
    var sec1 by remember { mutableIntStateOf(0) }
    var min2 by remember { mutableIntStateOf(5) }
    var sec2 by remember { mutableIntStateOf(0) }
    var incrementSec by remember { mutableIntStateOf(0) }

    if (isCustomMode) {
        BackHandler { isCustomMode = false }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isCustomMode) {
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "Zegar Szachowy",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            val options = listOf(
                "Bullet" to 3,
                "Blitz" to 5,
                "Rapid" to 10,
                "Klasyczne" to 60
            )

            options.forEach { (label, minutes) ->
                Button(
                    onClick = { onSettingsSelected(ClockSettings(minutes * 60 * 1000L, minutes * 60 * 1000L)) },
                    modifier = Modifier.fillMaxWidth(0.7f).padding(vertical = 4.dp)
                ) {
                    Text(text = "$label: $minutes min")
                }
            }

            Button(
                onClick = { isCustomMode = true },
                modifier = Modifier.fillMaxWidth(0.7f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Custom")
            }

            Spacer(modifier = Modifier.weight(1f))

            // Przycisk "Zapisane gry" zawsze widoczny na dole menu głównego
            OutlinedButton(
                onClick = onOpenSavedGames,
                modifier = Modifier.fillMaxWidth(0.7f).padding(bottom = 16.dp),
            ) {
                Icon(Icons.Default.History, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Zapisane gry")
            }
        } else {
            Text("Ustawienia zaawansowane", fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Zsynchronizuj czasy")
                Checkbox(checked = linkTimes, onCheckedChange = { linkTimes = it })
            }

            Text(if (linkTimes) "Czas graczy" else "Gracz 1 (Dół)", fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TimeSpinBox("Min", min1, { min1 = it; if(linkTimes) min2 = it }, 0..999)
                Text(":", fontSize = 24.sp, modifier = Modifier.padding(horizontal = 4.dp))
                TimeSpinBox("Sek", sec1, { sec1 = it; if(linkTimes) sec2 = it }, 0..59, true)
            }

            if (!linkTimes) {
                Spacer(Modifier.height(16.dp))
                Text("Gracz 2 (Góra)", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimeSpinBox("Min", min2, { min2 = it }, 0..999)
                    Text(":", fontSize = 24.sp, modifier = Modifier.padding(horizontal = 4.dp))
                    TimeSpinBox("Sek", sec2, { sec2 = it }, 0..59, true)
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Inkrementacja", fontWeight = FontWeight.Bold)
                Checkbox(checked = hasIncrement, onCheckedChange = { 
                    hasIncrement = it
                    if (!it) incrementSec = 0
                })
            }
            if (hasIncrement) {
                TimeSpinBox("(sek/ruch)", incrementSec, { incrementSec = it }, 0..60)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row {
                Button(onClick = { isCustomMode = false }, modifier = Modifier.padding(horizontal = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                    Text("Wróć")
                }
                Button(
                    onClick = { 
                        val t1 = (min1 * 60L + sec1) * 1000L
                        val t2 = (min2 * 60L + sec2) * 1000L
                        if (t1 > 0 && t2 > 0) onSettingsSelected(ClockSettings(t1, t2, if(hasIncrement) incrementSec * 1000L else 0))
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
fun TimeSpinBox(label: String, value: Int, onValueChange: (Int) -> Unit, range: IntRange, wrap: Boolean = false) {
    val plusInteractionSource = remember { MutableInteractionSource() }
    val isPlusPressed by plusInteractionSource.collectIsPressedAsState()
    val minusInteractionSource = remember { MutableInteractionSource() }
    val isMinusPressed by minusInteractionSource.collectIsPressedAsState()

    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    LaunchedEffect(isPlusPressed) {
        if (isPlusPressed) {
            delay(400) // Opóźnienie przed rozpoczęciem auto-powtarzania
            var delayTime = 200L
            while (isPlusPressed) {
                var next = currentValue + 1
                if (next > range.last) next = if (wrap) range.first else range.last
                currentOnValueChange(next)
                delay(delayTime)
                delayTime = (delayTime * 0.8).toLong().coerceAtLeast(50L)
            }
        }
    }

    LaunchedEffect(isMinusPressed) {
        if (isMinusPressed) {
            delay(400) // Opóźnienie przed rozpoczęciem auto-powtarzania
            var delayTime = 200L
            while (isMinusPressed) {
                var prev = currentValue - 1
                if (prev < range.first) prev = if (wrap) range.last else range.first
                currentOnValueChange(prev)
                delay(delayTime)
                delayTime = (delayTime * 0.8).toLong().coerceAtLeast(50L)
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
        if (label.isNotEmpty()) Text(label, fontSize = 12.sp)
        IconButton(
            onClick = {
                var next = currentValue + 1
                if (next > range.last) next = if (wrap) range.first else range.last
                currentOnValueChange(next)
            },
            interactionSource = plusInteractionSource
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
        }
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { val n = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0; onValueChange(n.coerceIn(range)) },
            modifier = Modifier.width(70.dp),
            textStyle = LocalTextStyle.current.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        IconButton(
            onClick = {
                var prev = currentValue - 1
                if (prev < range.first) prev = if (wrap) range.last else range.first
                currentOnValueChange(prev)
            },
            interactionSource = minusInteractionSource
        ) {
            Icon(Icons.Default.Remove, contentDescription = null)
        }
    }
}

@Composable
fun ChessClockScreen(
    initialSettings: ClockSettings,
    savedState: GameState? = null,
    onBack: () -> Unit,
    onSaveAndExit: (GameState) -> Unit
) {
    var timeLeft1 by remember { mutableLongStateOf(savedState?.timeLeft1 ?: initialSettings.time1Millis) }
    var timeLeft2 by remember { mutableLongStateOf(savedState?.timeLeft2 ?: initialSettings.time2Millis) }
    var moveCount1 by remember { mutableIntStateOf(savedState?.moveCount1 ?: 0) }
    var moveCount2 by remember { mutableIntStateOf(savedState?.moveCount2 ?: 0) }
    var activePlayer by remember { mutableStateOf<Int?>(savedState?.activePlayer) }
    var isPaused by remember { mutableStateOf(true) }
    var player1Color by remember { mutableStateOf<Color>(savedState?.player1Color ?: Color.Blue) }
    var player2Color by remember { mutableStateOf<Color>(savedState?.player2Color ?: Color.Blue) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var gameName by remember { mutableStateOf(savedState?.name ?: "") }

    BackHandler { showExitDialog = true }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Wyjście z gry") },
            text = { 
                Column {
                    Text("Czy na pewno chcesz wyjść? Możesz zapisać stan gry.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = gameName,
                        onValueChange = { gameName = it },
                        label = { Text("Nazwa zapisu") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveAndExit(
                        GameState(
                            id = savedState?.id ?: java.util.UUID.randomUUID().toString(),
                            name = gameName.ifBlank { "Gra ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}" },
                            timeLeft1 = timeLeft1,
                            timeLeft2 = timeLeft2,
                            moveCount1 = moveCount1,
                            moveCount2 = moveCount2,
                            activePlayer = activePlayer,
                            player1Color = player1Color,
                            player2Color = player2Color,
                            initialSettings = initialSettings
                        )
                    )
                }) { Text("Zapisz i wyjdź") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = onBack) { Text("Wyjdź bez zapisu", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { showExitDialog = false }) { Text("Anuluj") }
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset zegara") },
            text = { Text("Czy na pewno chcesz zresetować zegar?") },
            confirmButton = {
                TextButton(onClick = {
                    timeLeft1 = initialSettings.time1Millis
                    timeLeft2 = initialSettings.time2Millis
                    moveCount1 = 0; moveCount2 = 0
                    activePlayer = null; isPaused = true
                    player1Color = Color.Blue; player2Color = Color.Blue
                    showResetDialog = false
                }) { Text("Tak") }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Anuluj") } }
        )
    }

    LaunchedEffect(activePlayer, isPaused) {
        while (activePlayer != null && !isPaused) {
            delay(100)
            if (activePlayer == 1) {
                timeLeft1 = (timeLeft1 - 100).coerceAtLeast(0)
                if (timeLeft1 == 0L) isPaused = true
            } else {
                timeLeft2 = (timeLeft2 - 100).coerceAtLeast(0)
                if (timeLeft2 == 0L) isPaused = true
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ClockButton(
            timeMillis = timeLeft2, moveCount = moveCount2, backgroundColor = player2Color, isActive = activePlayer == 2,
            onClick = {
                if (activePlayer == null) {
                    player2Color = Color.White; player1Color = Color.Black
                    moveCount2 = 1; activePlayer = 1; isPaused = false
                } else if (activePlayer == 2) {
                    if (isPaused) {
                        isPaused = false
                    } else {
                        timeLeft2 += initialSettings.incrementMillis // Inkrementacja
                        activePlayer = 1; moveCount2++
                    }
                }
            },
            modifier = Modifier.weight(1f).rotate(180f)
        )

        Row(
            modifier = Modifier.fillMaxWidth().background(Color.DarkGray).padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showExitDialog = true }) { Icon(Icons.Default.Home, null, tint = Color.White) }
            IconButton(onClick = { isPaused = !isPaused }, enabled = activePlayer != null) {
                Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, null, tint = if (activePlayer != null) Color.White else Color.Gray)
            }
            IconButton(onClick = { showResetDialog = true }, enabled = player1Color != Color.Blue) {
                Icon(Icons.Default.Refresh, null, tint = if (player1Color != Color.Blue) Color.White else Color.Gray)
            }
        }

        ClockButton(
            timeMillis = timeLeft1, moveCount = moveCount1, backgroundColor = player1Color, isActive = activePlayer == 1,
            onClick = {
                if (activePlayer == null) {
                    player1Color = Color.White; player2Color = Color.Black
                    moveCount1 = 1; activePlayer = 2; isPaused = false
                } else if (activePlayer == 1) {
                    if (isPaused) {
                        isPaused = false
                    } else {
                        timeLeft1 += initialSettings.incrementMillis // Inkrementacja
                        activePlayer = 2; moveCount1++
                    }
                }
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ClockButton(timeMillis: Long, moveCount: Int, backgroundColor: Color, isActive: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val contentColor = if (backgroundColor == Color.White) Color.Black else Color.White
    Surface(onClick = onClick, color = backgroundColor, modifier = modifier.fillMaxWidth().then(if (isActive) Modifier.border(8.dp, Color.Red) else Modifier)) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Ruchy: $moveCount", fontSize = 20.sp, color = contentColor, modifier = Modifier.align(Alignment.TopEnd))
            Text(formatTime(timeMillis), fontSize = 72.sp, color = contentColor, modifier = Modifier.align(Alignment.Center))
        }
    }
}

fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

fun GameState.toJson(): JSONObject {
    val json = JSONObject()
    json.put("id", id)
    json.put("name", name)
    json.put("timeLeft1", timeLeft1)
    json.put("timeLeft2", timeLeft2)
    json.put("moveCount1", moveCount1)
    json.put("moveCount2", moveCount2)
    json.put("activePlayer", activePlayer ?: -1)
    json.put("player1Color", player1Color.toArgb())
    json.put("player2Color", player2Color.toArgb())
    json.put("timestamp", timestamp)
    
    val settingsJson = JSONObject()
    settingsJson.put("t1", initialSettings.time1Millis)
    settingsJson.put("t2", initialSettings.time2Millis)
    settingsJson.put("inc", initialSettings.incrementMillis)
    json.put("settings", settingsJson)
    
    return json
}

fun JSONObject.toGameState(): GameState {
    val settingsJson = getJSONObject("settings")
    val settings = ClockSettings(
        settingsJson.getLong("t1"),
        settingsJson.getLong("t2"),
        settingsJson.getLong("inc")
    )
    val activePlayerVal = getInt("activePlayer")
    return GameState(
        id = getString("id"),
        name = getString("name"),
        timeLeft1 = getLong("timeLeft1"),
        timeLeft2 = getLong("timeLeft2"),
        moveCount1 = getInt("moveCount1"),
        moveCount2 = getInt("moveCount2"),
        activePlayer = if (activePlayerVal == -1) null else activePlayerVal,
        player1Color = Color(getInt("player1Color")),
        player2Color = Color(getInt("player2Color")),
        initialSettings = settings,
        timestamp = getLong("timestamp")
    )
}

fun saveGame(context: android.content.Context, state: GameState) {
    val prefs = context.getSharedPreferences("chess_clock_prefs", android.content.Context.MODE_PRIVATE)
    val games = loadAllGames(context).toMutableList()
    
    // Update existing or add new
    val index = games.indexOfFirst { it.id == state.id }
    if (index != -1) {
        games[index] = state
    } else {
        games.add(0, state)
    }
    
    val jsonArray = JSONArray()
    games.forEach { jsonArray.put(it.toJson()) }
    prefs.edit().putString("saved_games_list", jsonArray.toString()).apply()
}

fun loadAllGames(context: android.content.Context): List<GameState> {
    val prefs = context.getSharedPreferences("chess_clock_prefs", android.content.Context.MODE_PRIVATE)
    val jsonString = prefs.getString("saved_games_list", null) ?: return emptyList()
    val games = mutableListOf<GameState>()
    try {
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            games.add(jsonArray.getJSONObject(i).toGameState())
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return games.sortedByDescending { it.timestamp }
}

fun deleteGame(context: android.content.Context, gameId: String) {
    val prefs = context.getSharedPreferences("chess_clock_prefs", android.content.Context.MODE_PRIVATE)
    val games = loadAllGames(context).filter { it.id != gameId }
    val jsonArray = JSONArray()
    games.forEach { jsonArray.put(it.toJson()) }
    prefs.edit().putString("saved_games_list", jsonArray.toString()).apply()
}

@Composable
fun SavedGamesMenu(onGameSelected: (GameState) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var games by remember { mutableStateOf(loadAllGames(context)) }

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Zapisane gry", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        if (games.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Brak zapisanych gier", color = Color.Gray)
            }
        } else {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                games.forEach { game ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = { onGameSelected(game) }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(game.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(
                                    "${formatTime(game.timeLeft1)} / ${formatTime(game.timeLeft2)}",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(game.timestamp)),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            IconButton(onClick = {
                                deleteGame(context, game.id)
                                games = loadAllGames(context)
                            }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
