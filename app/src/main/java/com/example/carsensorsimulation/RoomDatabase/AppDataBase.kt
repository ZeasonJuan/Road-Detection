package com.example.carsensorsimulation.RoomDatabase

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.carsensorsimulation.DAO.SensorDataDAO
import com.example.carsensorsimulation.Entities.SensorData

@Database(entities = [SensorData::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sensorDataDAO(): SensorDataDAO
}