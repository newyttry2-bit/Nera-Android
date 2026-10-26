package com.virex.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.virex.music.data.AppDatabase
import com.virex.music.data.Song
import com.virex.music.player.PlayerViewModel
import com.virex.music.ui.screens.AboutScreen
import com.virex.music.ui.screens.DownloadsScreen
import com.virex.music.ui.screens.FullPlayerScreen
import com.virex.music.ui.screens.HomeScreen
import com.virex.music.ui.screens.MiniPlayer
import com.virex.music.ui.screens.PlaylistsScreen
import com.virex.music.ui.screens.SearchScreen
import com.virex.music.ui.theme.VirexMusicTheme

private enum class Tab(val route: String, val label: String) {
    HOME("home", "Home"),
    SEARCH("search", "Search"),
    PLAYLISTS("playlists", "Playlists"),
    DOWNLOADS("downloads", "Downloads"),
    ABOUT("about", "About")
}

class MainActivity : ComponentActivity() {
    private val player: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.get(this)
        setContent {
            VirexMusicTheme {
                App(player, db)
            }
        }
    }
}

@Composable
fun App(player: PlayerViewModel, db: AppDatabase) {
    val nav = rememberNavController()
    var pendingSong by remember { mutableStateOf<Song?>(null) }
    var showFullPlayer by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            Column {
                MiniPlayer(player = player, onExpand = { showFullPlayer = true })
                NavigationBar {
                    val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route
                    Tab.values().forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                showFullPlayer = false
                                nav.navigate(tab.route) {
                                    launchSingleTop = true
                                    popUpTo(nav.graph.startDestinationId)
                                }
                            },
                            icon = { Text(tab.label.first().toString()) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (showFullPlayer) {
                FullPlayerScreen(player)
            } else {
                NavHost(navController = nav, startDestination = Tab.HOME.route) {
                    composable(Tab.HOME.route) {
                        HomeScreen(player) { nav.navigate(Tab.DOWNLOADS.route) }
                    }
                    composable(Tab.SEARCH.route) {
                        SearchScreen(player) { song ->
                            pendingSong = song
                            nav.navigate(Tab.PLAYLISTS.route)
                        }
                    }
                    composable(Tab.PLAYLISTS.route) {
                        PlaylistsScreen(db, player, pendingSong) { pendingSong = null }
                    }
                    composable(Tab.DOWNLOADS.route) { DownloadsScreen(player) }
                    composable(Tab.ABOUT.route) { AboutScreen() }
                }
            }
        }
    }
}
