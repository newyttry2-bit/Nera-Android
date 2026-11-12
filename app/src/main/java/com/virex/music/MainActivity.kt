package com.virex.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.virex.music.data.AppDatabase
import com.virex.music.player.PlayerViewModel
import com.virex.music.ui.components.BottomDestinations
import com.virex.music.ui.components.MiniPlayer
import com.virex.music.ui.components.VirexBottomBar
import com.virex.music.ui.screens.FilesScreen
import com.virex.music.ui.screens.HomeScreen
import com.virex.music.ui.screens.LibraryScreen
import com.virex.music.ui.screens.LyricsScreen
import com.virex.music.ui.screens.PlayerScreen
import com.virex.music.ui.screens.PlaylistDetailScreen
import com.virex.music.ui.screens.SearchScreen
import com.virex.music.ui.screens.SettingsScreen
import com.virex.music.ui.theme.VirexColors
import com.virex.music.ui.theme.VirexMusicTheme

class MainActivity : ComponentActivity() {

    private val player: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.get(this)
        setContent {
            val pureBlack by player.settings.pureBlack.collectAsState()
            VirexMusicTheme(pureBlack = pureBlack) {
                VirexApp(player = player, db = db)
            }
        }
    }
}

@Composable
fun VirexApp(player: PlayerViewModel, db: AppDatabase) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"
    val currentSong by player.current.collectAsState()
    var playerExpanded by remember { mutableStateOf(false) }
    var lyricsOpen by remember { mutableStateOf(false) }

    val mainRoutes = BottomDestinations.map { it.route }
    val showBottomBar = currentRoute in mainRoutes

    BackHandler(enabled = playerExpanded) {
        playerExpanded = false
    }
    // Registered after the player handler, so back closes the lyrics
    // page first while it is on top of the player.
    BackHandler(enabled = lyricsOpen) {
        lyricsOpen = false
    }

    Box(Modifier.fillMaxSize().background(VirexColors.Black)) {
        Scaffold(
            containerColor = VirexColors.Black,
            bottomBar = {
                Column {
                    if (currentSong != null && !playerExpanded) {
                        MiniPlayer(
                            player = player,
                            onExpand = { playerExpanded = true }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    if (showBottomBar) {
                        VirexBottomBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(padding)
            ) {
                composable("home") {
                    HomeScreen(
                        player = player,
                        db = db,
                        onOpenSettings = { navController.navigate("settings") },
                        onOpenFiles = { navController.navigate("files") }
                    )
                }
                composable("search") {
                    SearchScreen(player = player, db = db)
                }
                composable("library") {
                    LibraryScreen(
                        player = player,
                        db = db,
                        onOpenPlaylist = { id -> navController.navigate("playlist/$id") }
                    )
                }
                composable("files") {
                    FilesScreen(player = player)
                }
                composable(
                    route = "playlist/{playlistId}",
                    arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getLong("playlistId") ?: 0L
                    PlaylistDetailScreen(
                        playlistId = id,
                        player = player,
                        db = db,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        player = player,
                        db = db,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        // Full player expands from the mini-player with a spring slide.
        AnimatedVisibility(
            visible = playerExpanded && currentSong != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    stiffness = Spring.StiffnessMedium,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(
                    stiffness = Spring.StiffnessMedium,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            )
        ) {
            Box(Modifier.fillMaxSize().background(VirexColors.Black)) {
                PlayerScreen(
                    player = player,
                    onClose = { playerExpanded = false },
                    onOpenLyrics = { lyricsOpen = true }
                )
            }
        }

        // Full-screen lyrics page, sliding up over the player.
        AnimatedVisibility(
            visible = lyricsOpen && currentSong != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    stiffness = Spring.StiffnessMedium,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(
                    stiffness = Spring.StiffnessMedium,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            )
        ) {
            LyricsScreen(
                player = player,
                onClose = { lyricsOpen = false }
            )
        }
    }
}
