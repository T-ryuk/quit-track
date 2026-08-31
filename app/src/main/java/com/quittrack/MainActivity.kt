package com.quittrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class LogEntry(
    val type: String,
    val time: Long,
    val intensity: Int = 0,
    val source: String = "",
    val context: String = ""
)

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("quit_track", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                QuitTrackApp(
                    loadStartDate = { prefs.getLong("startDate", 0L) },
                    saveStartDate = { prefs.edit().putLong("startDate", it).apply() },
                    loadEntries = { loadEntries() },
                    saveEntries = { saveEntries(it) }
                )
            }
        }
    }

    private fun loadEntries(): List<LogEntry> {
        val a = JSONArray(prefs.getString("entries", "[]") ?: "[]")
        return buildList {
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                add(LogEntry(o.getString("type"), o.getLong("time"), o.optInt("intensity"),
                    o.optString("source"), o.optString("context")))
            }
        }
    }

    private fun saveEntries(entries: List<LogEntry>) {
        val a = JSONArray()
        entries.forEach { e ->
            a.put(JSONObject().apply {
                put("type", e.type); put("time", e.time); put("intensity", e.intensity)
                put("source", e.source); put("context", e.context)
            })
        }
        prefs.edit().putString("entries", a.toString()).apply()
    }
}

@Composable
fun QuitTrackApp(
    loadStartDate: () -> Long,
    saveStartDate: (Long) -> Unit,
    loadEntries: () -> List<LogEntry>,
    saveEntries: (List<LogEntry>) -> Unit
) {
    var startDate by remember { mutableLongStateOf(loadStartDate()) }
    var entries by remember { mutableStateOf(loadEntries()) }
    var screen by remember { mutableStateOf("Today") }
    var smokeDialog by remember { mutableStateOf(false) }
    var cravingDialog by remember { mutableStateOf(false) }

    val midnight = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    if (startDate == 0L) {
        LaunchedEffect(Unit) { startDate = midnight; saveStartDate(midnight) }
    }
    val day = (((midnight - startDate) / 86_400_000L).toInt() + 1).coerceIn(1, 40)
    val today = entries.filter { sameDay(it.time, midnight) }
    val smoked = today.count { it.type == "SMOKED" }
    val morning = today.count { it.type == "SMOKED" && hour(it.time) < 10 }
    val cravings = today.count { it.type == "CRAVING" }

    Scaffold(bottomBar = {
        NavigationBar {
            listOf("Today","Plan","Stats","Settings").forEach { s ->
                NavigationBarItem(screen == s, { screen = s }, icon = {}, label = { Text(s) })
            }
        }
    }) { pad ->
        when (screen) {
            "Today" -> TodayScreen(Modifier.padding(pad), day, smoked, morning, cravings,
                { smokeDialog = true }, { cravingDialog = true },
                { screen = "Entries" }, { screen = "Plan" }, { screen = "Emergency" })
            "Plan" -> PlanScreen(Modifier.padding(pad), day)
            "Stats" -> StatsScreen(Modifier.padding(pad), entries)
            "Settings" -> SettingsScreen(Modifier.padding(pad), startDate) {
                val now = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0)
                }.timeInMillis
                startDate = now; entries = emptyList(); saveStartDate(now); saveEntries(entries)
            }
            "Entries" -> EntriesScreen(Modifier.padding(pad), entries)
            "Emergency" -> EmergencyScreen(Modifier.padding(pad))
        }
    }

    if (smokeDialog) SmokeDialog({ smokeDialog = false }) { source, context, intensity ->
        entries = entries + LogEntry("SMOKED", System.currentTimeMillis(), intensity, source, context)
        saveEntries(entries); smokeDialog = false
    }
    if (cravingDialog) CravingDialog({ cravingDialog = false }) { intensity, context ->
        entries = entries + LogEntry("CRAVING", System.currentTimeMillis(), intensity, context = context)
        saveEntries(entries); cravingDialog = false
    }
}

@Composable
fun TodayScreen(m: Modifier, day: Int, smoked: Int, morning: Int, cravings: Int,
                onSmoke:()->Unit, onCraving:()->Unit, onEntries:()->Unit, onPlan:()->Unit, onEmergency:()->Unit) {
    Column(m.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Quit Track", style=MaterialTheme.typography.headlineMedium)
        Text("Day $day of 40", style=MaterialTheme.typography.titleLarge)
        Text("Cigarettes today: $smoked")
        Text("Morning cigarettes: $morning")
        Text("Cravings today: $cravings")
        Button(onSmoke, Modifier.fillMaxWidth()) { Text("I smoked") }
        OutlinedButton(onCraving, Modifier.fillMaxWidth()) { Text("I have a craving") }
        OutlinedButton(onPlan, Modifier.fillMaxWidth()) { Text("View today's plan") }
        OutlinedButton(onEntries, Modifier.fillMaxWidth()) { Text("View all entries") }
        Button(onEmergency, Modifier.fillMaxWidth()) { Text("Emergency craving help") }
        OutlinedButton({}, Modifier.fillMaxWidth()) { Text("Complete daily review") }
    }
}

@Composable
fun PlanScreen(m:Modifier, day:Int) {
    val phase=((day-1)/3)+1
    Column(m.fillMaxSize().padding(20.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("Today's plan", style=MaterialTheme.typography.headlineSmall)
        Text("Day $day of 40"); Text("Phase $phase")
        Text("Focus on the targets established for this day.")
        Text("Quit Day is Day 37. Days 38–40 are smoke-free maintenance days.")
    }
}

@Composable
fun StatsScreen(m:Modifier, entries:List<LogEntry>) {
    val s=entries.filter{it.type=="SMOKED"}; val c=entries.filter{it.type=="CRAVING"}
    Column(m.fillMaxSize().padding(20.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("Statistics", style=MaterialTheme.typography.headlineSmall)
        Text("Total cigarettes: ${s.size}"); Text("Total cravings: ${c.size}")
        Text("Average craving: ${if(c.isEmpty()) "0.0" else "%.1f".format(c.map{it.intensity}.average())}/10")
        Text("Highest craving: ${c.maxOfOrNull{it.intensity} ?: 0}/10")
        Text("Bought: ${s.count{it.source=="Bought"}}")
        Text("Offered: ${s.count{it.source=="Offered"}}")
        Text("Asked for: ${s.count{it.source=="Asked for"}}")
    }
}

@Composable
fun SettingsScreen(m:Modifier, start:Long, reset:()->Unit) {
    var confirm by remember{mutableStateOf(false)}
    Column(m.fillMaxSize().padding(20.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Settings", style=MaterialTheme.typography.headlineSmall)
        Text("Start date: ${fmtDate(start)}")
        OutlinedButton({confirm=true}){Text("Reset plan and data")}
    }
    if(confirm) AlertDialog(onDismissRequest={confirm=false}, title={Text("Reset everything?")},
        text={Text("This deletes locally stored entries and restarts Day 1.")},
        confirmButton={TextButton({reset();confirm=false}){Text("Reset")}},
        dismissButton={TextButton({confirm=false}){Text("Cancel")}})
}

@Composable
fun EntriesScreen(m:Modifier, entries:List<LogEntry>) {
    LazyColumn(m.fillMaxSize().padding(20.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
        item{Text("All entries", style=MaterialTheme.typography.headlineSmall)}
        items(entries.sortedByDescending{it.time}){e->
            Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){
                Text(if(e.type=="SMOKED")"Smoked" else "Craving"); Text(fmtDateTime(e.time))
                if(e.source.isNotBlank())Text("Source: ${e.source}")
                if(e.intensity>0)Text("Intensity: ${e.intensity}/10")
                if(e.context.isNotBlank())Text("Context: ${e.context}")
            }}
        }
    }
}

@Composable
fun EmergencyScreen(m:Modifier) {
    var seconds by remember{mutableIntStateOf(600)}; var running by remember{mutableStateOf(false)}
    LaunchedEffect(running){while(running&&seconds>0){kotlinx.coroutines.delay(1000);seconds--};if(seconds==0)running=false}
    Column(m.fillMaxSize().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)){
        Text("Emergency craving help",style=MaterialTheme.typography.headlineSmall)
        Text("Delay the decision. Move away from cigarettes. Drink water. Distract yourself. Reassess.")
        Text("%02d:%02d".format(seconds/60,seconds%60),style=MaterialTheme.typography.displayMedium)
        Button({running=!running}){Text(if(running)"Pause" else "Start 10-minute timer")}
        Text("After the timer: Lower / Same / Higher")
    }
}

@Composable
fun SmokeDialog(dismiss:()->Unit, save:(String,String,Int)->Unit){
    var source by remember{mutableStateOf("Bought")}; var context by remember{mutableStateOf("")}; var intensity by remember{mutableIntStateOf(0)}
    AlertDialog(onDismissRequest=dismiss,title={Text("I smoked")},text={
        Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
            Text("How did you get it?")
            listOf("Bought","Offered","Asked for","Other").forEach{Row(verticalAlignment=Alignment.CenterVertically){
                RadioButton(source==it,{source=it});Text(it)}}
            OutlinedTextField(context,{context=it},label={Text("Context (optional)")})
            Text("Craving: ${if(intensity==0)"Not rated" else "$intensity/10"}")
            Slider(intensity.toFloat(),{intensity=it.toInt()},0f..10f,steps=9)
        }},confirmButton={TextButton({save(source,context,intensity)}){Text("Save")}},
        dismissButton={TextButton(dismiss){Text("Cancel")}})
}

@Composable
fun CravingDialog(dismiss:()->Unit, save:(Int,String)->Unit){
    var intensity by remember{mutableIntStateOf(5)};var context by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=dismiss,title={Text("I have a craving")},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("Intensity: $intensity/10")
            Slider(intensity.toFloat(),{intensity=it.toInt()},1f..10f,steps=8)
            OutlinedTextField(context,{context=it},label={Text("Situation/context")})
        }},confirmButton={TextButton({save(intensity,context)}){Text("Save")}},
        dismissButton={TextButton(dismiss){Text("Cancel")}})
}

fun sameDay(a:Long,b:Long):Boolean{
    val x=Calendar.getInstance().apply{timeInMillis=a};val y=Calendar.getInstance().apply{timeInMillis=b}
    return x.get(Calendar.YEAR)==y.get(Calendar.YEAR)&&x.get(Calendar.DAY_OF_YEAR)==y.get(Calendar.DAY_OF_YEAR)
}
fun hour(t:Long)=Calendar.getInstance().apply{timeInMillis=t}.get(Calendar.HOUR_OF_DAY)
fun fmtDate(t:Long)=SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(Date(t))
fun fmtDateTime(t:Long)=SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(Date(t))
