package com.example.sensorlogger

object SensorServiceManager {

    // サービスの参照（Activityなどからセットする）
    private var sensorService: SensorService? = null

    // サービスインスタンスをセット
    fun attachService(service: SensorService) {
        sensorService = service
    }

    fun detachService() {
        sensorService = null
    }

    // 外部からセンサー開始
    fun startSensors() {
        sensorService?.startSensorAndLocation()
    }

    // 外部からセンサー停止
    fun stopSensors() {
        sensorService?.stopSensorAndLocation()
    }
}
