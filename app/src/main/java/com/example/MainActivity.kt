package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.screens.*
import com.example.ui.settings.DialerSettings
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppCallState
import com.example.ui.viewmodel.DialerViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: DialerViewModel

    // Request permissions launcher
    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val contactsGranted = results[android.Manifest.permission.READ_CONTACTS] ?: false
        val logsGranted = results[android.Manifest.permission.READ_CALL_LOG] ?: false
        
        if (contactsGranted || logsGranted) {
            viewModel.syncSystemData()
        }
    }

    override fun onResume() {
        super.onResume()
        checkAppPermissionsAndStatus()
    }

    private fun checkAppPermissionsAndStatus() {
        if (!::viewModel.isInitialized) return
        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val isDefault = telecomManager?.defaultDialerPackage == packageName
        viewModel.isDefaultDialer.value = isDefault

        val contactsGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val logsGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        viewModel.arePermissionsGranted.value = contactsGranted && logsGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[DialerViewModel::class.java]

        // Handle dialer tel scheme incoming intent
        handleDialerIntent(intent)

        setContent {
            MyApplicationTheme {
                val settings by viewModel.uiSettings.collectAsState()
                
                // Root Container running gorgeous blurred backgrounds
                GlassBackground(
                    settings = settings,
                    modifier = Modifier.fillMaxSize()
                ) {
                    val callState by viewModel.callState.collectAsState()
                    val showHeadsUpCallPopup by viewModel.showHeadsUpCallPopup.collectAsState()

                    // Visual switching between Idle (standard tabs) and call states
                    if (callState != AppCallState.Idle && !showHeadsUpCallPopup) {
                        Crossfade(
                            targetState = callState,
                            label = "call_state_transition"
                        ) { activeCall ->
                            when (activeCall) {
                                is AppCallState.Incoming -> {
                                    IncomingCallScreen(
                                        state = activeCall,
                                        viewModel = viewModel,
                                        settings = settings
                                    )
                                }
                                is AppCallState.Ongoing -> {
                                    OngoingCallScreen(
                                        state = activeCall,
                                        viewModel = viewModel,
                                        settings = settings
                                    )
                                }
                                AppCallState.Idle -> {}
                            }
                        }
                    } else {
                        // Standard app screen views
                        Box(modifier = Modifier.fillMaxSize()) {
                            DialerMainScaffold(
                                viewModel = viewModel,
                                settings = settings,
                                onRequestPermissions = { requestAppPermissions() },
                                onRequestDefaultDialer = { requestDefaultDialerLauncher() }
                            )

                            // Simulated Heads-up floating popup of call
                            AnimatedVisibility(
                                visible = callState != AppCallState.Idle && showHeadsUpCallPopup,
                                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .statusBarsPadding()
                                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                            ) {
                                HeadsUpCallPopup(
                                    callState = callState,
                                    viewModel = viewModel,
                                    settings = settings
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDialerIntent(intent)
    }

    private fun handleDialerIntent(intent: Intent?) {
        if (intent == null) return
        val dataStr = intent.data?.schemeSpecificPart
        if (!dataStr.isNullOrEmpty()) {
            viewModel.clearDigits()
            for (char in dataStr) {
                if (char.isDigit() || char == '*' || char == '#' || char == '+') {
                    viewModel.appendDigit(char)
                }
            }
            viewModel.setTab("Keypad")
        }
    }

    private fun requestAppPermissions() {
        val permissions = mutableListOf(
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.WRITE_CONTACTS,
            android.Manifest.permission.CALL_PHONE,
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.WRITE_CALL_LOG
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionsLauncher.launch(permissions.toTypedArray())
    }

    private fun requestDefaultDialerLauncher() {
        try {
            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Default action not supported on this workspace", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun DialerMainScaffold(
    viewModel: DialerViewModel,
    settings: DialerSettings,
    onRequestPermissions: () -> Unit,
    onRequestDefaultDialer: () -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isDefaultDialer by viewModel.isDefaultDialer.collectAsState()
    val arePermissionsGranted by viewModel.arePermissionsGranted.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Current Active Screen
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1.0f)) {
                when (currentTab) {
                    "Recents" -> {
                        RecentsScreen(
                            viewModel = viewModel,
                            settings = settings,
                            onNavigateToCall = { num, name -> viewModel.startCall(num, name) }
                        )
                    }
                    "Contacts" -> {
                        ContactsScreen(
                            viewModel = viewModel,
                            settings = settings,
                            onNavigateToCall = { num, name -> viewModel.startCall(num, name) }
                        )
                    }
                    "Keypad" -> {
                        KeypadScreen(
                            viewModel = viewModel,
                            settings = settings,
                            onNavigateToCall = { num, name -> viewModel.startCall(num, name) }
                        )
                    }
                }
            }
        }

        // 2. Animated Sliding Glass Bottom Navigation Bar (excluding raw settings button, moved to three-dots)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            val navRadius = 30.dp
            val tabs = listOf(
                Triple("Recents", Icons.Default.Call, "recents_tab"),
                Triple("Contacts", Icons.Default.Person, "contacts_tab"),
                Triple("Keypad", Icons.Default.Dialpad, "keypad_tab")
            )

            val activeIndex = remember(currentTab) {
                val idx = tabs.indexOfFirst { it.first == currentTab }
                if (idx >= 0) idx else 0
            }

            GlassCard(
                settings = settings,
                modifier = Modifier
                    .width(340.dp)
                    .height(64.dp)
                    .testTag("floating_nav_bar"),
                cornerRadiusOverride = navRadius,
                borderWidth = 1.dp
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val totalWidth = maxWidth
                    val itemWidth = totalWidth / 3

                    val animatedOffset by animateDpAsState(
                        targetValue = itemWidth * activeIndex,
                        animationSpec = spring(
                            dampingRatio = 0.78f,
                            stiffness = 380f
                        ),
                        label = "tab_slide_anim"
                    )

                    // Sliding Pill capsule in background
                    Box(
                        modifier = Modifier
                            .offset(x = animatedOffset)
                            .width(itemWidth)
                            .fillMaxHeight()
                            .padding(6.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        settings.getAccentColor().copy(alpha = 0.28f),
                                        settings.getAccentColor().copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = settings.getAccentColor().copy(alpha = 0.45f),
                                shape = RoundedCornerShape(22.dp)
                            )
                    )

                    // Tab Item Row Overlay
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEach { (tabName, icon, tag) ->
                            val isSelected = currentTab == tabName
                            val color = if (isSelected) settings.getAccentColor() else Color.Gray

                            Column(
                                modifier = Modifier
                                    .weight(1.0f)
                                    .fillMaxHeight()
                                    .clickable { viewModel.setTab(tabName) }
                                    .padding(vertical = 6.dp)
                                    .testTag(tag),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = tabName,
                                    tint = color,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tabName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = color
                                )
                            }
                        }
                    }
                }
            }
        }

        // Automatic Overlay Dialog for Permissions (if missing or revoked!)
        if (!arePermissionsGranted) {
            AlertDialog(
                onDismissRequest = {}, // Force consent/action
                title = { Text("Permissions Required", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("This premium dialer requires system contacts & call logs access to display history and save contacts. Please allow permissions to continue.", color = Color.LightGray) },
                confirmButton = {
                    Button(
                        onClick = onRequestPermissions,
                        colors = ButtonDefaults.buttonColors(containerColor = settings.getAccentColor())
                    ) {
                        Text("Grant Access", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color(0xFF0F172A).copy(alpha = 0.95f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            )
        }

        // Automatic Dialog for Default status if permissions are granted but app is not system default!
        if (arePermissionsGranted && !isDefaultDialer) {
            AlertDialog(
                onDismissRequest = {}, // Force compliance
                title = { Text("Set as Default Dialer", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Phone Dialer requires default system status to capture and process inbound call triggers cleanly. Let's configure it.", color = Color.LightGray) },
                confirmButton = {
                    Button(
                        onClick = onRequestDefaultDialer,
                        colors = ButtonDefaults.buttonColors(containerColor = settings.getAccentColor())
                    ) {
                        Text("Set Default", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color(0xFF0F172A).copy(alpha = 0.95f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            )
        }

        // Beautiful glass full-screen modal overlay for settings!
        AnimatedVisibility(
            visible = isSettingsOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { viewModel.isSettingsOpen.value = false }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(top = 16.dp)
                        .clickable(enabled = false) {}
                ) {
                    GlassCard(
                        settings = settings,
                        modifier = Modifier.fillMaxSize(),
                        cornerRadiusOverride = 32.dp
                    ) {
                        SettingsScreen(
                            viewModel = viewModel,
                            settings = settings,
                            onClose = { viewModel.isSettingsOpen.value = false }
                        )
                    }
                }
            }
        }
    }
}
