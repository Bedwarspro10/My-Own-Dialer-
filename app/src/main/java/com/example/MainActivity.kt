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

                    // Visual switching between Idle (standard tabs) and call states
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
                            AppCallState.Idle -> {
                                DialerMainScaffold(
                                    viewModel = viewModel,
                                    settings = settings,
                                    onRequestPermissions = { requestAppPermissions() },
                                    onRequestDefaultDialer = { requestDefaultDialerLauncher() }
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
    val context = LocalContext.current

    // Detect if we are currently registered as default dialer
    var isDefaultDialer by remember { mutableStateOf(true) }
    var arePermissionsGranted by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Poll status
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        isDefaultDialer = telecomManager?.defaultDialerPackage == context.packageName

        val contactsCheck = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS)
        val logsCheck = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG)
        arePermissionsGranted = (contactsCheck == PackageManager.PERMISSION_GRANTED && logsCheck == PackageManager.PERMISSION_GRANTED)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Current Active Screen
        Column(modifier = Modifier.fillMaxSize()) {
            // Elegant top onboarding notification banner if setup is incomplete
            if (!isDefaultDialer || !arePermissionsGranted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .clickable {
                            if (!arePermissionsGranted) {
                                onRequestPermissions()
                            } else if (!isDefaultDialer) {
                                onRequestDefaultDialer()
                            }
                        }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.0f)) {
                            Text(
                                text = if (!arePermissionsGranted) "Access System Contacts" else "Set Phone Dialer as Default",
                                color = settings.getAccentColor(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (!arePermissionsGranted) "Unlock caller details sync and view historic call logs" else "Enjoy intelligent spam protection and native T9 dialer",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                        Button(
                            onClick = {
                                if (!arePermissionsGranted) onRequestPermissions() else onRequestDefaultDialer()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = settings.getAccentColor()),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(text = "Setup", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

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
                    "Settings" -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            settings = settings
                        )
                    }
                }
            }
        }

        // 2. Majestic Floating Glass Bottom Navigation Bar matching target photos
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            val navRadius = 30.dp
            
            GlassCard(
                settings = settings,
                modifier = Modifier
                    .width(340.dp)
                    .height(64.dp)
                    .testTag("floating_nav_bar"),
                cornerRadiusOverride = navRadius,
                borderWidth = 1.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf(
                        Triple("Recents", Icons.Default.Call, "recents_tab"),
                        Triple("Contacts", Icons.Default.Person, "contacts_tab"),
                        Triple("Keypad", Icons.Default.Dialpad, "keypad_tab"),
                        Triple("Settings", Icons.Default.Settings, "settings_tab")
                    )

                    tabs.forEach { (tabName, icon, tag) ->
                        val isSelected = currentTab == tabName
                        val color = if (isSelected) settings.getAccentColor() else Color.Gray

                        Column(
                            modifier = Modifier
                                .weight(1.0f)
                                .fillMaxHeight()
                                .clickable { viewModel.setTab(tabName) }
                                .padding(vertical = 4.dp)
                                .testTag(tag),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = tabName,
                                tint = color,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tabName,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = color
                            )
                        }
                    }
                }
            }
        }
    }
}
