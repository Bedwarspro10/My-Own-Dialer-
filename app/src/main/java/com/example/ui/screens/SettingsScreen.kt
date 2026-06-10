package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.settings.DialerSettings
import com.example.ui.viewmodel.DialerViewModel

import androidx.compose.material.icons.filled.Close

@Composable
fun SettingsScreen(
    viewModel: DialerViewModel,
    settings: DialerSettings,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Large Header Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = settings.getAccentColor(),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Settings",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (settings.themeMode == "LIGHT") Color.Black else Color.White
                )
            }

            if (onClose != null) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("settings_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = if (settings.themeMode == "LIGHT") Color.Black else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Text(
            text = "Personalize Liquid Glass Theme",
            fontSize = 15.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // 1. Theme Mode Selectors (LIGHT, DARK, AMOLED)
        Text(
            text = "Theme Mode",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = if (settings.themeMode == "LIGHT") Color.Black else Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("LIGHT", "DARK", "AMOLED").forEach { mode ->
                val isSelected = settings.themeMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) settings.getAccentColor()
                            else Color.White.copy(alpha = if (settings.themeMode == "LIGHT") 0.5f else 0.08f)
                        )
                        .border(
                            1.dp,
                            if (isSelected) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            viewModel.updateSettings(settings.copy(themeMode = mode))
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode,
                        color = if (isSelected) Color.White else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // 2. Sliders parameters (Blur strength, Glass opacity, Corner radius)
        Text(
            text = "Glass & Frost Intensity",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = if (settings.themeMode == "LIGHT") Color.Black else Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        GlassCard(
            settings = settings,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Blur Slider
                Text(
                    text = "Blur strength: ${settings.blurStrength.toInt()}%",
                    color = if (settings.themeMode == "LIGHT") Color.Black else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = settings.blurStrength,
                    onValueChange = { viewModel.updateSettings(settings.copy(blurStrength = it)) },
                    valueRange = 10f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = settings.getAccentColor(),
                        activeTrackColor = settings.getAccentColor()
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Opacity intensity Slider
                Text(
                    text = "Glass intensity/Opacity: ${(settings.glassIntensity * 100).toInt()}%",
                    color = if (settings.themeMode == "LIGHT") Color.Black else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = settings.glassIntensity,
                    onValueChange = { viewModel.updateSettings(settings.copy(glassIntensity = it)) },
                    valueRange = 0.1f..0.9f,
                    colors = SliderDefaults.colors(
                        thumbColor = settings.getAccentColor(),
                        activeTrackColor = settings.getAccentColor()
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Corner radius
                Text(
                    text = "Corner radius: ${settings.cornerRadius}dp",
                    color = if (settings.themeMode == "LIGHT") Color.Black else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = settings.cornerRadius.toFloat(),
                    onValueChange = { viewModel.updateSettings(settings.copy(cornerRadius = it.toInt())) },
                    valueRange = 8f..48f,
                    colors = SliderDefaults.colors(
                        thumbColor = settings.getAccentColor(),
                        activeTrackColor = settings.getAccentColor()
                    )
                )
            }
        }

        // 3. Accent Colors selection
        Text(
            text = "Accent Color",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = if (settings.themeMode == "LIGHT") Color.Black else Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        val colorsPalette = listOf("#0288D1", "#00B0FF", "#34C759", "#FF2D55", "#FF9500", "#5856D6")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            colorsPalette.forEach { colorStr ->
                val colorObj = Color(android.graphics.Color.parseColor(colorStr))
                val isSelected = settings.accentColorHex == colorStr
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(colorObj)
                        .border(
                            2.dp,
                            if (isSelected) Color.White else Color.Transparent,
                            CircleShape
                        )
                        .clickable { viewModel.updateSettings(settings.copy(accentColorHex = colorStr)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 4. Background Gradient index
        Text(
            text = "Gradient Wallpaper",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = if (settings.themeMode == "LIGHT") Color.Black else Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        val gradients = listOf("Slate High-Density", "Sky Pastel", "Indigo Velvet", "Cosmic AMOLED")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            gradients.forEachIndexed { index, name ->
                val isSelected = settings.backgroundGradientIndex == index
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) settings.getAccentColor().copy(alpha = 0.2f)
                            else Color.White.copy(alpha = if (settings.themeMode == "LIGHT") 0.4f else 0.05f)
                        )
                        .border(
                            1.dp,
                            if (isSelected) settings.getAccentColor() else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.updateSettings(settings.copy(backgroundGradientIndex = index)) }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            color = if (settings.themeMode == "LIGHT") Color.Black else Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        if (isSelected) {
                            Text(
                                text = "Active",
                                color = settings.getAccentColor(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 5. Stylings Options (Dial pad, navigation, contact card)
        Text(
            text = "Style Options",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = if (settings.themeMode == "LIGHT") Color.Black else Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        GlassCard(
            settings = settings,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column {
                listOf(
                    "Dial buttons" to listOf("Circular Glass", "Squircle Glass", "Borderless Minimal"),
                    "Navigation bar" to listOf("Glass Floating", "Full Width"),
                    "Contact Cards" to listOf("Large Glass Grid", "Compact Frosted")
                ).forEach { (caption, options) ->
                    Divider(color = Color.White.copy(alpha = 0.12f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Cycle through options dynamically
                                val currentVal = when (caption) {
                                    "Dial buttons" -> settings.dialPadStyle
                                    "Navigation bar" -> settings.navigationStyle
                                    else -> settings.contactCardStyle
                                }
                                val idx = options.indexOf(currentVal)
                                val nextIdx = (idx + 1) % options.size
                                val nextVal = options[nextIdx]
                                val nextSettings = when (caption) {
                                    "Dial buttons" -> settings.copy(dialPadStyle = nextVal)
                                    "Navigation bar" -> settings.copy(navigationStyle = nextVal)
                                    else -> settings.copy(contactCardStyle = nextVal)
                                }
                                viewModel.updateSettings(nextSettings)
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = caption,
                                color = if (settings.themeMode == "LIGHT") Color.Black else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            val selectionVal = when (caption) {
                                "Dial buttons" -> settings.dialPadStyle
                                "Navigation bar" -> settings.navigationStyle
                                else -> settings.contactCardStyle
                            }
                            Text(
                                text = selectionVal,
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Cycle option",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 6. Test incoming calling simulation trigger
        Text(
            text = "Testing Controls",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = if (settings.themeMode == "LIGHT") Color.Black else Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Button(
            onClick = {
                viewModel.triggerSimulatedIncomingCallDelay(seconds = 3)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("simulate_call_test_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = settings.getAccentColor()
            ),
            shape = RoundedCornerShape(settings.cornerRadius.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Smartphone,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Simulate Incoming Call (3 seconds)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}
