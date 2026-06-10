package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.ContactEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassCircleButton
import com.example.ui.settings.DialerSettings
import com.example.ui.viewmodel.DialerViewModel

@Composable
fun KeypadScreen(
    viewModel: DialerViewModel,
    settings: DialerSettings,
    onNavigateToCall: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val digits by viewModel.enteredDigits.collectAsState()
    val t9Suggestions by viewModel.t9Suggestions.collectAsState()

    // Find the prime suggestion matching current typed digits
    val mainSuggestion = remember(t9Suggestions, digits) {
        if (digits.isEmpty()) null else t9Suggestions.firstOrNull()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Digits entry display area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Main digits text
            Text(
                text = formatDigits(digits),
                fontSize = if (digits.length > 10) 30.sp else 42.sp,
                fontWeight = FontWeight.Light,
                color = if (settings.themeMode == "LIGHT") Color.Black else Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("entered_digits_display"),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle capsule matching contact suggestion literally from screenshot: "Jenny T... (555) 867-5309"
            AnimatedVisibility(
                visible = digits.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (mainSuggestion != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                if (settings.themeMode == "LIGHT") Color.Black.copy(alpha = 0.05f) 
                                else Color.White.copy(alpha = 0.1f)
                            )
                            .clickable {
                                // Direct dial
                                onNavigateToCall(mainSuggestion.phoneNumber, mainSuggestion.name)
                            }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = settings.getAccentColor(),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${mainSuggestion.name} (${mainSuggestion.phoneNumber})",
                                color = if (settings.themeMode == "LIGHT") Color.DarkGray else Color.LightGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Outlined.PersonAdd,
                                contentDescription = "Add suggested",
                                tint = settings.getAccentColor(),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else if (digits.isNotEmpty()) {
                    // Option to add as a new contact
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                if (settings.themeMode == "LIGHT") Color.Black.copy(alpha = 0.05f) 
                                else Color.White.copy(alpha = 0.1f)
                            )
                            .clickable { /* Trigger new contact flow */ }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.PersonAdd,
                                contentDescription = "Add Contact",
                                tint = settings.getAccentColor(),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Create New Contact",
                                color = settings.getAccentColor(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // T9 Matching Suggestions List overlay
        AnimatedVisibility(
            visible = digits.isNotEmpty() && t9Suggestions.size > 1,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 110.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.15f))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Skip the first one which is already shown in the main suggestion capsule!
                items(t9Suggestions.drop(1)) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onNavigateToCall(contact.phoneNumber, contact.name)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = contact.name, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text(text = contact.phoneNumber, color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 12 Spaced keys area from screenshot
        val keys = listOf(
            Triple("1", "", ""),
            Triple("2", "A B C", "ABC"),
            Triple("3", "D E F", "DEF"),
            Triple("4", "G H I", "GHI"),
            Triple("5", "J K L", "JKL"),
            Triple("6", "M N O", "MNO"),
            Triple("7", "P Q R S", "PQRS"),
            Triple("8", "T U V", "TUV"),
            Triple("9", "W X Y Z", "WXYZ"),
            Triple("*", "", ""),
            Triple("0", "+", "+"),
            Triple("#", "", "")
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            for (row in 0..3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (col in 0..2) {
                        val index = row * 3 + col
                        if (index < keys.size) {
                            val keyData = keys[index]
                            KeyButton(
                                number = keyData.first,
                                letters = keyData.second,
                                settings = settings,
                                onClick = {
                                    viewModel.appendDigit(keyData.first[0])
                                }
                            )
                        }
                    }
                }
            }

            // Bottom call bar row (Green call circle & backspace)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Invisible spacer for visual symmetry aligning call icon to center
                Box(modifier = Modifier.size(72.dp))

                // Huge circular Green call button matching design exactly
                GlassCircleButton(
                    onClick = {
                        if (digits.isNotEmpty()) {
                            viewModel.startCall(digits)
                        }
                    },
                    settings = settings,
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("dialpad_call_button"),
                    backgroundColor = Color(0xFF34C759) // Apple Green Call
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Dial call",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Delete Backspace glass icon
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (digits.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.removeDigit() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = "Delete backspace",
                                tint = if (settings.themeMode == "LIGHT") Color.Gray else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun KeyButton(
    number: String,
    letters: String,
    settings: DialerSettings,
    onClick: () -> Unit
) {
    val isLight = settings.themeMode == "LIGHT"
    
    // Choose font sizing based on spacing requirements
    val numberSize = if (number == "*" || number == "#") 38.sp else 30.sp
    
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                if (isLight) Color.Black.copy(alpha = 0.05f)
                else Color.White.copy(alpha = 0.08f)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = number,
                color = if (isLight) Color.Black else Color.White,
                fontSize = numberSize,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )
            if (letters.isNotEmpty()) {
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = letters,
                    color = if (isLight) Color.Gray else Color.LightGray.copy(alpha = 0.62f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// Format digits intelligently like a normal phone dialer with dashes
fun formatDigits(rawDigits: String): String {
    val clean = rawDigits.replace("-", "")
    if (clean.length == 7) {
        return "${clean.substring(0, 3)}-${clean.substring(3, 7)}"
    } else if (clean.length == 10) {
        return "(${clean.substring(0, 3)}) ${clean.substring(3, 6)}-${clean.substring(6, 10)}"
    }
    return rawDigits
}
