package com.nalansitan.chinesepoetry.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.nalansitan.chinesepoetry.R
import com.nalansitan.chinesepoetry.presentation.ui.theme.PoetryColors
import com.nalansitan.chinesepoetry.presentation.ui.theme.PoetryTheme

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Poetry)
        super.onCreate(savedInstanceState)
        setContent {
            PoetryTheme {
                SplashScreen(
                    onFinished = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher),
                contentDescription = "古诗词",
                modifier = Modifier.size(88.dp)
            )
            Text(
                text = "古诗词",
                style = MaterialTheme.typography.headlineMedium,
                color = PoetryColors.InkBlack,
                modifier = Modifier.padding(top = 20.dp)
            )
        }

        Text(
            text = "作者：纳兰斯坦、爱因容若",
            style = MaterialTheme.typography.labelSmall,
            color = PoetryColors.MediumGray,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
        )
    }
}
