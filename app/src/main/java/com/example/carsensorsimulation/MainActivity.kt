package com.example.carsensorsimulation

import android.app.LocaleManager
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.util.LruCache
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.carsensorsimulation.Entities.SensorData
import com.example.carsensorsimulation.RoomDatabase.AppDatabase
import com.example.carsensorsimulation.ui.theme.CarSensorSimulationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.sql.Array

private const val REQUEST_LOCATION_PERMISSION = 1
private lateinit var sensorManager: SensorManager
private var n = 3.0
var speedState = mutableStateOf(0f)
var acc = mutableStateOf(0f)
var gyro = mutableStateOf(0f)
var linAcc = mutableStateOf(0f)
var rotat = mutableStateOf(0f)
private const val cacheSize = 10 * 1024 * 1024
private val sensorDataCache = LruCache<Int, kotlin.Array<FloatArray>>(cacheSize)
private val timeCache = LruCache<Int, Long>(cacheSize / 1024)
private val speedCache = LruCache<Int, Float>(cacheSize / 1024)
private var key = 0
private var db: AppDatabase? = null

class MainActivity : ComponentActivity() {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var linearAccelerometer: Sensor? = null
    private var rotationVector: Sensor? = null
    private lateinit var locationManager: LocationManager



    private var arrayOfSensors = arrayOfNulls<FloatArray>(4)
    //an array used to mark the time at which each sensor get the data
    private var arrayOfTimes = arrayOfNulls<Long>(4)


    //cache that used for store the sensor data temporarily

    private var realSpeed: Float = 0.0f



    fun removeCache() {
        speedCache.evictAll()
        sensorDataCache.evictAll()
        timeCache.evictAll()
        key = 0
    }

    //an simple fun to clear time array
    fun timeArrayClear() {
        for(i in arrayOfTimes.indices) {
            arrayOfTimes[i] = null
        }
    }

    //another simple fun to clear sensor array
    fun sensorArrayClear() {
        for(i in arrayOfSensors.indices) {
            arrayOfSensors[i] = null
        }
    }

    fun bothArrayClear() {
        sensorArrayClear()
        timeArrayClear()
    }

    //Find the earliest time of the array
    fun getEarliestTime(): Long? {
        if (arrayOfTimes.all { it == null }) {
            return System.currentTimeMillis()
        }
        var earliestTime: Long? = Long.MAX_VALUE
        for (i in arrayOfTimes.indices) {
            if (arrayOfTimes[i] == null) {
                continue
            }
            if (arrayOfTimes[i]!! < earliestTime!!) {
                earliestTime = arrayOfTimes[i]
            }
        }
        return earliestTime
    }

    //fun that judge the time is out or not
    fun isTimeOut(): Boolean {
        return secondBetweenNowAnd(getEarliestTime()!!) > 0.1
    }

    fun timeJudge() {
        if (isTimeOut()) {
            bothArrayClear()
        }
    }

    fun replaceRunOnUi(state: MutableState<Float>?) {
        state ?: return
        runOnUiThread {
            state.value = n.toFloat()
            n += 1
            Log.d("UpdateUI", "UI updated with n = $n")
        }
    }



    //a little function to display some data in screen
    fun updateSpeedOnUI(data: Float, typeOfData: String?) {
        // sure that UI changed in main thread
        typeOfData ?: return
        when(typeOfData) {
            "speed" -> {
                replaceRunOnUi(speedState)
            }
            "accelerometer" -> {
                replaceRunOnUi(acc)
            }
            "gyroscope" -> {
                replaceRunOnUi(gyro)
            }
            "linear" -> {
                replaceRunOnUi(linAcc)
            }
            "rotation" -> {
                replaceRunOnUi(rotat)
            }
        }

    }
    // register locationListener to take the speed of mobile
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (location.hasSpeed()) {
                val speed = location.speed
                updateSpeedOnUI(speed, "speed")
                realSpeed = speed
            }
        }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }
        override fun onProviderEnabled(provider: String) {
        }
        override fun onProviderDisabled(provider: String) {
        }
    }
    //define sensorListener after get the data of sensors
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(p0: SensorEvent?) {
            p0 ?: return
            //judge if this four sensor data is out, if so, delete them all
            timeJudge()

            when(p0.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    //display X axis of the sensor
                    updateSpeedOnUI(p0.values[0], "accelerometer")

                    arrayOfSensors[0] = p0.values
                    arrayOfTimes[0] = System.currentTimeMillis()
                }
                Sensor.TYPE_GYROSCOPE -> {
                    updateSpeedOnUI(p0.values[0], "gyroscope")

                    arrayOfSensors[1] = p0.values
                    arrayOfTimes[1] = System.currentTimeMillis()
                }
                Sensor.TYPE_LINEAR_ACCELERATION -> {
                    updateSpeedOnUI(p0.values[0], "linear")

                    arrayOfSensors[2] = p0.values
                    arrayOfTimes[2] = System.currentTimeMillis()
                }
                Sensor.TYPE_ROTATION_VECTOR -> {
                    updateSpeedOnUI(p0.values[0], "rotation")

                    arrayOfSensors[3] = p0.values
                    arrayOfTimes[3] = System.currentTimeMillis()
                }
            }
            if (arrayOfSensors.all { it != null }) {
                sensorDataCache.put(key, arrayOfSensors)
                timeCache.put(key, System.currentTimeMillis())
                speedCache.put(key, realSpeed)
                key++
            }
        }
        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_LOCATION_PERMISSION)
        }


        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        linearAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(sensorListener, gyroscope, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(sensorListener, linearAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(sensorListener, rotationVector, SensorManager.SENSOR_DELAY_NORMAL)

        lifecycleScope.launch {
            db = withContext(Dispatchers.IO) {
                // 确保这里导入了 AppDatabase 的正确路径
                Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    "database-name"
                ).build()
            }
        }

        setContent {
            CarSensorSimulationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }
    }
}

//a more convenient way to calculate second
fun secondBetweenNowAnd(time: Long): Long {
    return (System.currentTimeMillis() - time) / 1000
}

@Composable
fun Greeting(name: String) {
    Surface(color = Color.Cyan) {
        Text(
            text = "Speed: ${speedState.value}!\nAccelerometer: ${acc.value}! " +
                    "\nGyroscope: ${gyro.value}!\nLinear: ${linAcc.value}!\nRotation: ${rotat.value}!",
            modifier = Modifier.size(400.dp, 200.dp),
            style = TextStyle(
                fontSize = 30.sp
            )
        )
    }
}

@Composable
fun RecordButton(abnormalType: String) {
    Button(
        onClick = {
            var sensorDataList = ArrayList<SensorData>()
            for (i in 0..key-1) {

                //if the record is too long ago, pass it
                if (secondBetweenNowAnd(timeCache.get(i)) > 5.0) {
                    continue
                }
                var thisSensorRecord = SensorData(
                    accelerometerX = sensorDataCache.get(i)[0][0],
                    accelerometerY = sensorDataCache.get(i)[0][1],
                    accelerometerZ = sensorDataCache.get(i)[0][2],
                    gyroscopeX = sensorDataCache.get(i)[1][0],
                    gyroscopeY = sensorDataCache.get(i)[1][1],
                    gyroscopeZ = sensorDataCache.get(i)[1][2],
                    linearAccelerometerX = sensorDataCache.get(i)[2][0],
                    linearAccelerometerY = sensorDataCache.get(i)[2][1],
                    linearAccelerometerZ = sensorDataCache.get(i)[2][2],
                    rotationVectorX = sensorDataCache.get(i)[3][0],
                    rotationVectorY = sensorDataCache.get(i)[3][1],
                    rotationVectorZ = sensorDataCache.get(i)[3][2],
                    time = timeCache.get(i),
                    roadType = abnormalType
                )
                sensorDataList.add(thisSensorRecord)
            }
            //Open a new thread so that write records in Room Database
            
        },
        modifier = Modifier.padding(4.dp),
    ) {
        Text(
            text = abnormalType,
            style = TextStyle(
                fontSize = 15.sp
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CarSensorSimulationTheme {
        ConstraintLayout (modifier = Modifier.fillMaxSize()){
            Surface(color = Color.Black) {
                val (row) = createRefs()

                Box(modifier = Modifier.constrainAs(row) {
                    bottom.linkTo(parent.bottom, margin = 16.dp)
                }.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter) {
                        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                            RecordButton(abnormalType = "坑")
                            RecordButton(abnormalType = "异物")
                            RecordButton(abnormalType = "积水")
                            RecordButton(abnormalType = "裂缝")
                            RecordButton(abnormalType = "积雪")
                    }

                }

                Greeting("Zeason Juan")
//
            }

        }

    }
}