package com.weguard.gyroscopesensor

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun IfDetectionHappened(navController: NavController, toLoad: Boolean) {
    val context = LocalContext.current
//    launches a coroutine that gradually increases the value of progress. This causes the progress indicator to iterate up in turn.
    val loading = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var progressValue by remember {
        mutableFloatStateOf(0f)
    }
    val iconState = remember { mutableStateOf(false) }
    val callStatus = remember { mutableStateOf(false) }
    val pageState = remember { mutableStateOf(false) }
    val mediaPlayer: MediaPlayer = MediaPlayer.create(context, R.raw.alarm)

    val receiver: BroadcastReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d("ScreenStateNow", "Screen Off")
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                    scope.cancel()
                    loading.value = false
                    iconState.value = false
                } else {
                    Log.d("ScreenStateNow", "mediaPlayerState: ${mediaPlayer.isPlaying}")
                }
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        pageState.value = toLoad
//        Log.d("NavigationResult", "$toLoad")
        LaunchedEffect(Unit) {
//          LaunchedEffect guarantees that its code block is executed only once
//          when the composable is initially composed.
            if (pageState.value && !mediaPlayer.isPlaying) {
                mediaPlayer.start()
                loading.value = true
                iconState.value = true
                scope.launch {
                    loadProgress { progress ->
                        progressValue = progress
                    }
                    loading.value = false
                    iconState.value = false
                    callStatus.value = true
                }
                pageState.value = false
            } else {
                Log.d("CurrentState", "Play nothing.")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(100))
                .background(color = Color.LightGray, RoundedCornerShape(100)),
            contentAlignment = Alignment.Center
        )
        {
            if (!iconState.value) Icon(
                Icons.Default.PlayArrow,
                contentDescription = "",
                modifier = Modifier.size(80.dp),
                tint = Color.Red
            ) else {
                Text(
                    text = "${(progressValue * 15).toInt()}",
                    fontSize = 55.sp,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }
            if (loading.value) {
                CircularProgressIndicator(
                    modifier = Modifier.size(150.dp),
                    color = Color.Red,
                    trackColor = Color.Cyan,
                    progress = {
                        // Changing it to { progressValue } instead of without adding parenthesis.
                        progressValue
                    }
                )
            }

            if (callStatus.value) {
                val intent = Intent(Intent.ACTION_CALL)
                val phoneNumber = 1234567890
                intent.data = Uri.parse("tel: $phoneNumber")
                LocalContext.current.startActivity(intent)
                callStatus.value = false
            } else {
                Log.d("CallingEmergency", "Make an emergency call")
            }
        }
    }

    // To Register or Unregister
    DisposableEffect(context) {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        filter.addAction(Intent.ACTION_USER_BACKGROUND)
        context.registerReceiver(receiver, filter)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
    //  Unable to do anything on back navigation as mediaPlayer.isPlaying return FALSE
    BackHandler(enabled = true, onBack = {
        Log.d(
            "UserClickedBackButton",
            "Navigating to previous screen ${mediaPlayer.isPlaying} && ${mediaPlayer.currentPosition} && ${mediaPlayer.audioSessionId}"
        )
        if (scope.isActive && loading.value && iconState.value) {
            scope.cancel()
            loading.value = false
            iconState.value = false
            navController.popBackStack()
        }
        navController.navigate("HomePage")
//        Need to stop mediaPlayer but it isn't been stopped.
        scope.launch {
            delay(10000) // 10 sec's
            mediaPlayer.stop()
        }
    })
}


suspend fun loadProgress(updateProgressBar: (Float) -> Unit) {
    for (i in 1..15) {
        updateProgressBar(i.toFloat() / 15)
        delay(1000) // 1000 -> 1 sec
    }
}
