package com.example.carsensorsimulation.DAO

import androidx.room.Dao
import androidx.room.Insert
import com.example.carsensorsimulation.Entities.SensorData


@Dao
interface SensorDataDAO {
    @Insert
    fun insertAll(vararg sensorData: SensorData)
}