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
import com.example.carsensorsimulation.ui.theme.CarSensorSimulationTheme
private const val REQUEST_LOCATION_PERMISSION = 1
private lateinit var sensorManager: SensorManager
private var n = 3.0
var speedState = mutableStateOf(0f)
var acc = mutableStateOf(0f)
var gyro = mutableStateOf(0f)
var linAcc = mutableStateOf(0f)
var rotat = mutableStateOf(0f)

class MainActivity : ComponentActivity() {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var linearAccelerometer: Sensor? = null
    private var rotationVector: Sensor? = null
    private lateinit var locationManager: LocationManager

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
            when(p0.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    updateSpeedOnUI(p0.values[0], "accelerometer")
                }
                Sensor.TYPE_GYROSCOPE -> {
                    updateSpeedOnUI(p0.values[0], "gyroscope")
                }
                Sensor.TYPE_LINEAR_ACCELERATION -> {
                    updateSpeedOnUI(p0.values[0], "linear")
                }
                Sensor.TYPE_ROTATION_VECTOR -> {
                    updateSpeedOnUI(p0.values[0], "rotation")
                }
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
        onClick = { /*TODO*/ },
        modifier = Modifier.padding(4.dp),
    ) {
        Text(
            text = abnormalType,
            style = TextStyle(
                fontSize = 15.sp
            ))
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