package com.weguard.gyroscopesensor

import androidx.compose.runtime.mutableIntStateOf

object Utils {
    var gyroscopeCrashDetect = mutableIntStateOf(0)
    var accelerometerCrashDetection = mutableIntStateOf(0)

    fun setGyroscopeDetection(crashValue: Int) {
        gyroscopeCrashDetect.intValue = crashValue
    }

    fun getGyroscopeDetection() = gyroscopeCrashDetect

    fun setAccelerometerDetection(crashAccValue: Int) {
        accelerometerCrashDetection.intValue = crashAccValue
    }

    fun getAccelerometerDetection() = accelerometerCrashDetection

}