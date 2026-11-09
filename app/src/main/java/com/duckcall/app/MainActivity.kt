package com.duckcall.app

import android.content.Intent
import android.app.role.RoleManager
import android.telecom.TelecomManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.os.Bundle
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

private val Ink = Color(0xFF14252B)
private val Teal = Color(0xFF087E78)
private val Mint = Color(0xFFE6F4F1)
private val Muted = Color(0xFF718187)
private val Canvas = Color(0xFFF6F8F8)

class MainActivity : ComponentActivity() {
    private var incomingDialNumber by mutableStateOf("")
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
                            Text("A clearer way to connect", fontSize = 12.sp, color = Muted)
                        }
                    }
                }

                if (ActiveCallStore.call != null) {
                    InCallScreen()
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
                                        telecom.placeCall(
                                            Uri.fromParts("tel", phoneNumber, null),
                                            Bundle()
                                        )
                                        recents.add(0, phoneNumber)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "DuckCall could not place this call", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        onVoipCall = { showVoipDialog = true }
                    )
                    "Recents" -> RecentsScreen(recents)
                    "Contacts" -> ContactsScreen()
                    else -> SettingsScreen(onVoipSetup = { showVoipDialog = true }, onRequestDefaultDialer = onRequestDefaultDialer)
                }

                Spacer(Modifier.weight(1f))
                BottomNavigation(selectedTab = selectedTab, onSelect = { selectedTab = it })
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
    onVoipCall: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
        Spacer(Modifier.height(18.dp))

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
                            .size(width = 82.dp, height = 65.dp)
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

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCellularCall,
                enabled = number.isNotBlank(),
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal)
            ) {
                Text("☎  Cellular", fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = onVoipCall,
                enabled = number.isNotBlank(),
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("◎  Internet", fontWeight = FontWeight.SemiBold)
            }
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
                        Text("Outgoing · Cellular handoff", fontSize = 11.sp, color = Muted)
                    }
                }
                Divider(color = Color(0xFFE9EEEE))
            }
        }
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
private fun SettingsScreen(onVoipSetup: () -> Unit, onRequestDefaultDialer: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(22.dp)) {
        Text("Settings", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("Make DuckCall yours", fontSize = 13.sp, color = Muted)
        Spacer(Modifier.height(20.dp))
        SettingRow("Cellular calling", "DuckCall can become your default phone app", "Android Telecom")
        Button(onClick = onRequestDefaultDialer, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Text("Set DuckCall as default phone app")
        }
        SettingRow("Internet calling", "Requires a VoIP provider or DuckCall backend", "Setup needed")
        SettingRow("App version", "DuckCall 0.1.0", "")
        Spacer(Modifier.height(12.dp))
        Button(onClick = onVoipSetup, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Text("Configure internet calling")
        }
    }
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
private fun BottomNavigation(selectedTab: String, onSelect: (String) -> Unit) {
    val tabs = listOf("Dialer" to "⌗", "Recents" to "◷", "Contacts" to "♙", "Settings" to "⚙")
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
private fun InCallScreen() {
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { ActiveCallStore.toggleMute() }) {
                    Text(if (ActiveCallStore.muted) "Unmute" else "Mute")
                }
                OutlinedButton(onClick = { ActiveCallStore.toggleSpeaker() }) {
                    Text(if (ActiveCallStore.speaker) "Earpiece" else "Speaker")
                }
                OutlinedButton(onClick = {
                    if (held) ActiveCallStore.unhold() else ActiveCallStore.hold()
                }) { Text(if (held) "Resume" else "Hold") }
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
