package com.example.sensorlogger

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // フォアグラウンドサービスを起動
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
    var gps by remember { mutableStateOf(GpsData(0.0, 0.0, 0f)) }
    var gnss by remember { mutableStateOf(GnssData(0, 0, emptyList())) }
    var isRunning by remember { mutableStateOf(false) }

    // 定期更新（UI用サンプル）
    LaunchedEffect(Unit) {
        while (true) {
            scope.launch {
                accelerometer = SensorRepository.getAccelerometer()
                gyroscope = SensorRepository.getGyroscope()
                magnetometer = SensorRepository.getMagnetometer()
                light = SensorRepository.getLight()
                gps = SensorRepository.getGps()
                gnss = SensorRepository.getGnss()
            }
            kotlinx.coroutines.delay(500)
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Accelerometer: ${accelerometer.joinToString()}")
        Text("Gyroscope: ${gyroscope.joinToString()}")
        Text("Magnetometer: ${magnetometer.joinToString()}")
        Text("Light: ${light.joinToString()}")

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        Text("GPS: Lat=${gps.latitude}, Lon=${gps.longitude}, Accuracy=${gps.accuracy}m")
        Text("GNSS: Satellites=${gnss.satelliteCount}, UsedInFix=${gnss.usedInFixCount}")
        Text("SNR: ${gnss.snrList.joinToString()}")

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (!isRunning) {
                    startSensorsSafe(scope)
                    isRunning = true
                } else {
                    stopSensorsSafe(scope)
                    isRunning = false
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isRunning) "Stop Sensors" else "Start Sensors")
        }
    }
}

// ワーカースレッドでセンサー開始
fun startSensorsSafe(scope: CoroutineScope) {
    scope.launch(Dispatchers.Default) {
        SensorServiceManager.startSensors()
    }
}

// ワーカースレッドでセンサー停止
fun stopSensorsSafe(scope: CoroutineScope) {
    scope.launch(Dispatchers.Default) {
        SensorServiceManager.stopSensors()
    }
}
