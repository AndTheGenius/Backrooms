package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.AppDatabase
import com.example.data.BackroomsRepository
import com.example.ui.BackroomsViewModel
import com.example.ui.screens.ArchiveScreen
import com.example.ui.screens.BackroomsPlayScreen
import com.example.ui.screens.CodexScreen
import com.example.ui.screens.TapeDeckScreen
import com.example.ui.theme.CrtVoid
import com.example.ui.theme.DampOchre
import com.example.ui.theme.FluorescentYellow
import com.example.ui.theme.LevelZeroTheme
import com.example.ui.theme.MutedTan
import com.example.ui.theme.TapeHousing
import com.example.ui.theme.TapeWhite

const val ROUTE_TAPE_DECK = "tape_deck"
const val ROUTE_PLAY = "play"
const val ROUTE_CODEX = "codex"
const val ROUTE_ARCHIVE = "archive"

private data class TopNavItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topNavItems = listOf(
    TopNavItem(ROUTE_TAPE_DECK, R.string.nav_deck, Icons.Filled.Videocam, Icons.Outlined.Videocam, "nav_tape_deck"),
    TopNavItem(ROUTE_CODEX, R.string.nav_codex, Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "nav_codex"),
    TopNavItem(ROUTE_ARCHIVE, R.string.nav_archive, Icons.Filled.History, Icons.Outlined.History, "nav_archive")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        val db = AppDatabase.getInstance(applicationContext)
        val repository = BackroomsRepository(db.backroomsDao())

        setContent {
            LevelZeroTheme {
                val viewModel: BackroomsViewModel = viewModel(
                    factory = BackroomsViewModel.Factory(repository)
                )
                LevelZeroApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun LevelZeroApp(viewModel: BackroomsViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_TAPE_DECK

    val careerStats by viewModel.careerStats.collectAsStateWithLifecycle()
    val selectedBiome by viewModel.selectedBiome.collectAsStateWithLifecycle()
    val discoveries by viewModel.discoveriesMap.collectAsStateWithLifecycle()
    val runsByDistance by viewModel.runsByDistance.collectAsStateWithLifecycle()
    val runsByRecent by viewModel.runsByRecent.collectAsStateWithLifecycle()

    val showChrome = currentRoute != ROUTE_PLAY

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CrtVoid)
    ) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = CrtVoid,
            bottomBar = {
                if (showChrome && !useNavRail) {
                    NavigationBar(
                        containerColor = TapeHousing,
                        contentColor = TapeWhite
                    ) {
                        topNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(item.labelRes),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = FluorescentYellow,
                                    selectedTextColor = FluorescentYellow,
                                    indicatorColor = DampOchre.copy(alpha = 0.55f),
                                    unselectedIconColor = MutedTan,
                                    unselectedTextColor = MutedTan
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize()) {
                if (showChrome && useNavRail) {
                    NavigationRail(
                        containerColor = TapeHousing,
                        contentColor = TapeWhite,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        topNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(item.labelRes),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = FluorescentYellow,
                                    selectedTextColor = FluorescentYellow,
                                    indicatorColor = DampOchre.copy(alpha = 0.55f),
                                    unselectedIconColor = MutedTan,
                                    unselectedTextColor = MutedTan
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_TAPE_DECK,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(if (showChrome) innerPadding else androidx.compose.foundation.layout.PaddingValues(0.dp))
                ) {
                    composable(ROUTE_TAPE_DECK) {
                        TapeDeckScreen(
                            careerStats = careerStats,
                            selectedBiome = selectedBiome,
                            onSelectBiome = { viewModel.selectStartingBiome(it) },
                            onInsertTape = {
                                viewModel.startNewRun(selectedBiome)
                                navController.navigate(ROUTE_PLAY)
                            }
                        )
                    }
                    composable(ROUTE_PLAY) {
                        BackroomsPlayScreen(
                            viewModel = viewModel,
                            onExitToDeck = {
                                navController.popBackStack(ROUTE_TAPE_DECK, inclusive = false)
                            },
                            onOpenCodex = {
                                navController.navigate(ROUTE_CODEX) {
                                    popUpTo(ROUTE_TAPE_DECK) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable(ROUTE_CODEX) {
                        CodexScreen(discoveries = discoveries)
                    }
                    composable(ROUTE_ARCHIVE) {
                        ArchiveScreen(
                            runsByDistance = runsByDistance,
                            runsByRecent = runsByRecent,
                            onClearArchive = { viewModel.clearArchive() }
                        )
                    }
                }
            }
        }
    }
}
