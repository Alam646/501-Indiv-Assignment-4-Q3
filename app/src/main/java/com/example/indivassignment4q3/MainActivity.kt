package com.example.indivassignment4q3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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

// A data class to represent a single, immutable temperature reading.
// Using a data class makes it easy to manage structured data.
data class TemperatureReading(val value: Float, val timestamp: String)

class TemperatureViewModel : ViewModel() {

    // Holds the list of the last 20 temperature readings.
    private val _readings = MutableStateFlow<List<TemperatureReading>>(emptyList())
    val readings = _readings.asStateFlow()

    // Manages the play/pause state of the data simulation.
    private val _isPaused = MutableStateFlow(false)
    val isPaused = _isPaused.asStateFlow()

    // A single formatter instance for efficiency.
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    // A reference to the running simulation coroutine, so it can be cancelled.
    private var simulationJob: Job? = null

    init {
        // Start the simulation as soon as the ViewModel is created.
        togglePauseResume()
    }

    // These are derived states. They automatically recalculate whenever the `readings` list changes.
    // This is more efficient than manually updating them each time we add a reading.
    val currentTemp = readings.map { it.firstOrNull()?.value }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val minTemp = readings.map { it.minOfOrNull { r -> r.value } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val maxTemp = readings.map { it.maxOfOrNull { r -> r.value } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val avgTemp = readings.map { if (it.isEmpty()) null else it.map { r -> r.value }.average() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


    fun togglePauseResume() {
        _isPaused.value = !_isPaused.value

        if (!_isPaused.value) {
            // Use the viewModelScope to tie the coroutine to the ViewModel's lifecycle.
            simulationJob = viewModelScope.launch {
                while (true) {
                    val newReading = TemperatureReading(
                        value = Random.nextFloat() * 20 + 65, // Simulate 65-85°F
                        timestamp = dateFormat.format(Date())
                    )

                    // `update` is a thread-safe way to modify the StateFlow's value.
                    _readings.update {
                        // Add the new reading to the front and keep only the last 20.
                        (listOf(newReading) + it).take(20)
                    }
                    delay(2000) // Requirement: update every 2 seconds.
                }
            }
        } else {
            // If paused, we must cancel the job to stop the background work.
            simulationJob?.cancel()
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IndivAssignment4Q3Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    IndivAssignment4Q3Theme {
        Greeting("Android")
    }
}
