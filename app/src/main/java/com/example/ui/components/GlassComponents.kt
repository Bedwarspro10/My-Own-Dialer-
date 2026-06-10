package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.settings.DialerSettings

// Predefined Luxury Gradients
val AuroraDarkGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF04373B), // Deep Teal
        Color(0xFF023136), 
        Color(0xFF012028), // Dark Ocean
        Color(0xFF00121A)
    )
)

val SkyGlassGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFE3F2FD), // Ice Blue
        Color(0xFFE8F5E9), // Pale Green
        Color(0xFFF3E5F5)  // Light Amethyst
    )
)

val VelvetPurpleGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1E0A2D), // Amethyst
        Color(0xFF14071F), 
        Color(0xFF09030E), // Obsidian Orchid
        Color(0xFF020104)
    )
)

val AmoledCosmicGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF000000), // Perfect Black
        Color(0xFF0A0A0A),
        Color(0xFF121212)
    )
)

@Composable
fun GlassBackground(
    settings: DialerSettings,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val brush = when (settings.backgroundGradientIndex) {
        0 -> AuroraDarkGradient
        1 -> SkyGlassGradient
        2 -> VelvetPurpleGradient
        else -> AmoledCosmicGradient
    }

    val isAmoled = settings.themeMode == "AMOLED"
    val finalBrush = if (isAmoled) AmoledCosmicGradient else brush

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(finalBrush)
    ) {
        // Overlay some abstract blurry glass color spots in non-AMOLED modes for extra richness!
        if (settings.themeMode != "AMOLED") {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .offset(x = (-40).dp, y = 80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                settings.getAccentColor().copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
                    .blur(60.dp)
            )

            Box(
                modifier = Modifier
                    .size(310.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 60.dp, y = (-20).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00E676).copy(alpha = 0.18f), // Soft Emerald
                                Color.Transparent
                            )
                        )
                    )
                    .blur(70.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            content = content
        )
    }
}

@Composable
fun GlassCard(
    settings: DialerSettings,
    modifier: Modifier = Modifier,
    cornerRadiusOverride: Dp? = null,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val radius = cornerRadiusOverride ?: settings.cornerRadius.dp
    val isLight = settings.themeMode == "LIGHT"
    
    // Choose backing color based on light/dark mode and glass intensity Config
    val baseColor = if (isLight) {
        Color.White.copy(alpha = settings.glassIntensity * 1.2f)
    } else {
        if (settings.themeMode == "AMOLED") {
            Color(0xFF141414).copy(alpha = settings.glassIntensity * 0.8f)
        } else {
            Color(0xFF263238).copy(alpha = settings.glassIntensity * 0.6f)
        }
    }

    // Border opacity (Specular EdgeHighlight)
    val highlightColor = if (isLight) {
        Color.White.copy(alpha = (settings.glassIntensity + 0.3f).coerceAtMost(0.95f))
    } else {
        Color.White.copy(alpha = (settings.glassIntensity * 0.4f).coerceAtMost(0.85f))
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (settings.themeMode == "AMOLED") 0.dp else 4.dp,
                shape = RoundedCornerShape(radius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color.Black.copy(alpha = 0.4f)
            )
            .border(
                width = borderWidth,
                color = highlightColor,
                shape = RoundedCornerShape(radius)
            )
            .background(
                color = baseColor,
                shape = RoundedCornerShape(radius)
            ),
        content = content
    )
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    settings: DialerSettings,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val radius = settings.cornerRadius.dp
    val isLight = settings.themeMode == "LIGHT"

    val baseColor = when {
        isSelected -> settings.getAccentColor().copy(alpha = 0.85f)
        isLight -> Color.White.copy(alpha = settings.glassIntensity * 1.1f)
        settings.themeMode == "AMOLED" -> Color(0xFF1E1E1E).copy(alpha = settings.glassIntensity * 0.9f)
        else -> Color(0xFF37474F).copy(alpha = settings.glassIntensity * 0.7f)
    }

    val highlightColor = if (isSelected) {
        Color.White.copy(alpha = 0.6f)
    } else {
        if (isLight) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.25f)
    }

    Box(
        modifier = modifier
            .size(56.dp)
            .shadow(
                elevation = if (isSelected) 6.dp else 2.dp,
                shape = RoundedCornerShape(radius),
                clip = false
            )
            .border(
                width = 1.dp,
                color = highlightColor,
                shape = RoundedCornerShape(radius)
            )
            .background(
                color = baseColor,
                shape = RoundedCornerShape(radius)
            )
            .clip(RoundedCornerShape(radius))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun GlassCircleButton(
    onClick: () -> Unit,
    settings: DialerSettings,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    backgroundColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    // Large 72dp Circular button used for keys
    val isLight = settings.themeMode == "LIGHT"

    val baseColor = when {
        backgroundColor != null -> backgroundColor
        isSelected -> settings.getAccentColor().copy(alpha = 0.85f)
        isLight -> Color.White.copy(alpha = settings.glassIntensity * 1.3f)
        settings.themeMode == "AMOLED" -> Color(0xFF1C1C1E).copy(alpha = 0.85f)
        else -> Color.White.copy(alpha = 0.15f) // matches standard transparent keyboard feel!
    }

    val highlightColor = if (isSelected || backgroundColor != null) {
        Color.White.copy(alpha = 0.5f)
    } else {
        if (isLight) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.15f)
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isSelected) 4.dp else 1.dp,
                shape = CircleShape,
                clip = false
            )
            .border(
                width = 1.dp,
                color = highlightColor,
                shape = CircleShape
            )
            .background(
                color = baseColor,
                shape = CircleShape
            )
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}
