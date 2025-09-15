package com.example.sensorlogger

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class GpsData(val latitude: Double, val longitude: Double, val accuracy: Float)
data class GnssData(val satelliteCount: Int, val usedInFixCount: Int, val snrList: List<Float>)

object SensorRepository {

    private val mutex = Mutex()

    private var _accelerometerValues: FloatArray = FloatArray(3)
    private var _gyroscopeValues: FloatArray = FloatArray(3)
    private var _magnetometerValues: FloatArray = FloatArray(3)
    private var _lightValues: FloatArray = FloatArray(1)

    private var _gpsValues: GpsData = GpsData(0.0, 0.0, 0f)
    private var _gnssValues: GnssData = GnssData(0, 0, emptyList())

    // --- 更新 ---
    suspend fun updateAccelerometer(values: FloatArray) {
        mutex.withLock { _accelerometerValues = values.copyOf() }
    }

    suspend fun updateGyroscope(values: FloatArray) {
        mutex.withLock { _gyroscopeValues = values.copyOf() }
    }

    suspend fun updateMagnetometer(values: FloatArray) {
        mutex.withLock { _magnetometerValues = values.copyOf() }
    }

    suspend fun updateLight(values: FloatArray) {
        mutex.withLock { _lightValues = values.copyOf() }
    }

    suspend fun updateGps(values: GpsData) {
        mutex.withLock { _gpsValues = values }
    }

    suspend fun updateGnss(values: GnssData) {
        mutex.withLock { _gnssValues = values }
    }

    // --- 取得（非 null保証） ---
    suspend fun getAccelerometer(): FloatArray = mutex.withLock { _accelerometerValues.copyOf() }
    suspend fun getGyroscope(): FloatArray = mutex.withLock { _gyroscopeValues.copyOf() }
    suspend fun getMagnetometer(): FloatArray = mutex.withLock { _magnetometerValues.copyOf() }
    suspend fun getLight(): FloatArray = mutex.withLock { _lightValues.copyOf() }

    suspend fun getGps(): GpsData = mutex.withLock { _gpsValues }
    suspend fun getGnss(): GnssData = mutex.withLock { _gnssValues }
}
