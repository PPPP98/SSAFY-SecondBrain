package com.example.secondbrain.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/**
 * 온보딩/도움말 화면
 *
 * 앱 첫 실행 시 또는 사용자가 도움말 버튼을 눌렀을 때 표시됩니다.
 */
@Composable
fun OnboardingScreen(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎤",
            style = MaterialTheme.typography.display1,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "음성 인식",
            style = MaterialTheme.typography.title2,
            color = MaterialTheme.colors.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "앱 실행 시\n자동으로 시작됩니다",
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onDismiss
        ) {
            Text("시작")
        }
    }
}