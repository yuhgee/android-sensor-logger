package com.example.sensorlogger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.*
import android.location.*
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SensorService : Service(), SensorEventListener {

    private val sensors = mutableMapOf<Int, Sensor?>()
    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager

    private val scope = CoroutineScope(Dispatchers.IO)

    private val notificationChannelId = "sensor_logger_channel"
    private val notificationId = 1

    override fun onCreate() {
        super.onCreate()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // --- Foreground Notification ---
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(notificationChannelId, "Sensor Logger", NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
        val notification: Notification = Notification.Builder(this, notificationChannelId)
            .setContentTitle("Sensor Logger Running")
            .setContentText("Collecting sensor & GPS data")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .build()
        startForeground(notificationId, notification)

        // --- センサー登録 ---
        sensors[Sensor.TYPE_ACCELEROMETER] = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensors[Sensor.TYPE_GYROSCOPE] = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        sensors[Sensor.TYPE_MAGNETIC_FIELD] = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensors[Sensor.TYPE_LIGHT] = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        sensors.values.forEach { sensor ->
            sensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        }

        // --- GPS / GNSS ---
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f) { location ->
                Log.d("requestLocationUpdates", "location: $location")
                scope.launch {
                    SensorRepository.updateGps(GpsData(location.latitude, location.longitude, location.accuracy))
                }
            }

            locationManager.registerGnssStatusCallback(object : GnssStatus.Callback() {
                override fun onSatelliteStatusChanged(status: GnssStatus) {
                    Log.d("onSatelliteStatusChanged", "status: $status")
                    val satelliteCount = status.satelliteCount
                    val usedInFix = (0 until satelliteCount).count { status.usedInFix(it) }
                    val snrList = (0 until satelliteCount).map { status.getCn0DbHz(it) }

                    scope.launch {
                        SensorRepository.updateGnss(GnssData(satelliteCount, usedInFix, snrList))
                    }
                }
            })
        } catch (e: SecurityException) {
            Log.e("SensorService", "Permission denied for location/GNSS", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        locationManager.removeUpdates { }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // --- SensorEventListener ---
    override fun onSensorChanged(event: SensorEvent) {
        scope.launch {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> SensorRepository.updateAccelerometer(event.values)
                Sensor.TYPE_GYROSCOPE -> SensorRepository.updateGyroscope(event.values)
                Sensor.TYPE_MAGNETIC_FIELD -> SensorRepository.updateMagnetometer(event.values)
                Sensor.TYPE_LIGHT -> SensorRepository.updateLight(event.values)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
