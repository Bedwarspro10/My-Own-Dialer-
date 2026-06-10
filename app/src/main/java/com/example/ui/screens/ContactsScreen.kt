package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
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
import com.example.ui.settings.DialerSettings
import com.example.ui.viewmodel.DialerViewModel
import kotlinx.coroutines.launch

@Composable
fun ContactsScreen(
    viewModel: DialerViewModel,
    settings: DialerSettings,
    onNavigateToCall: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val contacts by viewModel.contactsList.collectAsState()
    val searchQuery by viewModel.contactSearchQuery.collectAsState()
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Group contacts by first letter for indexing
    val groupedContacts = remember(contacts) {
        contacts.groupBy { it.name.trim().firstOrNull()?.uppercaseChar() ?: '#' }
            .toSortedMap()
    }

    // Alphabet index sidebar list
    val alphabet = ('A'..'Z').toList() + '#'

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { /* Back */ },
                modifier = Modifier.testTag("contacts_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    tint = settings.getAccentColor(),
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "Contacts",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (settings.themeMode == "LIGHT") Color.Black else Color.White
            )

            IconButton(
                onClick = { /* Show Add New Contact Popup/Form */ },
                modifier = Modifier.testTag("add_contact_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Contact",
                    tint = settings.getAccentColor(),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Large search input layout
        GlassCard(
            settings = settings,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("contacts_search_bar"),
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
                    contentDescription = "Search",
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
                    BasicTextFieldDummy(
                        value = searchQuery,
                        onValueChange = { viewModel.setContactSearchQuery(it) },
                        textColor = if (settings.themeMode == "LIGHT") Color.Black else Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f)
        ) {
            // Contacts list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedContacts.forEach { (initial, contactsForInitial) ->
                    item {
                        Text(
                            text = initial.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(contactsForInitial) { contact ->
                        ContactRow(
                            contact = contact,
                            settings = settings,
                            onClick = {
                                onNavigateToCall(contact.phoneNumber, contact.name)
                            }
                        )
                    }
                }
            }

            // Alphabet Index Scrollbar on the right (matching provided photos!)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(24.dp)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround
            ) {
                alphabet.forEach { char ->
                    val isPresent = groupedContacts.containsKey(char)
                    Text(
                        text = char.toString(),
                        color = if (isPresent) settings.getAccentColor() else Color.Gray.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isPresent) {
                                val keys = groupedContacts.keys.toList()
                                val indexOfSection = keys.indexOf(char)
                                if (indexOfSection >= 0) {
                                    coroutineScope.launch {
                                        // Calculate exact model list index
                                        var targetIdx = 0
                                        for (i in 0 until indexOfSection) {
                                            val key = keys[i]
                                            targetIdx += 1 + (groupedContacts[key]?.size ?: 0)
                                        }
                                        listState.animateScrollToItem(targetIdx)
                                    }
                                }
                            }
                            .padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ContactRow(
    contact: ContactEntity,
    settings: DialerSettings,
    onClick: () -> Unit
) {
    val color = Color(android.graphics.Color.parseColor(contact.avatarColorHex))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // High density visual initials avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
                .border(1.dp, color.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.name.take(2).uppercase(),
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1.0f)) {
            Text(
                text = contact.name,
                color = if (settings.themeMode == "LIGHT") Color.Black else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${contact.phoneNumber} • ${contact.label}",
                color = Color.Gray,
                fontSize = 13.sp
            )
        }

        if (contact.isFavorite) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Favorite",
                tint = Color(0xFFFFD607),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
