package com.example.ui.screens

import android.util.Log
import java.util.Locale
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Voicemail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.settings.DialerSettings
import com.example.ui.viewmodel.AppCallState
import com.example.ui.viewmodel.DialerViewModel
import kotlin.math.roundToInt

@Composable
fun IncomingCallScreen(
    state: AppCallState.Incoming,
    viewModel: DialerViewModel,
    settings: DialerSettings,
    modifier: Modifier = Modifier
) {
    // Left-to-right swipe drawer to answer simulation
    var slideOffset by remember { mutableStateOf(0f) }
    val maxSlideDistanceDp = 220.dp
    val maxSlideDistancePx = with(LocalDensity.current) { maxSlideDistanceDp.toPx() }

    // Pulsing text scale animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Full screen layout with rich glassy blur background
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030712),
                        Color(0xFF0B1229),
                        Color(0xFF030712)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Caller Information Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.label,
                color = Color.LightGray.copy(alpha = 0.8f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.name,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 38.sp
            )
        }

        // 2. Action Buttons: Message, Voicemail
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Message button (translucent outline style from photo)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .clickable { viewModel.hangUpCall() /* simulates sending quick content */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Message,
                            contentDescription = "Message",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Message", color = Color.White, fontSize = 12.sp)
                }

                // Voicemail button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .clickable { viewModel.hangUpCall() /* send to voicemail */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Voicemail,
                            contentDescription = "Voicemail",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Voicemail", color = Color.White, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            val isLocked by viewModel.isLockedSimulation.collectAsState()

            if (isLocked) {
                // 3. Sliding Track answer layout (capsule track from photo)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .clip(RoundedCornerShape(34.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(34.dp)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // slide guidance pulsing label
                    Text(
                        text = "slide to answer",
                        color = Color.White.copy(alpha = textAlpha),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    // Draggable phone handle
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(slideOffset.roundToInt(), 0) }
                            .size(68.dp)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .draggable(
                                orientation = Orientation.Horizontal,
                                state = rememberDraggableState { delta ->
                                    slideOffset = (slideOffset + delta).coerceIn(0f, maxSlideDistancePx)
                                },
                                onDragStopped = {
                                    if (slideOffset > maxSlideDistancePx * 0.7f) {
                                        viewModel.answerIncomingCall()
                                    } else {
                                        slideOffset = 0f // Bounce back
                                    }
                                }
                            )
                            .testTag("slide_answer_handle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Answer Call",
                            tint = Color(0xFF34C759), // Active call Green
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            } else {
                // Unlocked Mode: Side-by-side dual slide to answer & decline!
                var declineSlideOffset by remember { mutableStateOf(0f) }
                var answerSlideOffset by remember { mutableStateOf(0f) }
                
                val dualMaxDistanceDp = 70.dp
                val dualMaxDistancePx = with(LocalDensity.current) { dualMaxDistanceDp.toPx() }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Decline Slider (Left)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(Color(0xFF3C1818).copy(alpha = 0.6f))
                            .border(1.dp, Color(0xFFFF3B30).copy(alpha = 0.3f), RoundedCornerShape(30.dp)),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "decline",
                            color = Color(0xFFFF8282).copy(alpha = textAlpha),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth().padding(start = 44.dp),
                            textAlign = TextAlign.Center
                        )

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(declineSlideOffset.roundToInt(), 0) }
                                .size(60.dp)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30))
                                .draggable(
                                    orientation = Orientation.Horizontal,
                                    state = rememberDraggableState { delta ->
                                        declineSlideOffset = (declineSlideOffset + delta).coerceIn(0f, dualMaxDistancePx)
                                    },
                                    onDragStopped = {
                                        if (declineSlideOffset > dualMaxDistancePx * 0.7f) {
                                            viewModel.hangUpCall()
                                        } else {
                                            declineSlideOffset = 0f
                                        }
                                    }
                                )
                                .testTag("dual_slide_decline"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Decline Call",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Answer Slider (Right)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(Color(0xFF143322).copy(alpha = 0.6f))
                            .border(1.dp, Color(0xFF34C759).copy(alpha = 0.3f), RoundedCornerShape(30.dp)),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "answer",
                            color = Color(0xFF8FFFA8).copy(alpha = textAlpha),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth().padding(start = 44.dp),
                            textAlign = TextAlign.Center
                        )

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(answerSlideOffset.roundToInt(), 0) }
                                .size(60.dp)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34C759))
                                .draggable(
                                    orientation = Orientation.Horizontal,
                                    state = rememberDraggableState { delta ->
                                        answerSlideOffset = (answerSlideOffset + delta).coerceIn(0f, dualMaxDistancePx)
                                    },
                                    onDragStopped = {
                                        if (answerSlideOffset > dualMaxDistancePx * 0.7f) {
                                            viewModel.answerIncomingCall()
                                        } else {
                                            answerSlideOffset = 0f
                                        }
                                    }
                                )
                                .testTag("dual_slide_answer"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Answer Call",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OngoingCallScreen(
    state: AppCallState.Ongoing,
    viewModel: DialerViewModel,
    settings: DialerSettings,
    modifier: Modifier = Modifier
) {
    val durationSeconds by viewModel.callTimerSeconds.collectAsState()
    val isMuted by viewModel.isCallMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val isFaceTimeActive by viewModel.isFaceTimeActive.collectAsState()
    
    var showDtmfKeypad by remember { mutableStateOf(false) }

    val formattedDuration = remember(durationSeconds) {
        val mins = durationSeconds / 60
        val secs = durationSeconds % 60
        String.format("%02d:%02d", mins, secs)
    }

    val toneGenerator = remember {
        try {
            android.media.ToneGenerator(android.media.AudioManager.STREAM_VOICE_CALL, 80)
        } catch (e: Exception) {
            null
        }
    }

    fun playDtmfTone(char: Char) {
        val dtmfType = when (char) {
            '1' -> android.media.ToneGenerator.TONE_DTMF_1
            '2' -> android.media.ToneGenerator.TONE_DTMF_2
            '3' -> android.media.ToneGenerator.TONE_DTMF_3
            '4' -> android.media.ToneGenerator.TONE_DTMF_4
            '5' -> android.media.ToneGenerator.TONE_DTMF_5
            '6' -> android.media.ToneGenerator.TONE_DTMF_6
            '7' -> android.media.ToneGenerator.TONE_DTMF_7
            '8' -> android.media.ToneGenerator.TONE_DTMF_8
            '9' -> android.media.ToneGenerator.TONE_DTMF_9
            '0' -> android.media.ToneGenerator.TONE_DTMF_0
            '*' -> android.media.ToneGenerator.TONE_DTMF_S
            '#' -> android.media.ToneGenerator.TONE_DTMF_P
            else -> return
        }
        try {
            toneGenerator?.startTone(dtmfType, 120)
            com.example.telecom.TelecomCallManager.playDtmf(char)
        } catch (e: Exception) {
            Log.e("OngoingCallScreen", "Error playing DTMF", e)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030712),
                        Color(0xFF0F172A),
                        Color(0xFF030712)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Dynamic duration and caller title header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formattedDuration,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = state.name,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // 2. Central Area: Toggleable DTMF dialer keypad or security notice
        if (showDtmfKeypad) {
            // Centered fully functional DTMF dialpad layout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (r in 0..3) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            for (c in 0..2) {
                                val key = keys[r * 3 + c]
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .clickable { playDtmfTone(key[0]) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Elegant centered logo and status presentation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(44.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0F1E26))
                        .border(1.dp, Color(0xFF142B36), RoundedCornerShape(18.dp))
                        .padding(horizontal = 20.dp, vertical = 11.dp)
                ) {
                    Text(
                        text = "Carrier voice line active • Secure dynamic connection",
                        color = Color(0xFF63B8F2),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 3. Circular Action Keys (3x2 grid matching photos + floating End Call)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val keysRow1 = listOf(
                Triple("Speaker", Icons.Default.VolumeUp, isSpeakerOn),
                Triple("FaceTime", Icons.Default.Videocam, isFaceTimeActive),
                Triple("Mute", Icons.Default.MicNone, isMuted)
            )

            val keysRow2 = listOf(
                Triple("More", Icons.Default.MoreHoriz, false),
                Triple("End", Icons.Default.CallEnd, false), // Rotated call under progress
                Triple("Keypad", Icons.Default.Dialpad, showDtmfKeypad)
            )

            // Dynamic grid construction
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                keysRow1.forEach { (label, icon, active) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ActionButtonCircle(
                            icon = icon,
                            isActive = active,
                            onClick = {
                                when (label) {
                                    "Speaker" -> viewModel.toggleSpeaker()
                                    "FaceTime" -> viewModel.toggleFaceTime()
                                    "Mute" -> viewModel.toggleMute()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                keysRow2.forEach { (label, icon, active) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val isEnd = label == "End"
                        ActionButtonCircle(
                            icon = icon,
                            isActive = active,
                            isEndButton = isEnd,
                            onClick = {
                                if (isEnd) {
                                    viewModel.hangUpCall()
                                } else if (label == "Keypad") {
                                    showDtmfKeypad = !showDtmfKeypad
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButtonCircle(
    icon: ImageVector,
    isActive: Boolean,
    isEndButton: Boolean = false,
    onClick: () -> Unit
) {
    val brushColor = when {
        isEndButton -> Color(0xFFFF3B30) // Red end button
        isActive -> Color.White // Filled white speaker
        else -> Color.White.copy(alpha = 0.12f)
    }

    val iconColor = when {
        isEndButton -> Color.White
        isActive -> Color.Black
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(brushColor)
            .border(
                width = if (isActive || isEndButton) 0.dp else 1.dp,
                color = Color.White.copy(alpha = 0.22f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .testTag(if (isEndButton) "end_call_button" else "action_circle"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun HeadsUpCallPopup(
    callState: AppCallState,
    viewModel: DialerViewModel,
    settings: DialerSettings,
    modifier: Modifier = Modifier
) {
    val durationSeconds by viewModel.callTimerSeconds.collectAsState(initial = 0)
    val isMuted by viewModel.isCallMuted.collectAsState(initial = false)
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState(initial = false)

    val formattedDuration = remember(durationSeconds) {
        val mins = durationSeconds / 60
        val secs = durationSeconds % 60
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    GlassCard(
        settings = settings,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable {
                viewModel.showHeadsUpCallPopup.value = false
            },
        cornerRadiusOverride = 24.dp,
        borderWidth = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            when (callState) {
                is AppCallState.Incoming -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.0f)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(settings.getAccentColor().copy(alpha = 0.15f))
                                    .border(1.dp, settings.getAccentColor().copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = callState.name.take(2).uppercase(),
                                    color = settings.getAccentColor(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = callState.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Incoming Call (${callState.number})",
                                    color = Color.LightGray.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3B30))
                                    .clickable { viewModel.hangUpCall() }
                                    .testTag("headsup_decline_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = "Decline",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34C759))
                                    .clickable { viewModel.answerIncomingCall() }
                                    .testTag("headsup_answer_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Answer",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                is AppCallState.Ongoing -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.1f))
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = null,
                                        tint = Color(0xFF34C759),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = callState.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Ongoing Call • $formattedDuration",
                                        color = Color(0xFF34C759),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3B30))
                                    .clickable { viewModel.hangUpCall() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = "End",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { viewModel.toggleMute() }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isMuted) Color.White else Color.White.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MicOff,
                                        contentDescription = "Mute",
                                        tint = if (isMuted) Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isMuted) "Muted" else "Mute",
                                    color = Color.LightGray,
                                    fontSize = 10.sp
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { viewModel.toggleSpeaker() }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSpeakerOn) Color.White else Color.White.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Speaker",
                                        tint = if (isSpeakerOn) Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isSpeakerOn) "Speaker On" else "Speaker",
                                    color = Color.LightGray,
                                    fontSize = 10.sp
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { viewModel.showHeadsUpCallPopup.value = false }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = "Expand",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Expand",
                                    color = Color.LightGray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}
