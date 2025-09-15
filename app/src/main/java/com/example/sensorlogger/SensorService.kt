package com.example.sensorlogger

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class SensorService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sensors = mutableMapOf<Int, Sensor?>()
    private val channelId = "sensor_service_channel"

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // NotificationChannel 作成 + Foreground Notification
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            channelId,
            "Sensor Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Foreground service for sensor logging" }
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Sensor Logger Running")
            .setContentText("Sensors are being logged in the background")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()

        startForeground(1, notification)

        // 監視対象センサーをまとめて登録
        sensors[Sensor.TYPE_ACCELEROMETER] = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensors[Sensor.TYPE_GYROSCOPE] = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        sensors[Sensor.TYPE_MAGNETIC_FIELD] = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensors[Sensor.TYPE_LIGHT] = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        // センサー登録
        sensors.values.forEach { sensor ->
            sensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        serviceScope.launch {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> SensorRepository.updateAccelerometer(event.values)
                Sensor.TYPE_GYROSCOPE -> SensorRepository.updateGyroscope(event.values)
                Sensor.TYPE_MAGNETIC_FIELD -> SensorRepository.updateMagnetometer(event.values)
                Sensor.TYPE_LIGHT -> SensorRepository.updateLight(event.values)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 今回は未使用
    }
}
