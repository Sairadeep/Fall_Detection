package com.weguard.gyroscopesensor

import androidx.compose.runtime.mutableIntStateOf

object Utils {
    private var gyroscopeCrashDetect = mutableIntStateOf(0)
    private var accelerometerCrashDetection = mutableIntStateOf(0)
//    private var mediaPlayer : MediaPlayer? = null
//
//    fun setMediaPlayer(context : Context, value : Int){
//        if(mediaPlayer == null){
//            mediaPlayer = MediaPlayer.create(context, value)
//        }
//    }
//
//    fun getMediaPlayer(): MediaPlayer? {
//        return mediaPlayer
//    }

    fun setGyroscopeDetection(crashValue: Int) {
        gyroscopeCrashDetect.intValue = crashValue
    }

    fun getGyroscopeDetection() = gyroscopeCrashDetect

    fun setAccelerometerDetection(crashAccValue: Int) {
        accelerometerCrashDetection.intValue = crashAccValue
    }

    fun getAccelerometerDetection() = accelerometerCrashDetection

}