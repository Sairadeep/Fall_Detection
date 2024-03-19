package com.weguard.gyroscopesensor

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyCall(navController: NavController) {
    val context = LocalContext.current
    val toLoadDetection  = true
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
                    } else {
                        Toast.makeText(context, "Permission already granted", Toast.LENGTH_SHORT)
                            .show()
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
            if (Utils.getGyroscopeDetection().intValue == 1 || Utils.getAccelerometerDetection().intValue == 1) {
                Log.d("Crash Detection", "Crash....!")
                Utils.setAccelerometerDetection(0)
                Utils.setGyroscopeDetection(0)
                // navigate to detection page
                navController.navigate("DetectionPage/$toLoadDetection")
            }
        }
    }
}