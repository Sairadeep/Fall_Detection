package com.weguard.gyroscopesensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Half.EPSILON
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.weguard.gyroscopesensor.ui.theme.GyroscopeSensorTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null
    private var accelerometer: Sensor? = null
    private val linearAcceleration = FloatArray(3)
    private val gravity = FloatArray(3)
    private var callStatus: Boolean = false
    private var xArray = ArrayList<Float>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GyroscopeSensorTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigations()
                }
            }
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (gyroscope != null && accelerometer != null) {
            Toast.makeText(
                this@MainActivity,
                "Gyroscope and Accelerometer sensors exists...!",
                Toast.LENGTH_SHORT
            )
                .show()
        } else {
            Toast.makeText(
                this@MainActivity,
                "Gyroscope and Accelerometer sensor doesn't exists...!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    override fun onResume() {
        super.onResume()
        gyroscope?.let { gyroscopeSensor ->
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        accelerometer?.let { accelerometerSensor ->
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this, gyroscope)
        sensorManager.unregisterListener(this, accelerometer)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {

            when (event.sensor) {

                gyroscope -> {
                    var axisX: Float = event.values[0]
                    var axisY: Float = event.values[1]
                    var axisZ: Float = event.values[2]

//                 Angular velocity magnitude refers to the overall rate of rotation of an object around an axis,
//                 regardless of the direction of rotation
                    val omegaMagnitude: Float = sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)

                    // Normalize the rotation vector if it's big enough to get the axis
                    // (i.e., EPSILON should represent your maximum allowable margin of error)
                    if (omegaMagnitude > EPSILON) {
                        axisX /= omegaMagnitude
                        axisY /= omegaMagnitude
                        axisZ /= omegaMagnitude
                    }

                    if (omegaMagnitude > 1.8f) {
                        Utils.setGyroscopeDetection(1)
                        callStatus = true
                    } else {
                        Utils.setGyroscopeDetection(0)
                    }
                }

                accelerometer -> {
                    val alpha = 0.8f //  alpha is calculated as t / (t + dT)
                    val fallThreshold = -2f

                    // Isolate the force of gravity with the low-pass filter.
                    gravity[0] = alpha * gravity[0] + ((1 - alpha) * event.values[0])
                    gravity[1] = alpha * gravity[1] + ((1 - alpha) * event.values[1])
                    gravity[2] = alpha * gravity[2] + ((1 - alpha) * event.values[2])

                    // Remove the gravity contribution with the high-pass filter.
                    linearAcceleration[0] = event.values[0] - gravity[0]
                    linearAcceleration[1] = event.values[1] - gravity[1]
                    linearAcceleration[2] = event.values[2] - gravity[2]

                    // Gravity check needs to be added.
                    if (linearAcceleration[1] < fallThreshold) {
                        val x = abs(linearAcceleration[1])
                        xArray.add(x)
                        Log.d("nowX", "x: $x, xArray: $xArray and Size: ${xArray.size}")
                        if (xArray.size > 1) {
                            for (i in 0 until xArray.size - 1) {
                                val iNow = xArray[i]
                                val iOnIncrement = xArray[i + 1]
                                Log.d("nowX", "i: $iNow, i+1: $iOnIncrement")
                                if (iNow < iOnIncrement) {
                                    // Fall need to have an sudden increase.
                                    CoroutineScope(Dispatchers.IO).launch {
                                        delay(800)
                                        val y: Float = abs(linearAcceleration[1])
                                        Log.d("linearAcceleration", "Y: $y") // ~ 0f
                                        val z = x - y
                                        val r = x / 2
                                        callStatus = if (z > r && y > 0.5) {
                                            Log.d("FallState", "Fall x: $x, y: $y, z: $z, r: $r")
                                            Utils.setAccelerometerDetection(1)
                                            true
                                        } else {
                                            Log.d("FallState", "No Fall x: $x, y: $y, z: $z, r: $r")
                                            Utils.setAccelerometerDetection(0)
                                            false
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Utils.setAccelerometerDetection(0)
                    }
                }
            }
        } else {
            Toast.makeText(this@MainActivity, "Empty..!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
//      Toast.makeText(this@MainActivity, "Accuracy: $accuracy", Toast.LENGTH_SHORT).show()
        Log.d("Accuracy:", "$accuracy")
    }
}

@Composable
fun AppNavigations() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "HomePage") {
        composable(route = "HomePage") {
            EmergencyCall(navController)
        }
        composable(
            route = "DetectionPage/{toStartIt}",
            arguments = listOf(navArgument("toStartIt") { type = NavType.BoolType })
        ) { navBackstackEntry ->
            val startAction = navBackstackEntry.arguments
                ?.getBoolean("toStartIt")
            startAction?.let { actionValue ->
                IfDetectionHappened(navController,actionValue)
            }
        }
    }
}


