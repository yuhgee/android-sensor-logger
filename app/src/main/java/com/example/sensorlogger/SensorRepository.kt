package com.example.sensorlogger

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object SensorRepository {

    private val mutex = Mutex()

    private var _accelerometerValues: FloatArray = FloatArray(3)
    private var _gyroscopeValues: FloatArray = FloatArray(3)
    private var _magnetometerValues: FloatArray = FloatArray(3)
    private var _lightValues: FloatArray = FloatArray(1)

    // 更新
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

    // 取得
    suspend fun getAccelerometer(): FloatArray = mutex.withLock { _accelerometerValues.copyOf() }
    suspend fun getGyroscope(): FloatArray = mutex.withLock { _gyroscopeValues.copyOf() }
    suspend fun getMagnetometer(): FloatArray = mutex.withLock { _magnetometerValues.copyOf() }
    suspend fun getLight(): FloatArray = mutex.withLock { _lightValues.copyOf() }
}
