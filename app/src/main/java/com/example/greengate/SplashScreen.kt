package com.example.greengate

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.greengate.ui.theme.GreenGateTheme
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    displayMillis: Long = 2500L
) {
    var visible by remember { mutableStateOf(true) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "splashAlpha"
    )

    LaunchedEffect(Unit) {
        delay(displayMillis)
        visible = false
        delay(450)
        onFinished()
    }

    Image(
        painter = painterResource(R.drawable.splash_bg),
        contentDescription = "Welcome. Your community, now smarter.",
        modifier = Modifier.fillMaxSize().alpha(alpha),
        alignment = Alignment.CenterStart,
        contentScale = ContentScale.Crop
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun SplashScreenPreview() {
    GreenGateTheme {
        SplashScreen(onFinished = {})
    }
}
