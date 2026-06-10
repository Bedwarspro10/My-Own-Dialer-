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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Call
import com.example.ui.settings.DialerSettings

// Predefined Luxury Gradients
val HighDensitySlateGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF030712), // Deep Near-Black Slate (High Density Theme)
        Color(0xFF030712)
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
        0 -> HighDensitySlateGradient
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
            val isHighDensity = settings.backgroundGradientIndex == 0
            
            val glowColor1 = if (isHighDensity) {
                Color(0xFF4F46E5).copy(alpha = 0.20f) // Indigo glow
            } else {
                settings.getAccentColor().copy(alpha = 0.25f)
            }
            
            val glowColor2 = if (isHighDensity) {
                Color(0xFF14B8A6).copy(alpha = 0.15f) // Teal/Emerald glow
            } else {
                Color(0xFF00E676).copy(alpha = 0.18f)
            }

            Box(
                modifier = Modifier
                    .size(if (isHighDensity) 380.dp else 280.dp)
                    .offset(x = if (isHighDensity) (-60).dp else (-40).dp, y = if (isHighDensity) (-40).dp else 80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                glowColor1,
                                Color.Transparent
                            )
                        )
                    )
                    .blur(if (isHighDensity) 100.dp else 60.dp)
            )

            Box(
                modifier = Modifier
                    .size(if (isHighDensity) 340.dp else 310.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 60.dp, y = if (isHighDensity) 30.dp else (-20).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                glowColor2,
                                Color.Transparent
                            )
                        )
                    )
                    .blur(if (isHighDensity) 80.dp else 70.dp)
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
    backgroundBrush: Brush? = null,
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

    val highlightColor = if (isSelected || backgroundColor != null || backgroundBrush != null) {
        Color.White.copy(alpha = 0.5f)
    } else {
        if (isLight) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.15f)
    }

    val backgroundModifier = if (backgroundBrush != null) {
        Modifier.background(brush = backgroundBrush, shape = CircleShape)
    } else {
        Modifier.background(color = baseColor, shape = CircleShape)
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
            .then(backgroundModifier)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun ContactDetailDialog(
    name: String,
    phoneNumber: String,
    label: String,
    avatarColorHex: String,
    isFavorite: Boolean,
    settings: DialerSettings,
    onDismiss: () -> Unit,
    onCall: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        val color = try {
            Color(android.graphics.Color.parseColor(avatarColorHex))
        } catch (e: Exception) {
            settings.getAccentColor()
        }
        val isLight = settings.themeMode == "LIGHT"
        
        GlassCard(
            settings = settings,
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            cornerRadiusOverride = 28.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row (Close & Favorite)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.IconButton(
                        onClick = onDismiss
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isLight) Color.Black else Color.White
                        )
                    }
                    
                    if (onToggleFavorite != null) {
                        androidx.compose.material3.IconButton(
                            onClick = onToggleFavorite
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Toggle Favorite",
                                tint = if (isFavorite) Color(0xFFFFD607) else (if (isLight) Color.LightGray else Color.Gray)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(24.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Huge Glass Profile Circle with initials
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f))
                        .border(2.dp, color.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(2).uppercase(),
                        color = color,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        fontSize = 32.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Name
                Text(
                    text = name,
                    fontSize = 24.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = if (isLight) Color.Black else Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Label Classification (pill shape)
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(settings.getAccentColor().copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = label.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = settings.getAccentColor()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Phone Number field
                Text(
                    text = phoneNumber,
                    fontSize = 18.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action: Voice Call Button with green glass background
                androidx.compose.material3.Button(
                    onClick = {
                        onDismiss()
                        onCall()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF34C759)
                    ),
                    shape = RoundedCornerShape(settings.cornerRadius.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Call Contact",
                        color = Color.White,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
