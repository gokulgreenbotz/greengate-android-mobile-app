package com.greenbotz.greengate.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenbotz.greengate.R
import com.greenbotz.greengate.ui.theme.DMSans

@Composable
internal fun WhiteLoginBranding(modifier: Modifier = Modifier) {
    Column(
        modifier.widthIn(max = 320.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painterResource(R.drawable.greengate_white_brand_lockup),
            contentDescription = "GreenGate Condo",
            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 2f)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "A SMARTER\nWAY OF LIVING", color = Muted, fontFamily = DMSans,
            fontSize = 11.sp, letterSpacing = 3.sp, lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        Box(Modifier.width(72.dp).height(4.dp).clip(CircleShape).background(Color(0xFFE5E3DF))) {
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(.45f).clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(Color(0xFFE7B86D), Color(0xFFF5D297))))
            )
        }
    }
}
