package com.truerandom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truerandom.ui.main.MainScreenState
import com.truerandom.ui.main.MainViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import truerandomwindows.composeapp.generated.resources.Res
import truerandomwindows.composeapp.generated.resources.app_name
import truerandomwindows.composeapp.generated.resources.no_track_playing
import truerandomwindows.composeapp.generated.resources.unknown_track

@Composable
@Preview
fun App(mainViewModel: MainViewModel) {
    val state by mainViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        mainViewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    MaterialTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = { TopBar(state) },
            bottomBar = { PlayerBottomBar(mainViewModel, state) }
        ) { paddingValues ->
            // Liked tracks paged list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), // Important: respects topBar/bottomBar height
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
//                items(state.tracks) { track ->
//                    TrackRow(track)
//                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(state: MainScreenState) {
    Column {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(Res.string.app_name),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // Top progress bar (show if ANY state is loading)
        if (state.isLoading) {
            val loadPercentage = state.tracks.data

            if (loadPercentage != null) {
                LinearProgressIndicator(
                    progress = { loadPercentage },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun PlayerBottomBar(mainViewModel: MainViewModel, state: MainScreenState) {
    Surface(
        tonalElevation = 8.dp, // Separation from the list
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        val scope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally, // Centers everything horizontally
            verticalArrangement = Arrangement.spacedBy(8.dp)   // Space between text and buttons
        ) {
            // Top Part: Artist - Track
            Text(
                text = with (state.currentTrackDetails) {
                    if (this != null) {
                        "[$playCount] $artistName — $trackName"
                    } else {
                        if (state.isPlaying) {
                            stringResource(Res.string.unknown_track)
                        } else {
                            stringResource(Res.string.no_track_playing)
                        }
                    }
                },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Bottom Part: Row of 3 Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp) // Space between icons
            ) {
                // TODO: JAY_LOG - use as logout for now
                IconButton(onClick = {
                    mainViewModel.logoutBtnOnClick()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Previous")
                }

                // Play/Pause usually looks better slightly larger
                FilledIconButton(
                    onClick = {
                        mainViewModel.playPauseBtnOnClick()
                    },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Toggle Play"
                    )
                }

                // TODO: JAY_LOG - use as resync for now
                IconButton(onClick = {
                    scope.launch {
                        mainViewModel.fetchAndSyncLikedTracks()
                    }
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Next")
                }
            }
        }
    }
}