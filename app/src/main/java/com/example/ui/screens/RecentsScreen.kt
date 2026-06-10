package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.CallLogEntity
import com.example.data.database.ContactEntity
import com.example.ui.components.GlassCard
import com.example.ui.settings.DialerSettings
import com.example.ui.viewmodel.DialerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecentsScreen(
    viewModel: DialerViewModel,
    settings: DialerSettings,
    onNavigateToCall: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val callLogs by viewModel.callLogsList.collectAsState()
    val filter by viewModel.recentsFilter.collectAsState()
    val favorites by viewModel.favoriteContacts.collectAsState()
    val contacts by viewModel.contactsList.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var activeDetailContact by remember { mutableStateOf<ContactEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Large title & Top filter Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Edit",
                color = settings.getAccentColor(),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { /* Edit contact / call logs action */ }
                    .testTag("edit_logs_button")
            )

            // Segmented controller (All, Missed) inside glass layout
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White.copy(alpha = if (settings.themeMode == "LIGHT") 0.6f else 0.12f))
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("All", "Missed").forEach { type ->
                    val isSelected = filter == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                if (isSelected) {
                                    if (settings.themeMode == "LIGHT") Color.White else Color.White.copy(alpha = 0.25f)
                                } else Color.Transparent
                            )
                            .clickable { viewModel.setRecentsFilter(type) }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = type,
                            color = if (isSelected) {
                                if (settings.themeMode == "LIGHT") Color.Black else Color.White
                            } else {
                                Color.Gray
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            var menuExpanded by remember { mutableStateOf(false) }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("sort_logs_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu options",
                        tint = if (settings.themeMode == "LIGHT") Color.Black else Color.White
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(Color(0xFF1E293B)),
                    scrollState = rememberScrollState()
                ) {
                    DropdownMenuItem(
                        text = { Text("Settings Theme & Customize", color = Color.White, fontSize = 14.sp) },
                        onClick = {
                            menuExpanded = false
                            viewModel.isSettingsOpen.value = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Simulate Inbound Call (Locked)", color = Color.White, fontSize = 14.sp) },
                        onClick = {
                            menuExpanded = false
                            viewModel.isLockedSimulation.value = true
                            viewModel.triggerSimulatedIncomingCallDelay(seconds = 2, isHeadsUp = false)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Simulate Inbound Call (Unlocked)", color = Color.White, fontSize = 14.sp) },
                        onClick = {
                            menuExpanded = false
                            viewModel.isLockedSimulation.value = false
                            viewModel.triggerSimulatedIncomingCallDelay(seconds = 2, isHeadsUp = false)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Simulate Heads-Up Overlay", color = Color.White, fontSize = 14.sp) },
                        onClick = {
                            menuExpanded = false
                            viewModel.triggerSimulatedIncomingCallDelay(seconds = 2, isHeadsUp = true)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Recents",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = if (settings.themeMode == "LIGHT") Color.Black else Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Custom Glass Search Bar
        GlassCard(
            settings = settings,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("recents_search_bar"),
            cornerRadiusOverride = 16.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search icon",
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.weight(1.0f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(text = "Search", color = Color.Gray, fontSize = 16.sp)
                    }
                    // Simple input for mockup purposes
                    BasicTextFieldDummy(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textColor = if (settings.themeMode == "LIGHT") Color.Black else Color.White
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = "Voice search",
                    tint = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filters callLogs locally if search is active
        val filteredLogs = if (searchQuery.isEmpty()) {
            callLogs
        } else {
            callLogs.filter {
                (it.callerName ?: "").contains(searchQuery, ignoreCase = true) ||
                        it.phoneNumber.contains(searchQuery)
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            // Favorites Carousel Story Section (only if "All" is active and there is no search filter query)
            if (filter == "All" && searchQuery.isEmpty() && favorites.isNotEmpty()) {
                item {
                    Text(
                        text = "Favorites",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (settings.themeMode == "LIGHT") Color.Gray else Color.LightGray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        items(favorites) { contact ->
                            FavoriteCard(contact = contact, settings = settings, onClick = {
                                viewModel.startCall(contact.phoneNumber, contact.name)
                            })
                        }
                    }

                    Text(
                        text = "Call History",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (settings.themeMode == "LIGHT") Color.Gray else Color.LightGray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            if (filteredLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "Empty",
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "No call logs yet", color = Color.Gray)
                        }
                    }
                }
            } else {
                items(filteredLogs, key = { it.id }) { log ->
                    CallLogItem(
                        log = log,
                        settings = settings,
                        onDial = {
                            viewModel.startCall(log.phoneNumber, log.callerName)
                        },
                        onInfo = {
                            val match = contacts.firstOrNull { it.phoneNumber.replace(" ", "") == log.phoneNumber.replace(" ", "") }
                            activeDetailContact = match ?: ContactEntity(
                                name = log.callerName ?: log.phoneNumber,
                                phoneNumber = log.phoneNumber,
                                label = log.label,
                                avatarColorHex = "#7A8AFF",
                                isFavorite = false
                            )
                        }
                    )
                }
            }
        }
    }

    activeDetailContact?.let { contact ->
        com.example.ui.components.ContactDetailDialog(
            name = contact.name,
            phoneNumber = contact.phoneNumber,
            label = contact.label,
            avatarColorHex = contact.avatarColorHex,
            isFavorite = contact.isFavorite,
            settings = settings,
            onDismiss = { activeDetailContact = null },
            onCall = { onNavigateToCall(contact.phoneNumber, contact.name) },
            onToggleFavorite = if (contact.id != 0) {
                { viewModel.toggleContactFavorite(contact) }
            } else null
        )
    }
}

@Composable
fun FavoriteCard(
    contact: ContactEntity,
    settings: DialerSettings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Round square shape card as requested in the "Calls" screenshot on the right
    val cardRadius = settings.cornerRadius.dp
    val color = Color(android.graphics.Color.parseColor(contact.avatarColorHex))

    GlassCard(
        settings = settings,
        modifier = modifier
            .width(82.dp)
            .height(115.dp)
            .clickable(onClick = onClick),
        cornerRadiusOverride = cardRadius
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Profile circular container
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(settings.cornerRadius.dp / 2))
                    .background(color.copy(alpha = 0.25f))
                    .border(1.dp, color, RoundedCornerShape(settings.cornerRadius.dp / 2)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.take(2).uppercase(),
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Text(
                text = contact.name,
                color = if (settings.themeMode == "LIGHT") Color.Black else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = settings.getAccentColor(),
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = contact.label,
                    color = Color.Gray,
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun CallLogItem(
    log: CallLogEntity,
    settings: DialerSettings,
    onDial: () -> Unit,
    onInfo: () -> Unit
) {
    val isLight = settings.themeMode == "LIGHT"
    val isMissed = log.callType == "MISSED"
    
    val nameColor = if (isMissed) {
        Color(0xFFFF3B30) // Red missed call name
    } else {
        if (isLight) Color.Black else Color.White
    }

    val typeIcon = when (log.callType) {
        "OUTGOING" -> Icons.Default.CallMade
        "MISSED" -> Icons.Default.CallMissed
        else -> Icons.Default.CallReceived
    }

    val iconColor = when (log.callType) {
        "OUTGOING" -> settings.getAccentColor()
        "MISSED" -> Color(0xFFFF3B30)
        else -> Color(0xFF34C759) // Green incoming
    }

    val formattedDate = remember(log.timestamp) {
        val formatter = SimpleDateFormat("EEE hh:mm a", Locale.getDefault())
        // If yesterday or today
        val curTime = System.currentTimeMillis()
        val diff = curTime - log.timestamp
        if (diff < 24 * 3600 * 1000L) {
            val hourFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
            "Today " + hourFormatter.format(Date(log.timestamp))
        } else if (diff < 48 * 3600 * 1000L) {
            "Yesterday"
        } else {
            formatter.format(Date(log.timestamp))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDial)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon style building or avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (isMissed) Color(0xFFFF3B30).copy(alpha = 0.15f)
                    else settings.getAccentColor().copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (log.callerName?.lowercase()?.contains("walgreens") == true || log.callerName?.lowercase()?.contains("center") == true) Icons.Default.Business else Icons.Default.Person,
                contentDescription = null,
                tint = if (isMissed) Color(0xFFFF3B30) else settings.getAccentColor(),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1.0f)) {
            Text(
                text = log.callerName ?: log.phoneNumber,
                color = nameColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = log.label.lowercase(),
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = formattedDate,
                color = Color.Gray,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onInfo,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Details",
                    tint = settings.getAccentColor(),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun BasicTextFieldDummy(
    value: String,
    onValueChange: (String) -> Unit,
    textColor: Color
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = textColor,
            fontSize = 16.sp
        ),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}
