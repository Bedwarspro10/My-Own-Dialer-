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
import androidx.lifecycle.lifecycleScope
import android.app.KeyguardManager
import com.example.ui.viewmodel.CallStateHolder
import com.example.ui.viewmodel.HolderCallEvent
import kotlinx.coroutines.launch
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.screens.*
import com.example.ui.settings.DialerSettings
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppCallState
import com.example.ui.viewmodel.DialerViewModel
import coil.compose.AsyncImage
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Environment

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: DialerViewModel

    companion object {
        var isResumed = false
    }

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

    // Request role launcher for default app (Android 10/Q+)
    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        checkAppPermissionsAndStatus()
    }

    override fun onResume() {
        super.onResume()
        isResumed = true
        checkAppPermissionsAndStatus()
    }

    override fun onPause() {
        super.onPause()
        isResumed = false
    }

    private fun checkAppPermissionsAndStatus() {
        if (!::viewModel.isInitialized) return
        val isDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as? android.app.role.RoleManager
            roleManager?.isRoleHeld(android.app.role.RoleManager.ROLE_DIALER) ?: false
        } else {
            val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            telecomManager?.defaultDialerPackage == packageName
        }
        viewModel.isDefaultDialer.value = isDefault

        val contactsGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val logsGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        val phoneStateGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        viewModel.arePermissionsGranted.value = contactsGranted && logsGranted && phoneStateGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[DialerViewModel::class.java]

        // Handle dialer tel scheme incoming intent
        handleDialerIntent(intent)

        lifecycleScope.launch {
            CallStateHolder.callEventFlow.collect { event ->
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                val isDeviceLocked = keyguardManager?.isKeyguardLocked ?: false
                val isAppInBackground = !isResumed
                
                when (event) {
                    is HolderCallEvent.Incoming -> {
                        val finalName = viewModel.contactsList.value
                            .firstOrNull { it.phoneNumber.replace(" ", "") == event.number.replace(" ", "") }?.name ?: event.number
                        val label = viewModel.contactsList.value
                            .firstOrNull { it.phoneNumber.replace(" ", "") == event.number.replace(" ", "") }?.label ?: "mobile"
                        
                        viewModel.isLockedSimulation.value = isDeviceLocked
                        viewModel.triggerIncomingCallImmediately(
                            name = finalName,
                            number = event.number,
                            label = label,
                            isHeadsUp = isAppInBackground && !isDeviceLocked
                        )
                        
                        // If app is in background but device is locked, we want to launch MainActivity on top of the lockscreen!
                        if (isDeviceLocked || !isAppInBackground) {
                            val launchIntent = Intent(this@MainActivity, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            startActivity(launchIntent)
                        }
                    }
                    is HolderCallEvent.Ongoing -> {
                        val finalName = viewModel.contactsList.value
                            .firstOrNull { it.phoneNumber.replace(" ", "") == event.number.replace(" ", "") }?.name ?: event.number
                        viewModel.startCall(event.number, finalName)
                    }
                    is HolderCallEvent.Idle -> {
                        viewModel.hangUpCall()
                    }
                }
            }
        }

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
            android.Manifest.permission.WRITE_CALL_LOG,
            android.Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionsLauncher.launch(permissions.toTypedArray())
    }

    private fun requestDefaultDialerLauncher() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = getSystemService(Context.ROLE_SERVICE) as? android.app.role.RoleManager
                if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_DIALER)) {
                    if (!roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_DIALER)) {
                        val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_DIALER)
                        roleRequestLauncher.launch(intent)
                    } else {
                        Toast.makeText(this, "Already set as default dialer", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    launchLegacyDefaultDialer()
                }
            } else {
                launchLegacyDefaultDialer()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "RoleManager request failed, fallback to legacy", e)
            launchLegacyDefaultDialer()
        }
    }

    private fun launchLegacyDefaultDialer() {
        try {
            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Default action not supported on this device/workspace", Toast.LENGTH_SHORT).show()
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
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()

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

        // Automatic Overlay Dialog for Onboarding & Download APK / Permissions
        if (isFirstLaunch) {
            val context = LocalContext.current
            OnboardingDownloadDialog(
                settings = settings,
                onDownloadApk = {
                    extractAndSaveApk(context)
                },
                onDismiss = {
                    viewModel.completeFirstLaunch()
                }
            )
        } else if (!arePermissionsGranted) {
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

@Composable
fun OnboardingDownloadDialog(
    settings: DialerSettings,
    onDownloadApk: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {}, // Force action
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = "Extract APK",
                    tint = settings.getAccentColor(),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Extract Offline Installer",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "No need to download from a slow web link! This app can automatically extract its fully functioning offline installer APK file directly to your phone's 'Downloads' storage so you can easily install or share it on any device.",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Async QR Code
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=https%3A%2F%2Fais-pre-h2edxchs4vtrji2w2udxrc-757791243390.asia-southeast1.run.app%2F.build-outputs%2Fapp-debug.apk",
                        contentDescription = "Scan QR Code to download APK",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Scan QR is also available, or click Extract below for offline install",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onDownloadApk,
                    colors = ButtonDefaults.buttonColors(containerColor = settings.getAccentColor()),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Extract & Save APK Offline", color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("Continue to App", color = Color.LightGray, fontWeight = FontWeight.Medium)
                }
            }
        },
        containerColor = Color(0xFF0F172A).copy(alpha = 0.95f),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
    )
}

fun extractAndSaveApk(context: Context) {
    try {
        val appInfo = context.applicationInfo
        val srcFile = java.io.File(appInfo.sourceDir)
        if (!srcFile.exists()) {
            Toast.makeText(context, "Error: Source APK could not be found", Toast.LENGTH_SHORT).show()
            return
        }

        val filename = "Glass_Phone_Dialer.apk"

        // 1. Write the base.apk directly to the standard public Downloads folder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    srcFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        } else {
            // Legacy/Fallback for below Android Q: Direct file copy to standard Downloads folder
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            val destFile = java.io.File(downloadDir, filename)
            srcFile.inputStream().use { inputStream ->
                destFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }

        // Show Toast notifying success in extraction!
        Toast.makeText(
            context, 
            "Successfully extracted & saved the installer file to phone's standard 'Downloads' folder!", 
            Toast.LENGTH_LONG
        ).show()

        // 2. Also trigger standard Share Sheet for convenient immediate install / sending!
        try {
            val apkUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                srcFile
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, apkUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Save or Install Extracted APK"))
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to show share sheet fallback", e)
        }

    } catch (e: Exception) {
        Log.e("MainActivity", "Error extracting and saving APK", e)
        Toast.makeText(context, "Error copying APK: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

