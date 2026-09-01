package com.quittrack

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.font.FontFamily

data class LogEntry(
    val type: String,
    val time: Long,
    val intensity: Int = 0,
    val source: String = "",
    val context: String = "",
    val morning: Boolean = false
)

data class DailyReview(
    val date: String,
    val entries: List<LogEntry>
)

private val QuitGreen = androidx.compose.ui.graphics.Color(0xFF850707)
private val QuitGreenDark = androidx.compose.ui.graphics.Color(0xFF850707)
private val QuitGreenLight = androidx.compose.ui.graphics.Color(0xFF121111)
private val QuitOrange = androidx.compose.ui.graphics.Color(0xFF850707)
private val QuitOrangeLight = androidx.compose.ui.graphics.Color(0xFF121111)
private val WarmBackground = androidx.compose.ui.graphics.Color(0xFF000000)
private val CardWhite = androidx.compose.ui.graphics.Color(0xFF121111)
private val TextDark = androidx.compose.ui.graphics.Color(0xFFE6E7E8)
private val TextMuted = androidx.compose.ui.graphics.Color(0xFFE6E7E8)
private val BorderLight = androidx.compose.ui.graphics.Color(0xFF121111)
private val SystemFontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif

class MainActivity : ComponentActivity() {

    private val prefs by lazy {
        getSharedPreferences("quit_track", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            QuitTrackApp(
                loadStartDate = { prefs.getLong("startDate", 0L) },
                saveStartDate = { prefs.edit().putLong("startDate", it).apply() },
                loadEntries = { loadEntries() },
                saveEntries = { saveEntries(it) },
                loadDailyReviews = { loadDailyReviews() },
                saveDailyReview = { saveDailyReview(it) },
                loadFontSize = { prefs.getString("fontSize", "Medium") ?: "Medium" },
                saveFontSize = { prefs.edit().putString("fontSize", it).apply() }
            )
        }
    }

   private fun loadEntries(): List<LogEntry> {
    val a = JSONArray(prefs.getString("entries", "[]") ?: "[]")

    return buildList {
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)

            add(
                LogEntry(
                    o.getString("type"),
                    o.getLong("time"),
                    o.optInt("intensity"),
                    o.optString("source"),
                    o.optString("context"),
                    o.optBoolean("morning", false)
                )
            )
        }
    }
}

    private fun saveEntries(entries: List<LogEntry>) {
    val a = JSONArray()

    entries.forEach { e ->
        a.put(
            JSONObject().apply {
                put("type", e.type)
                put("time", e.time)
                put("intensity", e.intensity)
                put("source", e.source)
                put("context", e.context)
                put("morning", e.morning)
            }
        )
    }

    prefs.edit().putString("entries", a.toString()).apply()
}

    private fun saveDailyReview(review: DailyReview) {
    val reviews = JSONArray(
        prefs.getString("dailyReviews", "[]") ?: "[]"
    )

    var replaced = false

    for (i in 0 until reviews.length()) {
        val existing = reviews.getJSONObject(i)

        if (existing.optString("date") == review.date) {
            reviews.put(i, reviewToJson(review))
            replaced = true
            break
        }
    }

    if (!replaced) {
        reviews.put(reviewToJson(review))
    }

    prefs.edit()
        .putString("dailyReviews", reviews.toString())
        .apply()
}

    private fun loadDailyReviews(): List<DailyReview> {
    val reviews = JSONArray(
        prefs.getString("dailyReviews", "[]") ?: "[]"
    )

    return buildList {
        for (i in 0 until reviews.length()) {
            val root = reviews.getJSONObject(i)
            val array = root.optJSONArray("entries") ?: JSONArray()

            val reviewEntries = buildList {
                for (j in 0 until array.length()) {
                    val o = array.getJSONObject(j)

                    add(
                        LogEntry(
                            type = o.getString("type"),
                            time = o.getLong("time"),
                            intensity = o.optInt("intensity"),
                            source = o.optString("source"),
                            context = o.optString("context"),
                            morning = o.optBoolean("morning", false)
                        )
                    )
                }
            }

            add(
                DailyReview(
                    date = root.optString("date"),
                    entries = reviewEntries
                )
            )
        }
    }
}

private fun reviewToJson(review: DailyReview): JSONObject {
    val root = JSONObject()

    root.put("date", review.date)

    val array = JSONArray()

    review.entries.forEach { e ->
        array.put(
            JSONObject().apply {
                put("type", e.type)
                put("time", e.time)
                put("intensity", e.intensity)
                put("source", e.source)
                put("context", e.context)
                put("morning", e.morning)
            }
        )
    }

    root.put("entries", array)

    return root
}

@Composable
fun QuitTrackTheme(
    fontSize: String,
    content: @Composable () -> Unit
) {
    val colors = darkColorScheme(
    primary = QuitGreen,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = QuitOrange,
    background = WarmBackground,
    surface = CardWhite,
    onBackground = TextDark,
    onSurface = TextDark
)

    val multiplier = when (fontSize) {
        "Small" -> 0.9f
        "Large" -> 1.12f
        else -> 1f
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(
            defaultFontFamily = SystemFontFamily,
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(
                fontSize = (16 * multiplier).sp
            ),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(
                fontSize = (14 * multiplier).sp
            ),
            titleLarge = MaterialTheme.typography.titleLarge.copy(
                fontSize = (22 * multiplier).sp
            ),
            headlineSmall = MaterialTheme.typography.headlineSmall.copy(
                fontSize = (28 * multiplier).sp
            ),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(
                fontSize = (32 * multiplier).sp
            )
        ),
        content = content
    )
}

@Composable
fun QuitTrackApp(
    loadStartDate: () -> Long,
    saveStartDate: (Long) -> Unit,
    loadEntries: () -> List<LogEntry>,
    saveEntries: (List<LogEntry>) -> Unit,
    loadDailyReviews: () -> List<DailyReview>,
    loadFontSize: () -> String,
    saveFontSize: (String) -> Unit,
    saveDailyReview: (DailyReview) -> Unit
) {
    var startDate by remember { mutableLongStateOf(loadStartDate()) }
    var entries by remember { mutableStateOf(loadEntries()) }
    var dailyReviews by remember { mutableStateOf(loadDailyReviews()) }
    var screen by remember { mutableStateOf("Today") }
    var smokeDialog by remember { mutableStateOf(false) }
    var cravingDialog by remember { mutableStateOf(false) }
    var reviewSaved by remember { mutableStateOf(false) }

    var fontSize by remember { mutableStateOf(loadFontSize()) }

    val midnight = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    if (startDate == 0L) {
        LaunchedEffect(Unit) {
            startDate = midnight
            saveStartDate(midnight)
        }
    }

    val day = (((midnight - startDate) / 86_400_000L).toInt() + 1)
        .coerceIn(1, 40)

    val today = entries.filter { sameDay(it.time, midnight) }
    val smoked = today.count { it.type == "SMOKED" }
    val morning = today.count {
        it.type == "SMOKED" && it.morning
    }
    val cravings = today.count { it.type == "CRAVING" }

    QuitTrackTheme(
        fontSize = fontSize
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    listOf(
                        "Today" to "⌂",
                        "Plan" to "☷",
                        "Stats" to "▥",
                        "Settings" to "⚙"
                    ).forEach { (name, icon) ->

                        NavigationBarItem(
                            selected = screen == name,
                            onClick = { screen = name },
                            icon = {
                                Text(
                                    icon,
                                    fontSize = 22.sp
                                )
                            },
                            label = {
                                Text(name)
                            }
                        )
                    }
                }
            }
        ) { pad ->

                        when (screen) {
                "Today" -> TodayScreen(
                    Modifier.padding(pad),
                    day,
                    smoked,
                    morning,
                    cravings,
                    onSmoke = { smokeDialog = true },
                    onCraving = { cravingDialog = true },
                    onEntries = { screen = "Entries" },
                    onPlan = { screen = "Plan" },
                    onEmergency = { screen = "Emergency" },
                    onSaveReview = {
    val todayEntries = entries.filter {
        sameDay(it.time, midnight)
    }

    val review = DailyReview(
        date = fmtDate(midnight),
        entries = todayEntries
    )

    saveDailyReview(review)

    dailyReviews = dailyReviews
        .filterNot { it.date == review.date } + review

    reviewSaved = true
},
    reviewSaved = reviewSaved
                )

                "Plan" -> PlanScreen(
                    Modifier.padding(pad),
                    day
                )

                "Stats" -> StatsScreen(
                    Modifier.padding(pad),
                    entries,
                    onDailyReviews = { screen = "DailyReviews" }
                )

                "DailyReviews" -> DailyReviewsScreen(
    Modifier.padding(pad),
    reviews = dailyReviews,
    onBack = { screen = "Stats" }
)


                "Settings" -> SettingsScreen(
                    Modifier.padding(pad),
                    startDate = startDate,
                    entries = entries,
                    fontSize = fontSize,
                    onFontSizeChange = {
                        fontSize = it
                        saveFontSize(it)
                    },
                    onReset = {
                        val now = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis

                        startDate = now
                        entries = emptyList()

                        saveStartDate(now)
                        saveEntries(entries)
                    },
                    onImport = { importedStartDate, importedEntries ->
                        startDate = importedStartDate
                        entries = importedEntries
                        saveStartDate(importedStartDate)
                        saveEntries(importedEntries)
                    }
                )

                "Entries" -> EntriesScreen(
                    Modifier.padding(pad),
                    entries,
                    onBack = { screen = "Today" }
                )

                "Emergency" -> EmergencyScreen(
                    Modifier.padding(pad),
                    onBack = { screen = "Today" }
                )
            }
        }

        if (smokeDialog) {
            SmokeDialog(
                dismiss = { smokeDialog = false }
            ) { source, context, intensity, morning ->

                entries = entries + LogEntry(
                    "SMOKED",
                    System.currentTimeMillis(),
                    intensity,
                    source,
                    context,
                    morning
                )

                saveEntries(entries)
                smokeDialog = false
            }
        }

        if (cravingDialog) {
            CravingDialog(
                dismiss = { cravingDialog = false }
            ) { intensity, context, morning ->

                entries = entries + LogEntry(
                    "CRAVING",
                    System.currentTimeMillis(),
                    intensity,
                    context = context,
                    morning = morning
                )

                saveEntries(entries)
                cravingDialog = false
            }
        }
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        ),
        content = content
    )
}

@Composable
fun TodayScreen(
    m: Modifier,
    day: Int,
    smoked: Int,
    morning: Int,
    cravings: Int,
    onSmoke: () -> Unit,
    onCraving: () -> Unit,
    onEntries: () -> Unit,
    onPlan: () -> Unit,
    onEmergency: () -> Unit,
    onSaveReview: () -> Unit,
    reviewSaved: Boolean
) {
    LazyColumn(
        modifier = m.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 24.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🌿  Quit Track",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = QuitGreen
                )

                Spacer(Modifier.weight(1f))

                Text(
                    "♧",
                    fontSize = 25.sp,
                    color = QuitGreen
                )
            }
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Good morning! 👋",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "You've got this. One day at a time.",
                    color = TextMuted
                )
            }
        }

        item {
            AppCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
) {
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Day $day of 40",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                "Phase ${phaseForDay(day)} • ${phaseName(day)}",
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }

    Text(
        "▣",
        fontSize = 28.sp,
        color = QuitGreen
    )
}

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "CIGARETTES TODAY",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )

                            Text(
                                smoked.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "cigarettes",
                                color = TextMuted
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                "MORNING",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )

                            Text(
                                morning.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "cigarettes",
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onSaveReview,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = QuitGreen
                )
            ) {
                Text(
    if (reviewSaved)
        "✓  Today's review saved"
    else
        "✓  Save today's review",
    fontWeight = FontWeight.Bold
)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onSmoke,
                    modifier = Modifier
                        .weight(1f)
                        .height(74.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = QuitGreen
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "＋",
                            fontSize = 24.sp
                        )
                        Text("Log a cigarette")
                    }
                }

                Button(
                    onClick = onCraving,
                    modifier = Modifier
                        .weight(1f)
                        .height(74.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = QuitOrange
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "🔥",
                            fontSize = 22.sp
                        )
                        Text("I have a craving")
                    }
                }
            }
        }

        item {
            ActionCard(
                title = "Today's plan",
                subtitle = "Focus on today's targets",
                icon = "🎯",
                onClick = onPlan
            )
        }

        item {
            ActionCard(
                title = "Emergency craving help",
                subtitle = "Tools to get through strong urges",
                icon = "🛟",
                onClick = onEmergency
            )
        }

        item {
            ActionCard(
                title = "View all entries",
                subtitle = "$cravings cravings • $smoked cigarettes today",
                icon = "📋",
                onClick = onEntries
            )
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    icon: String,
    onClick: () -> Unit
) {
    AppCard(
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                icon,
                fontSize = 28.sp
            )

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    subtitle,
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                "›",
                fontSize = 28.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
fun PlanScreen(
    m: Modifier,
    currentDay: Int
) {
    val days = (1..40).toList()

    LazyColumn(
        modifier = m.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 24.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        item {
            Text(
                "Plan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = QuitGreen
            )
        }

        item {
            Text(
                "Your 40-Day Plan",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Your journey toward a smoke-free life.",
                color = TextMuted
            )
        }

        itemsIndexed(days) { index, day ->

            val completed = day < currentDay
            val current = day == currentDay

            AppCard {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    completed -> QuitGreen
                                    current -> QuitGreenLight
                                    else -> BorderLight
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (completed) "✓" else day.toString(),
                            fontWeight = FontWeight.Bold,
                            color = if (completed) androidx.compose.ui.graphics.Color.White
                            else QuitGreen
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Day $day",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            planTitle(day),
                            fontWeight = if (current)
                                FontWeight.Bold
                            else
                                FontWeight.Normal
                        )

                        Text(
                            planSubtitle(day),
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Text(
                        "›",
                        fontSize = 28.sp,
                        color = TextMuted
                    )
                }
            }
        }

        item {
            AppCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "🌱  Every day is a step forward.",
                        fontWeight = FontWeight.Bold,
                        color = QuitGreen
                    )

                    Text(
                        "Small steps. Big change.",
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun StatsScreen(
    m: Modifier,
    entries: List<LogEntry>,
    onDailyReviews: () -> Unit
) {
    val smoked = entries.filter { it.type == "SMOKED" }
    val cravings = entries.filter { it.type == "CRAVING" }

    val averageCraving =
        if (cravings.isEmpty()) 0.0
        else cravings.map { it.intensity }.average()

    val highestCraving =
        cravings.maxOfOrNull { it.intensity } ?: 0

    LazyColumn(
        modifier = m.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 24.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {
            Text(
                "Stats",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = QuitGreen
            )
        }

        item {
            AppCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Overview",
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "Your progress so far",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = QuitGreenLight
                        ) {
                            Text(
                                "All time",
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 8.dp
                                ),
                                color = QuitGreen
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatBox(
                            modifier = Modifier.weight(1f),
                            title = "Cigarettes logged",
                            value = smoked.size.toString()
                        )

                        StatBox(
                            modifier = Modifier.weight(1f),
                            title = "Cravings logged",
                            value = cravings.size.toString()
                        )
                    }
                }
            }
        }

        item {
            AppCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Craving insights",
                        fontWeight = FontWeight.Bold
                    )

                    StatRow(
                        "Average craving",
                        "${"%.1f".format(averageCraving)}/10"
                    )

                    StatRow(
                        "Highest craving",
                        "$highestCraving/10"
                    )
                }
            }
        }

        item {
            AppCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "How cigarettes were obtained",
                        fontWeight = FontWeight.Bold
                    )

                    StatRow(
                        "Bought",
                        smoked.count { it.source == "Bought" }.toString()
                    )

                    StatRow(
                        "Offered",
                        smoked.count { it.source == "Offered" }.toString()
                    )

                    StatRow(
                        "Asked for",
                        smoked.count { it.source == "Asked for" }.toString()
                    )

                    StatRow(
                        "Other",
                        smoked.count { it.source == "Other" }.toString()
                    )
                }
            }
        }

        item {
            AppCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Progress",
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        if (smoked.isEmpty())
                            "Your tracking journey starts here."
                        else
                            "Keep tracking. Patterns become clearer over time.",
                        color = TextMuted
                    )
                }
            }
        }

        item {
            ActionCard(
                title = "Daily reviews",
                subtitle = "View your saved daily reviews",
                icon = "📖",
                onClick = onDailyReviews
            )
        }
    }
}


@Composable
fun StatBox(
    modifier: Modifier,
    title: String,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = QuitGreen
            )
        }
    }
}

@Composable
fun StatRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(
            value,
            fontWeight = FontWeight.Bold,
            color = QuitGreen
        )
    }
}

@Composable
fun SettingsScreen(
    m: Modifier,
    startDate: Long,
    entries: List<LogEntry>,
    fontSize: String,
    onFontSizeChange: (String) -> Unit,
    onReset: () -> Unit,
    onImport: (Long, List<LogEntry>) -> Unit
) {
    var showReset by remember { mutableStateOf(false) }
    var showFontSize by remember { mutableStateOf(false) }
    var showObjective by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val prefs = context.getSharedPreferences(
        "quit_track",
        android.content.Context.MODE_PRIVATE
    )

    var objective by remember {
        mutableStateOf(
            prefs.getString("objective", "Reduce gradually")
                ?: "Reduce gradually"
        )
    }

    var cigarettesPerDay by remember {
        mutableStateOf(
            prefs.getInt("cigarettesPerDay", 8).toString()
        )
    }

    var morningCigarettes by remember {
        mutableStateOf(
            prefs.getInt("morningCigarettes", 3).toString()
        )
    }

    var savedMessage by remember { mutableStateOf(false) }

    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri != null) {
                val root = JSONObject()

                root.put("startDate", startDate)

                val array = JSONArray()

                entries.forEach { e ->
                    array.put(
                        JSONObject().apply {
                            put("type", e.type)
                            put("time", e.time)
                            put("intensity", e.intensity)
                            put("source", e.source)
                            put("context", e.context)
                        }
                    )
                }

                root.put("entries", array)

                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(root.toString(2).toByteArray())
                }
            }
        }

    val importLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                try {
                    val json = context.contentResolver
                        .openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }

                    if (json != null) {
                        val root = JSONObject(json)

                        val importedStart =
                            root.optLong("startDate", startDate)

                        val array =
                            root.optJSONArray("entries") ?: JSONArray()

                        val importedEntries =
                            buildList {
                                for (i in 0 until array.length()) {
                                    val o = array.getJSONObject(i)

                                    add(
                                        LogEntry(
                                            o.getString("type"),
                                            o.getLong("time"),
                                            o.optInt("intensity"),
                                            o.optString("source"),
                                            o.optString("context")
                                        )
                                    )
                                }
                            }

                        onImport(
                            importedStart,
                            importedEntries
                        )
                    }
                } catch (_: Exception) {
                    // Invalid import files are ignored.
                }
            }
        }

    LazyColumn(
        modifier = m.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 24.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = QuitGreen
            )
        }

        item {
            SettingsSectionTitle("GOALS & TARGETS")

            AppCard {
                Column {

                    SettingsRow(
                        icon = "🎯",
                        title = "Objective",
                        subtitle = objective,
                        onClick = { showObjective = true }
                    )

                    Divider()

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Daily cigarette limit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            "Maximum cigarettes permitted per day",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )

                        OutlinedTextField(
                            value = cigarettesPerDay,
                            onValueChange = {
                                if (it.length <= 3 && it.all { c -> c.isDigit() }) {
                                    cigarettesPerDay = it
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Cigarettes per day") }
                        )

                        OutlinedTextField(
                            value = morningCigarettes,
                            onValueChange = {
                                if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                                    morningCigarettes = it
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Morning cigarettes") }
                        )

                        Button(
                            onClick = {
                                val daily = cigarettesPerDay.toIntOrNull()
                                    ?.coerceIn(0, 100) ?: 8

                                val morning = morningCigarettes.toIntOrNull()
                                    ?.coerceIn(0, 50) ?: 3

                                cigarettesPerDay = daily.toString()
                                morningCigarettes = morning.toString()

                                prefs.edit()
                                    .putString("objective", objective)
                                    .putInt("cigarettesPerDay", daily)
                                    .putInt("morningCigarettes", morning)
                                    .apply()

                                savedMessage = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save targets")
                        }

                        if (savedMessage) {
                            Text(
                                "Targets saved",
                                color = QuitGreen,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsSectionTitle("APPEARANCE")

            AppCard {
                Column {
                    SettingsRow(
                        icon = "Aa",
                        title = "Font size",
                        subtitle = fontSize,
                        onClick = { showFontSize = true }
                    )
                }
            }
        }

        item {
            SettingsSectionTitle("DATA")

            AppCard {
                Column {
                    SettingsRow(
                        icon = "⇩",
                        title = "Export my data",
                        subtitle = "Save a copy of your data",
                        onClick = {
                            exportLauncher.launch("quit-track-backup.json")
                        }
                    )

                    Divider()

                    SettingsRow(
                        icon = "⇧",
                        title = "Import my data",
                        subtitle = "Restore data from a file",
                        onClick = {
                            importLauncher.launch(
                                arrayOf("application/json")
                            )
                        }
                    )

                    Divider()

                    SettingsRow(
                        icon = "⌫",
                        title = "Delete all my data",
                        subtitle = "Permanently delete your local data",
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { showReset = true }
                    )
                }
            }
        }

        item {
            SettingsSectionTitle("ABOUT")

            AppCard {
                Column {
                    SettingsRow(
                        icon = "ⓘ",
                        title = "About Quit Track",
                        subtitle = "Learn more about the app",
                        onClick = {}
                    )

                    Divider()

                    SettingsRow(
                        icon = "✓",
                        title = "Privacy",
                        subtitle = "Your tracking data stays on this device",
                        onClick = {}
                    )

                    Divider()

                    SettingsRow(
                        icon = "#",
                        title = "Version",
                        subtitle = "0.2.0",
                        onClick = {}
                    )
                }
            }
        }

        item {
            Text(
                "Start date: ${fmtDate(startDate)}",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (showObjective) {
        AlertDialog(
            onDismissRequest = {
                showObjective = false
            },
            title = {
                Text("Objective")
            },
            text = {
                Column {
                    listOf(
                        "Quit completely",
                        "Reduce gradually",
                        "Quit by a specific date"
                    ).forEach { option ->

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    objective = option
                                    showObjective = false
                                    savedMessage = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = objective == option,
                                onClick = {
                                    objective = option
                                    showObjective = false
                                    savedMessage = false
                                }
                            )

                            Spacer(Modifier.width(8.dp))

                            Text(option)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showFontSize) {
        AlertDialog(
            onDismissRequest = {
                showFontSize = false
            },
            title = {
                Text("Font size")
            },
            text = {
                Column {
                    listOf("Small", "Medium", "Large").forEach { size ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onFontSizeChange(size)
                                    showFontSize = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = fontSize == size,
                                onClick = {
                                    onFontSizeChange(size)
                                    showFontSize = false
                                }
                            )

                            Spacer(Modifier.width(8.dp))

                            Text(size)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }


    if (showReset) {
        AlertDialog(
            onDismissRequest = {
                showReset = false
            },
            title = {
                Text("Delete everything?")
            },
            text = {
                Text(
                    "This permanently deletes locally stored entries and restarts the plan at Day 1."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReset()
                        showReset = false
                    }
                ) {
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showReset = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionTitle(
    text: String
) {
    Text(
        text,
        modifier = Modifier.padding(
            start = 4.dp,
            bottom = 4.dp
        ),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = QuitGreen
    )
}

@Composable
fun SettingsRow(
    icon: String,
    title: String,
    subtitle: String,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            icon,
            modifier = Modifier.width(36.dp),
            fontSize = 21.sp,
            color = if (titleColor == MaterialTheme.colorScheme.error)
                titleColor
            else
                QuitGreen
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )

            Text(
                subtitle,
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text(
            "›",
            fontSize = 26.sp,
            color = TextMuted
        )
    }
}

@Composable
fun EntriesScreen(
    m: Modifier,
    entries: List<LogEntry>,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = m.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 24.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        item {
            TextButton(onClick = onBack) {
                Text("‹ Back")
            }
        }

        item {
            Text(
                "All entries",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (entries.isEmpty()) {
            item {
                AppCard {
                    Text(
                        "No entries yet.",
                        modifier = Modifier.padding(20.dp),
                        color = TextMuted
                    )
                }
            }
        }

        items(
            entries.sortedByDescending { it.time }
        ) { e ->

            AppCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        if (e.type == "SMOKED")
                            "🚬  Smoked"
                        else
                            "🔥  Craving",
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        fmtDateTime(e.time),
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (e.source.isNotBlank()) {
                        Text("Source: ${e.source}")
                    }

                    if (e.intensity > 0) {
                        Text("Intensity: ${e.intensity}/10")
                    }

                    if (e.context.isNotBlank()) {
                        Text("Context: ${e.context}")
                    }
                }
            }
        }
    }
}

@Composable
fun EmergencyScreen(
    m: Modifier,
    onBack: () -> Unit
) {
    var seconds by remember {
        mutableIntStateOf(600)
    }

    var running by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(running) {
        while (running && seconds > 0) {
            kotlinx.coroutines.delay(1000)
            seconds--
        }

        if (seconds == 0) {
            running = false
        }
    }

    Column(
        modifier = m
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = onBack) {
                Text("‹ Back")
            }
        }

        Text(
            "🔥",
            fontSize = 48.sp
        )

        Text(
            "Emergency craving help",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            "Delay the decision. Move away from cigarettes. Drink water. Distract yourself. Reassess.",
            textAlign = TextAlign.Center,
            color = TextMuted
        )

        Text(
            "%02d:%02d".format(
                seconds / 60,
                seconds % 60
            ),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = QuitGreen
        )

        Button(
            onClick = {
                running = !running
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                if (running)
                    "Pause"
                else
                    "Start 10-minute timer"
            )
        }

        AppCard {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "While you wait",
                    fontWeight = FontWeight.Bold
                )

                Text("• Move somewhere cigarettes aren't available.")
                Text("• Drink a glass of water.")
                Text("• Take a few slow breaths.")
                Text("• Distract yourself for a few minutes.")
                Text("• Reassess the craving when the timer ends.")
            }
        }

        Text(
            "After the timer: Lower / Same / Higher",
            color = TextMuted
        )
    }
}

@Composable
fun SmokeDialog(
    dismiss: () -> Unit,
    save: (String, String, Int, Boolean) -> Unit
) {
    var source by remember { mutableStateOf("Bought") }
    var context by remember { mutableStateOf("") }
    var intensity by remember { mutableIntStateOf(0) }
    var morning by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("I smoked") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("How did you get it?")

                listOf("Bought", "Offered", "Asked for", "Other").forEach {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = source == it,
                            onClick = { source = it }
                        )
                        Text(it)
                    }
                }

                HorizontalDivider()

                Text("Was this a morning cigarette?")

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = morning,
                        onClick = { morning = true }
                    )
                    Text("Yes")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !morning,
                        onClick = { morning = false }
                    )
                    Text("No")
                }

                OutlinedTextField(
                    value = context,
                    onValueChange = { context = it },
                    label = { Text("Context (optional)") }
                )

                Text(
                    "Craving: ${
                        if (intensity == 0) "Not rated"
                        else "$intensity/10"
                    }"
                )

                Slider(
                    value = intensity.toFloat(),
                    onValueChange = { intensity = it.toInt() },
                    valueRange = 0f..10f,
                    steps = 9
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    save(source, context, intensity, morning)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = dismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CravingDialog(
    dismiss: () -> Unit,
    save: (Int, String, Boolean) -> Unit
) {
    var intensity by remember { mutableIntStateOf(5) }
    var context by remember { mutableStateOf("") }
    var morning by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("I have a craving") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Intensity: $intensity/10")

                Slider(
                    value = intensity.toFloat(),
                    onValueChange = { intensity = it.toInt() },
                    valueRange = 1f..10f,
                    steps = 8
                )

                Text("Is this a morning craving?")

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = morning,
                        onClick = { morning = true }
                    )
                    Text("Yes")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !morning,
                        onClick = { morning = false }
                    )
                    Text("No")
                }

                OutlinedTextField(
                    value = context,
                    onValueChange = { context = it },
                    label = { Text("Situation/context") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    save(intensity, context, morning)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = dismiss) {
                Text("Cancel")
            }
        }
    )
}

fun phaseForDay(day: Int): Int {
    return when {
        day <= 3 -> 1
        day <= 6 -> 2
        day <= 9 -> 3
        day <= 12 -> 4
        day <= 15 -> 5
        day <= 18 -> 6
        day <= 21 -> 7
        day <= 24 -> 8
        day <= 27 -> 9
        day <= 30 -> 10
        day <= 33 -> 11
        day <= 36 -> 12
        day == 37 -> 13
        else -> 14
    }
}

fun phaseName(day: Int): String {
    return when {
        day == 1 -> "Foundation"
        day == 2 -> "Preparation"
        day == 3 -> "Strengthening"
        day in 38..40 -> "Smoke-free maintenance"
        day == 37 -> "Quit Day"
        else -> "Building skills"
    }
}

fun planTitle(day: Int): String {
    return when {
        day == 1 -> "Foundation"
        day == 2 -> "Preparation"
        day == 3 -> "Strengthening"
        day == 37 -> "Quit Day 🎯"
        day in 38..40 -> "Smoke-free maintenance"
        else -> "Building skills"
    }
}

fun planSubtitle(day: Int): String {
    return when {
        day == 1 -> "Build awareness and track your habits."
        day == 2 -> "Understand your triggers and routines."
        day == 3 -> "Build skills to handle cravings."
        day == 37 -> "This is your quit day. You've got this!"
        day in 38..40 -> "Stay strong and keep the momentum."
        else -> "Continue working toward your quit day."
    }
}

fun sameDay(
    a: Long,
    b: Long
): Boolean {
    val x = Calendar.getInstance().apply {
        timeInMillis = a
    }

    val y = Calendar.getInstance().apply {
        timeInMillis = b
    }

    return x.get(Calendar.YEAR) == y.get(Calendar.YEAR) &&
            x.get(Calendar.DAY_OF_YEAR) == y.get(Calendar.DAY_OF_YEAR)
}

fun hour(t: Long): Int {
    return Calendar.getInstance()
        .apply {
            timeInMillis = t
        }
        .get(Calendar.HOUR_OF_DAY)
}

fun fmtDate(t: Long): String {
    return SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.getDefault()
    ).format(Date(t))
}

fun fmtDateTime(t: Long): String {
    return SimpleDateFormat(
        "yyyy-MM-dd HH:mm",
        Locale.getDefault()
    ).format(Date(t))
}
@Composable
fun DailyReviewsScreen(
    m: Modifier,
    reviews: List<DailyReview>,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = m.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 24.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {
            TextButton(onClick = onBack) {
                Text("‹ Back")
            }
        }

        item {
            Text(
                "Daily reviews",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = QuitGreen
            )

            Text(
                "Your saved daily summaries.",
                color = TextMuted
            )
        }

        if (reviews.isEmpty()) {
    item {
        AppCard {
            Text(
                "No saved reviews yet.",
                modifier = Modifier.padding(20.dp),
                color = TextMuted
            )
        }
    }
} else {
    items(
        reviews.sortedByDescending { it.date }
    ) { review ->

        val smoked = review.entries.count {
            it.type == "SMOKED"
        }

        val cravings = review.entries.count {
            it.type == "CRAVING"
        }

        val morning = review.entries.count {
            it.type == "SMOKED" && it.morning
        }

        AppCard {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    review.date,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                Text("Cigarettes: $smoked")
                Text("Morning cigarettes: $morning")
                Text("Cravings: $cravings")
            }
        }
    }
}
    }
}
}
