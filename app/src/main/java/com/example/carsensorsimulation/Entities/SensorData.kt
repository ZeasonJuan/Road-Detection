package com.example.carsensorsimulation.Entities

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class SensorData (
    @PrimaryKey val time: Long,
    val accelerometerX: Float,
    val accelerometerY: Float,
    val accelerometerZ: Float,
    val gyroscopeX: Float,
    val gyroscopeY: Float,
    val gyroscopeZ: Float,
    val linearAccelerometerX: Float,
    val linearAccelerometerY: Float,
    val linearAccelerometerZ: Float,
    val rotationVectorX: Float,
    val rotationVectorY: Float,
    val rotationVectorZ: Float,
    val roadType: String
)