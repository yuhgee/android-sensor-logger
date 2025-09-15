package com.example.sensorlogger

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, SensorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        setContent {
            SensorLoggerApp()
        }
    }
}

@Composable
fun SensorLoggerApp() {
    val scope = rememberCoroutineScope()
    var accelerometer by remember { mutableStateOf(FloatArray(3)) }
    var gyroscope by remember { mutableStateOf(FloatArray(3)) }
    var magnetometer by remember { mutableStateOf(FloatArray(3)) }
    var light by remember { mutableStateOf(FloatArray(1)) }

    LaunchedEffect(Unit) {
        while (true) {
            scope.launch {
                accelerometer = SensorRepository.getAccelerometer()
                gyroscope = SensorRepository.getGyroscope()
                magnetometer = SensorRepository.getMagnetometer()
                light = SensorRepository.getLight()
            }
            kotlinx.coroutines.delay(500)
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Accelerometer: ${accelerometer.joinToString()}")
        Text("Gyroscope: ${gyroscope.joinToString()}")
        Text("Magnetometer: ${magnetometer.joinToString()}")
        Text("Light: ${light.joinToString()}")
    }
}
