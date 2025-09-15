package com.example.sensorlogger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class SensorService : Service() {

    private lateinit var sensorManager: SensorManager
    private val sensors = mutableMapOf<Int, Sensor?>()

    // センサーのコールバック
    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            CoroutineScope(Dispatchers.Default).launch {
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

    // LocationListener
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val data = GpsData(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy
            )
            CoroutineScope(Dispatchers.Default).launch {
                SensorRepository.updateGps(data)
            }
        }
    }

    // GNSSコールバック
    private val gnssCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val count = status.satelliteCount
            val usedInFix = (0 until count).count { status.usedInFix(it) }
            val cn0List = (0 until count).map { status.getCn0DbHz(it) }

            CoroutineScope(Dispatchers.Default).launch {
                SensorRepository.updateGnss(GnssData(count, usedInFix, cn0List))
            }
        }
    }

    private val notificationChannelId = "sensor_service_channel"

    override fun onCreate() {
        super.onCreate()
        // Manager にアタッチ
        SensorServiceManager.attachService(this)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        createNotificationChannel()
        startForeground(1, buildNotification())
    }


    override fun onDestroy() {
        super.onDestroy()

        // Manager からデタッチ
        SensorServiceManager.detachService()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            notificationChannelId,
            "Sensor Logger Service",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Sensor Logger Running")
            .setContentText("Sensors are being recorded in background")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
    }

    fun startSensorAndLocation() {
        CoroutineScope(Dispatchers.Main).launch {
            sensors[Sensor.TYPE_ACCELEROMETER] =
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            sensors[Sensor.TYPE_GYROSCOPE] = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            sensors[Sensor.TYPE_MAGNETIC_FIELD] =
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            sensors[Sensor.TYPE_LIGHT] = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

            sensors.values.forEach {
                it?.let {
                    sensorManager.registerListener(
                        sensorEventListener,
                        it,
                        SensorManager.SENSOR_DELAY_NORMAL
                    )
                }
            }

            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val executor = Executors.newSingleThreadExecutor()

            // GPS/NETWORK/PASSIVEを追加
            try {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    0f,
                    locationListener
                )
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L,
                    0f,
                    locationListener
                )
                locationManager.requestLocationUpdates(
                    LocationManager.PASSIVE_PROVIDER,
                    1000L,
                    0f,
                    locationListener
                )
                locationManager.registerGnssStatusCallback(executor, gnssCallback)
            } catch (e: SecurityException) {
                // TODO パーミッションなしの場合のException対応
                e.printStackTrace()
            }
        }
    }

    fun stopSensorAndLocation() {
        CoroutineScope(Dispatchers.Main).launch {
            sensorManager.unregisterListener(sensorEventListener)
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.removeUpdates(locationListener)
            locationManager.unregisterGnssStatusCallback(gnssCallback)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
