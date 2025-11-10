/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.secondbrain.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.secondbrain.R
import com.example.secondbrain.presentation.theme.SecondBrainTheme
import com.example.secondbrain.wakeword.WakeWordDetector

class MainActivity : ComponentActivity() {

    private lateinit var wakeWordDetector: WakeWordDetector

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            wakeWordDetector.startListening()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        wakeWordDetector = WakeWordDetector(this)

        setContent {
            WearApp(wakeWordDetector) {
                checkAndRequestPermission()
            }
        }
    }

    private fun checkAndRequestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                wakeWordDetector.startListening()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeWordDetector.stopListening()
    }
}

@Composable
fun WearApp(wakeWordDetector: WakeWordDetector, onStartListening: () -> Unit) {
    val wakeWordDetected by wakeWordDetector.wakeWordDetected.collectAsState()
    val recognizedText by wakeWordDetector.recognizedText.collectAsState()
    val isListening = wakeWordDetector.isCurrentlyListening()

    SecondBrainTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()

            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (wakeWordDetected) "헤이스비 감지됨!" else "웨이크워드 대기 중...",
                    style = MaterialTheme.typography.title3,
                    color = if (wakeWordDetected) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (recognizedText.isNotEmpty()) {
                    Text(
                        text = recognizedText,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onBackground,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (isListening) {
                            wakeWordDetector.stopListening()
                        } else {
                            onStartListening()
                        }
                    }
                ) {
                    Text(if (isListening) "중지" else "시작")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isListening) "🎤 듣는 중..." else "마이크 꺼짐",
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onBackground
                )
            }
        }
    }
}