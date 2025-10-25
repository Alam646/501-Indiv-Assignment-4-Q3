package com.example.indivassignment4q3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.indivassignment4q3.ui.theme.IndivAssignment4Q3Theme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

data class TemperatureReading(val value: Float, val timestamp: String)

class TemperatureViewModel : ViewModel() {
    private val _readings = MutableStateFlow<List<TemperatureReading>>(emptyList())
    val readings = _readings.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused = _isPaused.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private var simulationJob: Job? = null

    init {
        togglePauseResume() // Start simulation immediately.
    }

    val currentTemp = readings.map { it.firstOrNull()?.value }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val minTemp = readings.map { it.minOfOrNull { r -> r.value } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val maxTemp = readings.map { it.maxOfOrNull { r -> r.value } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val avgTemp = readings.map { if (it.isEmpty()) null else it.map { r -> r.value }.average() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun togglePauseResume() {
        _isPaused.value = !_isPaused.value

        if (!_isPaused.value) {
            simulationJob = viewModelScope.launch {
                while (true) {
                    val newReading = TemperatureReading(
                        value = Random.nextFloat() * 20 + 65, // 65-85°F
                        timestamp = dateFormat.format(Date())
                    )
                    _readings.update { (listOf(newReading) + it).take(20) }
                    delay(2000) // Requirement: update every 2 seconds.
                }
            }
        } else {
            simulationJob?.cancel()
        }
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel: TemperatureViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IndivAssignment4Q3Theme {
                TemperatureDashboardApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemperatureDashboardApp(viewModel: TemperatureViewModel) {
    val isPaused by viewModel.isPaused.collectAsState()

    Scaffold(
        topBar = {
            TopBar(isPaused = isPaused, onToggle = { viewModel.togglePauseResume() })
        }
    ) { paddingValues ->
        DashboardScreen(
            modifier = Modifier.padding(paddingValues),
            viewModel = viewModel
        )
    }
}

@Composable
fun DashboardScreen(modifier: Modifier = Modifier, viewModel: TemperatureViewModel) {
    val readings by viewModel.readings.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val currentTemp by viewModel.currentTemp.collectAsState()
    val minTemp by viewModel.minTemp.collectAsState()
    val maxTemp by viewModel.maxTemp.collectAsState()
    val avgTemp by viewModel.avgTemp.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        SummaryView(currentTemp, minTemp, maxTemp, avgTemp, isPaused)
        LineChart(readings = readings)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Recent Readings",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(readings) { reading ->
                ReadingItem(reading)
            }
        }
    }
}

@Composable
fun LineChart(readings: List<TemperatureReading>) {
    // Only draw the chart if there is more than one point to plot.
    if (readings.size < 2) return

    val data = readings.reversed()
    val minTemp = 65f
    val maxTemp = 85f
    val chartColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) { 
        val path = Path()
        val xStep = size.width / (data.size - 1)
        val yRange = maxTemp - minTemp

        data.forEachIndexed { index, reading ->
            val x = index * xStep
            // Normalize the y-value to the canvas height, inverting it as canvas Y is top-to-bottom.
            val y = size.height - ((reading.value - minTemp) / yRange) * size.height

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = chartColor,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

@Composable
fun SummaryView(current: Float?, min: Float?, max: Float?, avg: Double?, isPaused: Boolean) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Live Dashboard",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Status: ${if (isPaused) "Paused" else "Running"}",
            color = if (isPaused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatCard(label = "Current", value = current)
            StatCard(label = "Min", value = min)
            StatCard(label = "Max", value = max)
            StatCard(label = "Average", value = avg?.toFloat())
        }
    }
}

@Composable
fun StatCard(label: String, value: Float?) {
    Card(elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(
                text = value?.roundTo1Decimal() ?: "--",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ReadingItem(reading: TemperatureReading) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = reading.timestamp, modifier = Modifier.weight(1f))
        Text(text = "${reading.value.roundTo1Decimal()}°F")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(isPaused: Boolean, onToggle: () -> Unit) {
    TopAppBar(
        title = { Text("Temperature Dashboard") },
        actions = {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = "Pause or Resume"
                )
            }
        }
    )
}

fun Float.roundTo1Decimal(): String = "%.1f".format(this)

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    IndivAssignment4Q3Theme {
        val viewModel = TemperatureViewModel()
        DashboardScreen(viewModel = viewModel)
    }
}
