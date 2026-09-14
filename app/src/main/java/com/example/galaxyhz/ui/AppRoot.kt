package com.example.galaxyhz.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.galaxyhz.theme.Bg0
import com.example.galaxyhz.theme.Bg1
import com.example.galaxyhz.ui.components.NavigationDrawerItemStyled
import com.example.galaxyhz.ui.components.LoadingOverlay
import com.example.galaxyhz.ui.screens.AdaptiveScreen
import com.example.galaxyhz.ui.screens.CustomRateScreen
import com.example.galaxyhz.ui.screens.HomeScreen
import com.example.galaxyhz.ui.screens.OverclockScreen
import com.example.galaxyhz.ui.screens.ResolutionScreen
import com.example.galaxyhz.ui.screens.SettingsScreen
import com.example.galaxyhz.ui.screens.SetupScreen
import com.example.galaxyhz.ui.screens.ToolsScreen
import kotlinx.coroutines.launch

private val destIcons = mapOf(
    Dest.HOME to Icons.Filled.Home,
    Dest.RESOLUTION to Icons.Filled.GridView,
    Dest.CUSTOM_RATE to Icons.Filled.Speed,
    Dest.ADAPTIVE to Icons.Filled.Bolt,
    Dest.OVERCLOCK to Icons.Filled.RocketLaunch,
    Dest.TOOLS to Icons.Filled.Build,
    Dest.SETTINGS to Icons.Filled.Settings
)

private fun destTitle(dest: Dest, lang: String): String = when (dest) {
    Dest.HOME -> L10n.t("home", lang)
    Dest.RESOLUTION -> L10n.t("resolution", lang)
    Dest.CUSTOM_RATE -> L10n.t("custom_rate", lang)
    Dest.ADAPTIVE -> L10n.t("adaptive", lang)
    Dest.OVERCLOCK -> L10n.t("overclock", lang)
    Dest.TOOLS -> L10n.t("tools", lang)
    Dest.SETTINGS -> L10n.t("settings", lang)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppRoot(viewModel: GalaxyHzViewModel = viewModel()) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val status by viewModel.status.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val setupDone by viewModel.setupDone.collectAsState()
    val dest by viewModel.selectedDest.collectAsState()
    val lang = viewModel.language

    if (!setupDone) {
        SetupScreen(
            hasRoot = status.hasRoot,
            lang = lang,
            onOpenMagisk = { viewModel.openMagiskThread() },
            onContinue = { viewModel.finishSetup() }
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Bg1) {
                Column(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        "GalaxyHz",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        status.deviceTitle.ifBlank { "Samsung display" },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Dest.entries.forEach { d ->
                    NavigationDrawerItemStyled(
                        label = destTitle(d, lang),
                        icon = destIcons[d]!!,
                        selected = dest == d,
                        onClick = {
                            viewModel.selectDest(d)
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            destTitle(dest, lang),
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectDest(Dest.SETTINGS) }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg0)
                )
            },
            containerColor = Bg0
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    when (dest) {
                        Dest.HOME -> HomeScreen(viewModel)
                        Dest.RESOLUTION -> ResolutionScreen(viewModel)
                        Dest.CUSTOM_RATE -> CustomRateScreen(viewModel)
                        Dest.ADAPTIVE -> AdaptiveScreen(viewModel)
                        Dest.OVERCLOCK -> OverclockScreen(viewModel)
                        Dest.TOOLS -> ToolsScreen(viewModel)
                        Dest.SETTINGS -> SettingsScreen(viewModel)
                    }
                    Spacer(Modifier.height(24.dp))
                }
                LoadingOverlay(busy)
            }
        }
    }
}
