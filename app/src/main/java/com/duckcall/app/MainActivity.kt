package com.duckcall.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.app.role.RoleManager
import android.telecom.TelecomManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF101114)
private val Teal = Color(0xFF2563EB)
private val Mint = Color(0xFFFFF2B3)
private val Muted = Color(0xFF707784)
private val Canvas = Color(0xFFF4F5F8)

class MainActivity : ComponentActivity() {
    private var incomingDialNumber by mutableStateOf("")
    private val notificationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val callPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            Toast.makeText(this, "Phone permission is needed to place cellular calls from DuckCall", Toast.LENGTH_LONG).show()
        } else {
            requestNotificationPermissionIfNeeded()
        }
    }
    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (isDefaultDialer()) {
            Toast.makeText(this, "DuckCall is now your default phone app", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "DuckCall was not selected as the default phone app", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingDialNumber = intent?.data?.schemeSpecificPart.orEmpty()
        setContent { DuckCallApp(initialNumber = incomingDialNumber, onRequestDefaultDialer = { requestDefaultDialer() }) }
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            callPermissionRequest.launch(Manifest.permission.CALL_PHONE)
        } else {
            requestNotificationPermissionIfNeeded()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingDialNumber = intent.data?.schemeSpecificPart.orEmpty()
    }

    private fun isDefaultDialer(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_DIALER)
        } else {
            getSystemService(TelecomManager::class.java).defaultDialerPackage == packageName
        }
    }

    private fun requestDefaultDialer() {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getSystemService(RoleManager::class.java)
                    .createRequestRoleIntent(RoleManager.ROLE_DIALER)
            } else {
                Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                    .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
            }
            roleRequest.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Android could not open the default phone app settings", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
private fun DuckCallApp(initialNumber: String, onRequestDefaultDialer: () -> Unit) {
    var selectedTab by rememberSaveable { mutableStateOf("Dialer") }
    var phoneNumber by rememberSaveable { mutableStateOf(initialNumber) }
    LaunchedEffect(initialNumber) { if (initialNumber.isNotBlank()) phoneNumber = initialNumber }
    var showVoipDialog by remember { mutableStateOf(false) }
    val recents = remember { mutableStateListOf<String>() }
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("duckcall_preferences", android.content.Context.MODE_PRIVATE) }
    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    var callsMadeToday by remember {
        mutableStateOf(
            if (preferences.getString("calls_today_date", "") == todayKey) {
                preferences.getInt("calls_made_today", 0)
            } else 0
        )
    }
    LaunchedEffect(todayKey) {
        if (preferences.getString("calls_today_date", "") != todayKey) {
            callsMadeToday = 0
            preferences.edit().putString("calls_today_date", todayKey).putInt("calls_made_today", 0).apply()
        }
    }
    var cellularEnabled by remember { mutableStateOf(preferences.getBoolean("cellular_enabled", true)) }
    var privateCallerId by remember { mutableStateOf(preferences.getBoolean("private_caller_id_enabled", false)) }
    var internetEnabled by remember { mutableStateOf(preferences.getBoolean("internet_enabled", true)) }
    var recentsEnabled by remember { mutableStateOf(preferences.getBoolean("recents_enabled", false)) }
    var favoritesEnabled by remember { mutableStateOf(preferences.getBoolean("favorites_enabled", false)) }
    var contactsEnabled by remember { mutableStateOf(preferences.getBoolean("contacts_enabled", true)) }
    var muteControlEnabled by remember { mutableStateOf(preferences.getBoolean("mute_control_enabled", true)) }
    var speakerControlEnabled by remember { mutableStateOf(preferences.getBoolean("speaker_control_enabled", true)) }
    var holdControlEnabled by remember { mutableStateOf(preferences.getBoolean("hold_control_enabled", true)) }
    var dialerLayout by remember { mutableStateOf(preferences.getString("dialer_layout", "standard") ?: "standard") }
    var mainScreenStyle by remember { mutableStateOf(preferences.getString("main_screen_style", "minimal") ?: "minimal") }
    var showDialpad by remember { mutableStateOf(preferences.getBoolean("show_dialpad", true)) }
    var showSearch by remember { mutableStateOf(preferences.getBoolean("show_search", false)) }
    var navigationPosition by remember { mutableStateOf(preferences.getString("navigation_position", "bottom") ?: "bottom") }
    var accentColor by remember { mutableStateOf(preferences.getString("accent_color", "blue") ?: "blue") }
    var callButtonOrder by remember { mutableStateOf(preferences.getString("call_button_order", "cellular_first") ?: "cellular_first") }
    var inCallButtonOrder by remember { mutableStateOf(preferences.getString("in_call_button_order", "mute_speaker_hold") ?: "mute_speaker_hold") }
    fun savePreference(key: String, value: Boolean) { preferences.edit().putBoolean(key, value).apply() }
    fun saveChoice(key: String, value: String) { preferences.edit().putString(key, value).apply() }
    LaunchedEffect(selectedTab, recentsEnabled, favoritesEnabled, contactsEnabled) {
        if (selectedTab == "Recents" && !recentsEnabled || selectedTab == "Favorites" && !favoritesEnabled || selectedTab == "Contacts" && !contactsEnabled) selectedTab = "Dialer"
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Teal,
            onPrimary = Color.White,
            secondary = Mint,
            background = Canvas,
            surface = Color.White,
            onSurface = Ink
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Canvas) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 22.dp, vertical = 18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Mint, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🦆", fontSize = 23.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("DuckCall", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Ink)
                            if (mainScreenStyle != "minimal") Text("A clearer way to connect", fontSize = 12.sp, color = Muted)
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { selectedTab = "Settings" }) {
                            Text("⚙ Settings", color = Teal, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (ActiveCallStore.call != null) {
                    InCallScreen(
                        showMute = muteControlEnabled, showSpeaker = speakerControlEnabled,
                        showHold = holdControlEnabled, buttonOrder = inCallButtonOrder
                    )
                } else when (selectedTab) {
                    "Dialer" -> DialerScreen(
                        number = phoneNumber,
                        onNumberChange = { phoneNumber = it },
                        onCellularCall = {
                            if (phoneNumber.isNotBlank()) {
                                val roleManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) context.getSystemService(RoleManager::class.java) else null
                                val isDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) == true
                                } else {
                                    (context.getSystemService(android.content.Context.TELECOM_SERVICE) as TelecomManager).defaultDialerPackage == context.packageName
                                }
                                if (!isDefault) {
                                    onRequestDefaultDialer()
                                } else {
                                    try {
                                        val telecom = context.getSystemService(android.content.Context.TELECOM_SERVICE) as TelecomManager
                                        val digitsOnly = phoneNumber.filter { it.isDigit() }
                                        val isEmergencyNumber = digitsOnly == "911" || digitsOnly == "112"
                                        val dialString = if (privateCallerId && !isEmergencyNumber && !phoneNumber.startsWith("*67")) {
                                            "*67$phoneNumber"
                                        } else {
                                            phoneNumber
                                        }
                                        telecom.placeCall(
                                            Uri.fromParts("tel", dialString, null),
                                            Bundle()
                                        )
                                        recents.add(0, phoneNumber)
                                        callsMadeToday += 1
                                        preferences.edit()
                                            .putString("calls_today_date", todayKey)
                                            .putInt("calls_made_today", callsMadeToday)
                                            .apply()
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "DuckCall could not place this call", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        onVoipCall = { showVoipDialog = true },
                        cellularEnabled = cellularEnabled,
                        privateCallerId = privateCallerId,
                        onPrivateCallerIdChange = { privateCallerId = it; savePreference("private_caller_id_enabled", it) },
                        internetEnabled = internetEnabled,
                        dialerLayout = dialerLayout, callButtonOrder = callButtonOrder,
                        showDialpad = showDialpad,
                        callsMadeToday = callsMadeToday
                    )
                    "Recents" -> RecentsScreen(recents)
                    "Favorites" -> FavoritesScreen()
                    "Contacts" -> ContactsScreen()
                    else -> SettingsScreen(
                        onVoipSetup = { showVoipDialog = true }, onRequestDefaultDialer = onRequestDefaultDialer,
                        cellularEnabled = cellularEnabled, onCellularChange = { cellularEnabled = it; savePreference("cellular_enabled", it) },
                        internetEnabled = internetEnabled, onInternetChange = { internetEnabled = it; savePreference("internet_enabled", it) },
                        recentsEnabled = recentsEnabled, onRecentsChange = { recentsEnabled = it; savePreference("recents_enabled", it) },
                        favoritesEnabled = favoritesEnabled, onFavoritesChange = { favoritesEnabled = it; savePreference("favorites_enabled", it) },
                        contactsEnabled = contactsEnabled, onContactsChange = { contactsEnabled = it; savePreference("contacts_enabled", it) },
                        mainScreenStyle = mainScreenStyle, onMainScreenStyleChange = { mainScreenStyle = it; saveChoice("main_screen_style", it) },
                        showSearch = showSearch, onShowSearchChange = { showSearch = it; savePreference("show_search", it) },
                        showDialpad = showDialpad, onShowDialpadChange = { showDialpad = it; savePreference("show_dialpad", it) },
                        navigationPosition = navigationPosition, onNavigationPositionChange = { navigationPosition = it; saveChoice("navigation_position", it) },
                        accentColor = accentColor, onAccentColorChange = { accentColor = it; saveChoice("accent_color", it) },
                        muteControlEnabled = muteControlEnabled, onMuteControlChange = { muteControlEnabled = it; savePreference("mute_control_enabled", it) },
                        speakerControlEnabled = speakerControlEnabled, onSpeakerControlChange = { speakerControlEnabled = it; savePreference("speaker_control_enabled", it) },
                        holdControlEnabled = holdControlEnabled, onHoldControlChange = { holdControlEnabled = it; savePreference("hold_control_enabled", it) },
                        dialerLayout = dialerLayout, onDialerLayoutChange = { dialerLayout = it; saveChoice("dialer_layout", it) },
                        callButtonOrder = callButtonOrder, onCallButtonOrderChange = { callButtonOrder = it; saveChoice("call_button_order", it) },
                        inCallButtonOrder = inCallButtonOrder, onInCallButtonOrderChange = { inCallButtonOrder = it; saveChoice("in_call_button_order", it) }
                    )
                }

                Spacer(Modifier.weight(1f))
                BottomNavigation(selectedTab = selectedTab, onSelect = { selectedTab = it }, recentsEnabled = recentsEnabled, favoritesEnabled = favoritesEnabled, contactsEnabled = contactsEnabled)
            }
        }

        if (showVoipDialog) {
            AlertDialog(
                onDismissRequest = { showVoipDialog = false },
                title = { Text("Set up internet calling") },
                text = {
                    Text(
                        "The VoIP calling screen is planned, but real internet calls need a calling provider or DuckCall server. Once we choose one, we can add sign-in, audio, incoming calls, and call notifications."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showVoipDialog = false }) { Text("Got it") }
                }
            )
        }
    }
}

@Composable
private fun DialerScreen(
    number: String,
    onNumberChange: (String) -> Unit,
    onCellularCall: () -> Unit,
    onVoipCall: () -> Unit,
    cellularEnabled: Boolean,
    privateCallerId: Boolean,
    onPrivateCallerIdChange: (Boolean) -> Unit,
    internetEnabled: Boolean,
    dialerLayout: String,
    callButtonOrder: String,
    showDialpad: Boolean,
    callsMadeToday: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("CALLS MADE TODAY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Muted, letterSpacing = 1.1.sp)
                Text("Outgoing cellular calls", fontSize = 11.sp, color = Muted)
            }
            Text(callsMadeToday.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Teal)
        }
        Spacer(Modifier.height(18.dp))
        Text("DIAL A NUMBER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Muted, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = number,
            onValueChange = onNumberChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("(555) 000-0000", color = Color.LightGray) },
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center, color = Ink),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            trailingIcon = {
                if (number.isNotEmpty()) {
                    TextButton(onClick = { onNumberChange(number.dropLast(1)) }) { Text("⌫", fontSize = 20.sp) }
                }
            }
        )
        Spacer(Modifier.height(10.dp))
        ToggleSetting(
            "Hide my caller ID",
            "Use *67 for outgoing cellular calls",
            privateCallerId,
            onPrivateCallerIdChange
        )
        Spacer(Modifier.height(8.dp))

        if (showDialpad) {
            val keys = listOf(
                listOf("1" to "", "2" to "ABC", "3" to "DEF"),
                listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
                listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
                listOf("*" to "", "0" to "+", "#" to "")
            )
            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                ) {
                    row.forEach { (digit, letters) ->
                        Column(
                            modifier = Modifier
                                .size(
                                    width = when (dialerLayout) { "compact" -> 72.dp; "large" -> 92.dp; else -> 82.dp },
                                    height = when (dialerLayout) { "compact" -> 56.dp; "large" -> 74.dp; else -> 65.dp }
                                )
                                .background(Color.White, RoundedCornerShape(18.dp))
                                .clickable { onNumberChange(number + digit) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(digit, fontSize = 25.sp, fontWeight = FontWeight.Medium, color = Ink)
                            if (letters.isNotEmpty()) Text(letters, fontSize = 9.sp, color = Muted, letterSpacing = 1.sp)
                        }
                    }
                }
                Spacer(Modifier.height(9.dp))
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (cellularEnabled && callButtonOrder == "cellular_first") Button(
                onClick = onCellularCall, enabled = number.isNotBlank(),
                modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal)
            ) { Text("☎  Cellular", fontWeight = FontWeight.SemiBold) }
            if (internetEnabled && callButtonOrder == "cellular_first") OutlinedButton(
                onClick = onVoipCall, enabled = number.isNotBlank(),
                modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(16.dp)
            ) { Text("◎  Internet", fontWeight = FontWeight.SemiBold) }
            if (internetEnabled && callButtonOrder == "internet_first") OutlinedButton(
                onClick = onVoipCall, enabled = number.isNotBlank(),
                modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(16.dp)
            ) { Text("◎  Internet", fontWeight = FontWeight.SemiBold) }
            if (cellularEnabled && callButtonOrder == "internet_first") Button(
                onClick = onCellularCall, enabled = number.isNotBlank(),
                modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal)
            ) { Text("☎  Cellular", fontWeight = FontWeight.SemiBold) }
        }
        Spacer(Modifier.height(8.dp))
        Text("Calls are handled by Android Telecom when DuckCall is the default phone app.", fontSize = 11.sp, color = Muted)
    }
}

@Composable
private fun RecentsScreen(recents: List<String>) {
    Column(Modifier.fillMaxWidth().padding(22.dp)) {
        Text("Recent calls", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("Your latest calls from this session", fontSize = 13.sp, color = Muted)
        Spacer(Modifier.height(18.dp))
        if (recents.isEmpty()) {
            EmptyState("◷", "No calls yet", "Numbers you dial from DuckCall will appear here.")
        } else {
            recents.forEach { number ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(42.dp).background(Mint, CircleShape), contentAlignment = Alignment.Center) {
                        Text("↗", color = Teal, fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(number, fontWeight = FontWeight.SemiBold, color = Ink)
                        Text("Outgoing · Cellular", fontSize = 11.sp, color = Muted)
                    }
                }
                Divider(color = Color(0xFFE9EEEE))
            }
        }
    }
}

@Composable
private fun FavoritesScreen() {
    Column(Modifier.fillMaxWidth().padding(22.dp)) {
        Text("Favorites", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("Your most important people, one tap away", fontSize = 13.sp, color = Muted)
        Spacer(Modifier.height(22.dp))
        EmptyState("★", "No favorites yet", "Favorite contacts will appear here once Contacts is connected.")
    }
}

@Composable
private fun ContactsScreen() {
    Column(Modifier.fillMaxWidth().padding(22.dp)) {
        Text("Contacts", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("People you'll reach from DuckCall", fontSize = 13.sp, color = Muted)
        Spacer(Modifier.height(22.dp))
        EmptyState("♙", "Your people, one tap away", "Contact access is not enabled yet. We'll add a permission-safe contact picker next.")
    }
}

@Composable
private fun SettingsScreen(
    onVoipSetup: () -> Unit, onRequestDefaultDialer: () -> Unit,
    cellularEnabled: Boolean, onCellularChange: (Boolean) -> Unit,
    internetEnabled: Boolean, onInternetChange: (Boolean) -> Unit,
    recentsEnabled: Boolean, onRecentsChange: (Boolean) -> Unit,
    favoritesEnabled: Boolean, onFavoritesChange: (Boolean) -> Unit,
    contactsEnabled: Boolean, onContactsChange: (Boolean) -> Unit,
    mainScreenStyle: String, onMainScreenStyleChange: (String) -> Unit,
    showSearch: Boolean, onShowSearchChange: (Boolean) -> Unit,
    showDialpad: Boolean, onShowDialpadChange: (Boolean) -> Unit,
    navigationPosition: String, onNavigationPositionChange: (String) -> Unit,
    accentColor: String, onAccentColorChange: (String) -> Unit,
    muteControlEnabled: Boolean, onMuteControlChange: (Boolean) -> Unit,
    speakerControlEnabled: Boolean, onSpeakerControlChange: (Boolean) -> Unit,
    holdControlEnabled: Boolean, onHoldControlChange: (Boolean) -> Unit,
    dialerLayout: String, onDialerLayoutChange: (String) -> Unit,
    callButtonOrder: String, onCallButtonOrderChange: (String) -> Unit,
    inCallButtonOrder: String, onInCallButtonOrderChange: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 16.dp)) {
        Text("Settings", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("Control what appears and how DuckCall works", fontSize = 13.sp, color = Muted)
        Spacer(Modifier.height(18.dp))
        Text("CALLING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Teal, letterSpacing = 1.2.sp)
        ToggleSetting("Cellular calling", "Show and enable carrier calls", cellularEnabled, onCellularChange)
        ToggleSetting("Internet calling", "Show the internet call option (provider setup still required)", internetEnabled, onInternetChange)
        Button(onClick = onRequestDefaultDialer, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Set DuckCall as default phone app") }
        Spacer(Modifier.height(18.dp))
        Text("NAVIGATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Teal, letterSpacing = 1.2.sp)
        ToggleSetting("Favorites", "Show the Favorites tab", favoritesEnabled, onFavoritesChange)
        ToggleSetting("Recent calls", "Show the Recents tab", recentsEnabled, onRecentsChange)
        ToggleSetting("Contacts", "Show the Contacts tab", contactsEnabled, onContactsChange)
        ChoiceSetting("Main screen style", mainScreenStyle.replaceFirstChar { it.uppercase() }) {
            onMainScreenStyleChange(if (mainScreenStyle == "minimal") "classic" else "minimal")
        }
        ToggleSetting("Search bar", "Show search on the main screen", showSearch, onShowSearchChange)
        ToggleSetting("Dial pad", "Show the number keypad", showDialpad, onShowDialpadChange)
        ChoiceSetting("Navigation position", navigationPosition.replaceFirstChar { it.uppercase() }) {
            onNavigationPositionChange(if (navigationPosition == "bottom") "top" else "bottom")
        }
        ChoiceSetting("Accent color", accentColor.replaceFirstChar { it.uppercase() }) {
            onAccentColorChange(if (accentColor == "blue") "yellow" else "blue")
        }
        Spacer(Modifier.height(18.dp))
        Text("IN-CALL CONTROLS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Teal, letterSpacing = 1.2.sp)
        ToggleSetting("Mute button", "Show the mute control during calls", muteControlEnabled, onMuteControlChange)
        ToggleSetting("Speakerphone button", "Show the speakerphone control during calls", speakerControlEnabled, onSpeakerControlChange)
        ToggleSetting("Hold button", "Show the hold and resume control during calls", holdControlEnabled, onHoldControlChange)
        ChoiceSetting("In-call button order", if (inCallButtonOrder == "mute_speaker_hold") "Mute · Speaker · Hold" else "Hold · Mute · Speaker") {
            val next = if (inCallButtonOrder == "mute_speaker_hold") "hold_mute_speaker" else "mute_speaker_hold"
            onInCallButtonOrderChange(next)
        }
        Spacer(Modifier.height(18.dp))
        Text("DIALER LAYOUT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Teal, letterSpacing = 1.2.sp)
        ChoiceSetting("Key size", dialerLayout.replaceFirstChar { it.uppercase() }) {
            val next = when (dialerLayout) { "standard" -> "compact"; "compact" -> "large"; else -> "standard" }
            onDialerLayoutChange(next)
        }
        ChoiceSetting("Call button order", if (callButtonOrder == "cellular_first") "Cellular first" else "Internet first") {
            onCallButtonOrderChange(if (callButtonOrder == "cellular_first") "internet_first" else "cellular_first")
        }
        Spacer(Modifier.height(18.dp))
        Text("MORE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Teal, letterSpacing = 1.2.sp)
        SettingRow("App version", "DuckCall 0.1.0", "Test build")
        Button(onClick = onVoipSetup, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Configure internet calling") }
    }
}

@Composable
private fun ToggleSetting(title: String, detail: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.height(3.dp))
            Text(detail, fontSize = 12.sp, color = Muted)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
    Divider(color = Color(0xFFE9EEEE))
}

@Composable
private fun ChoiceSetting(title: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(value, fontSize = 12.sp, color = Muted)
        }
        OutlinedButton(onClick = onClick) { Text("Change") }
    }
    Divider(color = Color(0xFFE9EEEE))
}

@Composable
private fun SettingRow(title: String, detail: String, status: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.weight(1f))
            if (status.isNotEmpty()) Text(status, fontSize = 11.sp, color = Teal, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        Text(detail, fontSize = 12.sp, color = Muted)
    }
    Divider(color = Color(0xFFE9EEEE))
}

@Composable
private fun EmptyState(symbol: String, title: String, detail: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(66.dp).background(Mint, CircleShape), contentAlignment = Alignment.Center) {
            Text(symbol, color = Teal, fontSize = 27.sp)
        }
        Spacer(Modifier.height(14.dp))
        Text(title, fontWeight = FontWeight.SemiBold, color = Ink)
        Spacer(Modifier.height(5.dp))
        Text(detail, color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BottomNavigation(selectedTab: String, onSelect: (String) -> Unit, recentsEnabled: Boolean, favoritesEnabled: Boolean, contactsEnabled: Boolean) {
    val tabs = buildList {
        add("Dialer" to "⌗")
        if (favoritesEnabled) add("Favorites" to "★")
        if (recentsEnabled) add("Recents" to "◷")
        if (contactsEnabled) add("Contacts" to "♙")
        add("Settings" to "⚙")
    }
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        tabs.forEach { (label, symbol) ->
            val selected = selectedTab == label
            Column(
                modifier = Modifier.weight(1f).clickable { onSelect(label) }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(symbol, fontSize = 21.sp, color = if (selected) Teal else Muted)
                Text(label, fontSize = 10.sp, color = if (selected) Teal else Muted, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}


@Composable
private fun InCallScreen(showMute: Boolean, showSpeaker: Boolean, showHold: Boolean, buttonOrder: String) {
    val callState = ActiveCallStore.state
    val incoming = callState == android.telecom.Call.STATE_RINGING
    val held = callState == android.telecom.Call.STATE_HOLDING
    val status = when (callState) {
        android.telecom.Call.STATE_RINGING -> "Incoming call"
        android.telecom.Call.STATE_DIALING -> "Calling…"
        android.telecom.Call.STATE_CONNECTING -> "Connecting…"
        android.telecom.Call.STATE_ACTIVE -> "Connected"
        android.telecom.Call.STATE_HOLDING -> "On hold"
        android.telecom.Call.STATE_DISCONNECTED -> "Call ended"
        else -> "Call"
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(104.dp).background(Mint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("☎", color = Teal, fontSize = 42.sp)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            ActiveCallStore.phoneNumber.ifBlank { "Unknown caller" },
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(status, color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(42.dp))

        if (incoming) {
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Button(
                    onClick = { ActiveCallStore.hangUp() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
                    shape = CircleShape,
                    modifier = Modifier.size(76.dp)
                ) { Text("Decline", fontSize = 11.sp) }
                Button(
                    onClick = { ActiveCallStore.answer() },
                    colors = ButtonDefaults.buttonColors(containerColor = Teal),
                    shape = CircleShape,
                    modifier = Modifier.size(76.dp)
                ) { Text("Answer", fontSize = 11.sp) }
            }
        } else {
            val muteButton: @Composable () -> Unit = {
                if (showMute) OutlinedButton(onClick = { ActiveCallStore.toggleMute() }) {
                    Text(if (ActiveCallStore.muted) "Unmute" else "Mute")
                }
            }
            val speakerButton: @Composable () -> Unit = {
                if (showSpeaker) OutlinedButton(onClick = { ActiveCallStore.toggleSpeaker() }) {
                    Text(if (ActiveCallStore.speaker) "Earpiece" else "Speaker")
                }
            }
            val holdButton: @Composable () -> Unit = {
                if (showHold) OutlinedButton(onClick = {
                    if (held) ActiveCallStore.unhold() else ActiveCallStore.hold()
                }) { Text(if (held) "Resume" else "Hold") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (buttonOrder == "hold_mute_speaker") {
                    holdButton(); muteButton(); speakerButton()
                } else {
                    muteButton(); speakerButton(); holdButton()
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { ActiveCallStore.hangUp() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
                shape = CircleShape,
                modifier = Modifier.size(76.dp)
            ) { Text("End call", fontSize = 11.sp) }
        }
    }
}
