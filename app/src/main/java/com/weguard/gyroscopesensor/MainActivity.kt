package com.weguard.gyroscopesensor

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.util.Half.EPSILON
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.weguard.gyroscopesensor.ui.theme.GyroscopeSensorTheme
import kotlin.math.sqrt

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null
    private var accelerometer: Sensor? = null
    private val linearAcceleration = FloatArray(3)
    private val gravity = FloatArray(3)
    private var callStatus: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GyroscopeSensorTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EmergencyCall()
                }
            }
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (gyroscope != null) {
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
        // This time stamp delta rotation to be multiplied by the current rotation
        // after computing it from the gyro sample data.
        if (event != null) {

            when (event.sensor) {

                gyroscope -> {
                    // Axis of the rotation sample, not normalized yet.
                    var axisX: Float = event.values[0]
                    Log.d("Axis-X", "$axisX")
                    var axisY: Float = event.values[1]
                    Log.d("Axis-Y", "$axisY")
                    var axisZ: Float = event.values[2]
                    Log.d("Axis-Z", "$axisZ")

//                 Calculate the angular speed of the sample
//                 Angular velocity magnitude refers to the overall rate of rotation of an object around an axis,
//                 regardless of the direction of rotation
                    val omegaMagnitude: Float = sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)
                    Log.d("omegaMagnitude", "$omegaMagnitude")

                    // Normalize the rotation vector if it's big enough to get the axis
                    // (i.e., EPSILON should represent your maximum allowable margin of error)
                    if (omegaMagnitude > EPSILON) {
                        axisX /= omegaMagnitude
                        axisY /= omegaMagnitude
                        axisZ /= omegaMagnitude
                    }

                    if (omegaMagnitude > 1.2f) {
                        Utils.setGyroscopeDetection(1)
                        callStatus = true
                    } else {
                        Utils.setGyroscopeDetection(0)
                    }
                }

                accelerometer -> {
//                  alpha is calculated as t / (t + dT)
                    val alpha = 0.8f
                    val fallThreshold = -2f

                    // Isolate the force of gravity with the low-pass filter.
                    gravity[0] = alpha * gravity[0] + ((1 - alpha) * event.values[0])
                    Log.d("gravity X-Axis", "${gravity[0]}")
                    gravity[1] = alpha * gravity[1] + ((1 - alpha) * event.values[1])
                    Log.d("gravity Y-Axis", "${gravity[1]}")
                    gravity[2] = alpha * gravity[2] + ((1 - alpha) * event.values[2])
                    Log.d("gravity Z-Axis", "${gravity[2]}")

                    // Remove the gravity contribution with the high-pass filter.
                    linearAcceleration[0] = event.values[0] - gravity[0]
                    Log.d("Acceleration X-Axis", "${linearAcceleration[0]}")
                    linearAcceleration[1] = event.values[1] - gravity[1]
                    Log.d("Acceleration Y-Axis", "${linearAcceleration[1]}")
                    linearAcceleration[2] = event.values[2] - gravity[2]
                    Log.d("Acceleration Z-Axis", "${linearAcceleration[2]}")

                    if (linearAcceleration[1] < fallThreshold) {
                        Log.d("linearAcceleration", "${linearAcceleration[2]}")
                        Utils.setAccelerometerDetection(1)
                        callStatus = true
                    } else {
                        Log.d("linearAcceleration", "Safe....!")
                        Utils.setAccelerometerDetection(0)
                    }
                }
            }
        } else {
            Toast.makeText(this@MainActivity, "Empty..!", Toast.LENGTH_SHORT).show()
        }

        if (Utils.getGyroscopeDetection().intValue == 1 || Utils.getAccelerometerDetection().intValue == 1) {
            Log.d("Crash Detection", "Crash....!")
            Utils.setAccelerometerDetection(0)
            Utils.setGyroscopeDetection(0)
            if (callStatus) {
                val intent = Intent(Intent.ACTION_CALL)
                val phoneNumber = 1234567890
                intent.data = Uri.parse("tel: $phoneNumber")
                startActivity(intent)
                callStatus = false
            } else {
                Toast.makeText(this@MainActivity, "Calling Emergency", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Toast.makeText(this@MainActivity, "Accuracy: $accuracy", Toast.LENGTH_SHORT).show()
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyCall() {
    val context = LocalContext.current
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(text = "Engine", fontSize = 20.sp) },
            actions = {
                IconButton(onClick = {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CALL_PHONE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // Permission to be prompted
                        ActivityCompat.requestPermissions(
                            context as Activity,
                            arrayOf(Manifest.permission.CALL_PHONE),
                            100
                        )
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_question_mark_24),
                        contentDescription = "Phone permission"
                    )
                }
            }
        )
    }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Emergency call back")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GyroscopeSensorTheme {
        EmergencyCall()
    }
}