package com.example.galaxyhz.ui.main

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.galaxyhz.data.DisplayStatus
import com.example.galaxyhz.data.HzMode
import com.example.galaxyhz.data.RefreshRateManager
import com.example.galaxyhz.util.RootHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _status = MutableStateFlow(DisplayStatus())
    val status: StateFlow<DisplayStatus> = _status.asStateFlow()

    private val _lastLog = MutableStateFlow("Ready.")
    val lastLog: StateFlow<String> = _lastLog.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun refreshStatus(context: Context) {
        viewModelScope.launch { _status.value = RefreshRateManager.getStatus(context) }
    }

    fun applyHz(context: Context, mode: HzMode) {
        viewModelScope.launch {
            _isLoading.value = true
            _lastLog.value = "Applying ${mode.hz} Hz mode..."
            val res = RefreshRateManager.applyRefreshRate(mode.hz)
            _lastLog.value =
                if (res.isSuccess) "Mode ${mode.hz} Hz applied successfully!"
                else "Error: ${res.stderr.ifBlank { res.stdout }}"
            delay(500)
            refreshStatus(context)
            _isLoading.value = false
        }
    }

    fun applyAntiFlicker(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _lastLog.value = "Applying anti-flicker SurfaceFlinger fixes..."
            val res = RefreshRateManager.applyAntiFlickerFix()
            _lastLog.value =
                if (res.isSuccess) "Idle timers disabled - anti-flicker active!"
                else "Error: ${res.stderr.ifBlank { res.stdout }}"
            delay(500)
            refreshStatus(context)
            _isLoading.value = false
        }
    }

    fun toggleAOD(context: Context, enable: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _lastLog.value = if (enable) "Enabling Always-On Display..." else "Disabling AOD (flicker-safe)..."
            RefreshRateManager.toggleAlwaysOnDisplay(enable)
            delay(500)
            refreshStatus(context)
            _isLoading.value = false
        }
    }

    fun resetPanel(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _lastLog.value = "Power-cycling the display panel..."
            RefreshRateManager.resetDisplayPanel()
            delay(300)
            refreshStatus(context)
            _isLoading.value = false
        }
    }

    fun repairMode(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _lastLog.value = "Re-applying full mode configuration (all layers)..."
            val rate = RefreshRateManager.getStatus(context).targetMode.hz
            val res = RootHelper.runCommands(
                listOf(
                    "cmd display set-match-content-frame-rate-pref 2",
                    *RefreshRateManager.applyRefreshRateCommands(rate).toTypedArray()
                )
            )
            _lastLog.value =
                if (res.isSuccess) "Mode ${rate} Hz re-applied on all layers!"
                else "Error: ${res.stderr.ifBlank { res.stdout }}"
            delay(500)
            refreshStatus(context)
            _isLoading.value = false
        }
    }
}

private val NeonCyan = Color(0xFF00E5FF)
private val NeonEmerald = Color(0xFF00E676)
private val NeonAmber = Color(0xFFFFB300)
private val DarkSurface = Color(0xFF12141A)
private val CardBackground = Color(0xFF1E222D)
private val AppBackground = Color(0xFF0A0C10)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier, viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val status by viewModel.status.collectAsState()
    val lastLog by viewModel.lastLog.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refreshStatus(context)
            delay(2000)
        }
    }

    val activeColor by animateColorAsState(
        targetValue = when (status.targetMode) {
            HzMode.H120 -> NeonCyan
            HzMode.H96 -> NeonEmerald
            HzMode.H60 -> NeonAmber
        },
        animationSpec = tween(400),
        label = "activeColor"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "GalaxyHz",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = Color.White
                        )
                        Text(
                            status.deviceModel.ifBlank { "Samsung Galaxy S20" } + " - forced refresh rates",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (status.hasRoot) Color(0xFF1B5E20) else Color(0xFFB71C1C))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            if (status.hasRoot) "ROOT OK" else "NO ROOT",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = AppBackground
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(status, activeColor)
            SectionHeader("REFRESH RATE")

            HzOptionCard(
                title = "120 Hz - Ultra Smooth",
                subtitle = "Maximum fluidity for gaming and scrolling. Highest battery drain.",
                details = "1080x2400 @ 120.00 Hz (panel 120HS)",
                isSelected = status.targetMode == HzMode.H120,
                accentColor = NeonCyan,
                enabled = !isLoading,
                onClick = { viewModel.applyHz(context, HzMode.H120) }
            )
            HzOptionCard(
                title = "96 Hz - Eco Smooth (recommended)",
                subtitle = "Feels as smooth as 120 Hz with far less heat and +30% battery life.",
                details = "1080x2400 @ 96.00 Hz (panel 96HS)",
                isSelected = status.targetMode == HzMode.H96,
                accentColor = NeonEmerald,
                enabled = !isLoading,
                onClick = { viewModel.applyHz(context, HzMode.H96) }
            )
            HzOptionCard(
                title = "60 Hz - Battery Saver",
                subtitle = "Standard rate for maximum standby and screen-on time.",
                details = "1080x2400 @ 60.00 Hz (panel 60NS)",
                isSelected = status.targetMode == HzMode.H60,
                accentColor = NeonAmber,
                enabled = !isLoading,
                onClick = { viewModel.applyHz(context, HzMode.H60) }
            )

            SectionHeader("ADAPTIVE BEHAVIOR")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SettingRow(
                        title = "Lock refresh rate (disable adaptive)",
                        subtitle = "Prevents the system from lowering FPS when content is static. Strongly recommended to avoid flicker.",
                        control = {
                            Switch(
                                checked = status.antiFlickerActive,
                                onCheckedChange = { viewModel.applyAntiFlicker(context) },
                                enabled = !isLoading
                            )
                        }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    SettingRow(
                        title = "Always-On Display (AOD)",
                        subtitle = if (status.isAodEnabled) "AOD is ON - known flicker source on S20 OLED" else "AOD is OFF (recommended)",
                        subtitleColor = if (status.isAodEnabled) Color(0xFFFF8A80) else NeonEmerald,
                        control = {
                            Switch(
                                checked = status.isAodEnabled,
                                onCheckedChange = { viewModel.toggleAOD(context, it) },
                                enabled = !isLoading
                            )
                        }
                    )
                }
            }

            SectionHeader("FLICKER PROTECTION")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "If the screen shows static, sparkles or glitch bands: apply the fixes below in order. The panel reset usually clears artifacts instantly.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    FixRow(
                        title = "1. Apply anti-flicker props",
                        subtitle = "Disables SurfaceFlinger idle/touch timers (root required)",
                        enabled = !isLoading,
                        onClick = { viewModel.applyAntiFlicker(context) },
                        buttonText = "Apply"
                    )
                    FixRow(
                        title = "2. Repair mode on all layers",
                        subtitle = "Re-writes DisplayManager, settings, wm and panel mode",
                        enabled = !isLoading,
                        onClick = { viewModel.repairMode(context) },
                        buttonText = "Repair"
                    )
                    FixRow(
                        title = "3. Emergency panel reset",
                        subtitle = "Power-cycles the display DDIC (screen blinks once)",
                        enabled = !isLoading,
                        onClick = { viewModel.resetPanel(context) },
                        buttonText = "Reset DDI"
                    )
                }
            }

            InfoCard(status)

            LogCard(lastLog)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatusCard(status: DisplayStatus, activeColor: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, activeColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "ACTIVE REFRESH RATE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        if (status.currentFps > 0) "${status.currentFps.toInt()}" else "--",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = activeColor
                    )
                    Text(
                        " Hz",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Panel: ${status.panelMode}",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
                Text(
                    "Resolution: ${status.resolution}",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
            }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(activeColor.copy(alpha = 0.35f), Color.Transparent)
                        )
                    )
                    .border(2.dp, activeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${status.targetMode.hz}",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun HzOptionCard(
    title: String,
    subtitle: String,
    details: String,
    isSelected: Boolean,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFF1E2838) else Color(0xFF161A22)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) accentColor else Color(0xFF262C38),
                RoundedCornerShape(16.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isSelected) accentColor else Color.White
                )
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = Color.LightGray)
                Spacer(Modifier.height(4.dp))
                Text(details, fontSize = 11.sp, color = Color.Gray)
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                enabled = enabled,
                colors = RadioButtonDefaults.colors(
                    selectedColor = accentColor,
                    unselectedColor = Color.Gray
                )
            )
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    subtitleColor: Color = Color.Gray,
    control: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = subtitleColor)
        }
        control()
    }
}

@Composable
private fun FixRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    buttonText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(buttonText, fontSize = 12.sp, color = Color.White)
        }
    }
}

@Composable
private fun InfoCard(status: DisplayStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "ABOUT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 1.sp
            )
            Text(
                "Forces 120/96/60 Hz on Galaxy S20-series devices (S20 / S20+ / S20 Ultra, Exynos x1s and Snapdragon y2s) on AOSP ROMs without Samsung's One UI refresh-rate service. Requires root (Magisk).",
                fontSize = 12.sp,
                color = Color.LightGray
            )
            Text(
                "Tip: the Quick Settings tile cycles 120 -> 96 -> 60 Hz without opening this app.",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun LogCard(lastLog: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1117))
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "OUTPUT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Spacer(Modifier.height(4.dp))
            Text(
                lastLog,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = if (lastLog.contains("Error", true)) Color(0xFFFF5252) else NeonEmerald
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        letterSpacing = 1.sp
    )
}
