package com.truerandom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.truerandom.model.LikedTrackWithCount
import com.truerandom.ui.common.ConfirmationDialog
import com.truerandom.ui.common.DialogEvent
import com.truerandom.ui.main.MainScreenState
import com.truerandom.ui.main.MainViewModel
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import org.jetbrains.compose.resources.stringResource
import truerandomwindows.composeapp.generated.resources.Res
import truerandomwindows.composeapp.generated.resources.app_name
import truerandomwindows.composeapp.generated.resources.logout
import truerandomwindows.composeapp.generated.resources.no_track_playing
import truerandomwindows.composeapp.generated.resources.re_sync
import truerandomwindows.composeapp.generated.resources.reset_play_counts_title
import truerandomwindows.composeapp.generated.resources.unknown_track

@Composable
@Preview
fun App(mainViewModel: MainViewModel, appVersionName: String, appVersionCode: String) {
    val state by mainViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmDialogState by remember { mutableStateOf(DialogEvent(false)) }

    val pagedTracks = mainViewModel.pagedLikedTracks.collectAsLazyPagingItems()
    val pagedListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        launch {
            mainViewModel.showSnackBarEvent.collect { message ->
                snackbarHostState.showSnackbar(message)
            }
        }

        launch {
            mainViewModel.showConfirmDialogEvent.collect { event ->
                println("show confirm dialog event = $event")
                confirmDialogState = event
            }
        }
    }

    MaterialTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = { TopBar(mainViewModel, state) },
            bottomBar = {
                val versionString = "v$appVersionName($appVersionCode)"
                PlayerBottomBar(mainViewModel, state, versionString)
            }
        ) { paddingValues ->
            // Liked tracks paged list
            LazyColumnScrollbar(
                state = pagedListState,
                settings = ScrollbarSettings.Default,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                LazyColumn(
                    state = pagedListState,
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = pagedTracks.itemCount,
                        key = pagedTracks.itemKey { it.trackUri }
                    ) { index ->
                        val track = pagedTracks[index]
                        if (track != null) {
                            TrackRow(track = track)
                        }
                    }
                }
            }
        }

        // 2-btn confirmation dialog
        val showDialog = confirmDialogState.showDialog

        ConfirmationDialog(
            DialogEvent(
                showDialog = showDialog,
                title = confirmDialogState.title,
                desc = confirmDialogState.desc,
                confirmLabel = confirmDialogState.confirmLabel,
                onConfirm = confirmDialogState.onConfirm,
                dismissLabel = confirmDialogState.dismissLabel,
                onDismiss = confirmDialogState.onDismiss
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(mainViewModel: MainViewModel, state: MainScreenState) {
    val scope = rememberCoroutineScope()

    var menuExpanded by remember { mutableStateOf(false) }

    Column {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(Res.string.app_name),
                    style = MaterialTheme.typography.titleLarge
                )
            },

            // Dropdown actions (re-sync, logout, etc)
            actions = {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        // Re-sync liked tracks
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.re_sync)) },
                            onClick = {
                                menuExpanded = false
                                scope.launch { mainViewModel.fetchAndSyncLikedTracks() }
                            }
                        )

                        // Reset play count
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.reset_play_counts_title)) },
                            onClick = {
                                menuExpanded = false
                                mainViewModel.resetPlayCountOnClick()
                            }
                        )

                        // Logout
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.logout)) },
                            onClick = {
                                menuExpanded = false
                                scope.launch { mainViewModel.logoutBtnOnClick() }
                            }
                        )
                    }
                }
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
fun PlayerBottomBar(mainViewModel: MainViewModel, state: MainScreenState, versionString: String) {
    Surface(
        tonalElevation = 8.dp, // Separation from the list
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally, // Centers everything horizontally
            verticalArrangement = Arrangement.spacedBy(8.dp)   // Space between text and buttons
        ) {
            // Top Part: Artist - Track
            Text(
                text = with(state.currentTrackDetails) {
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

            // Mid Part: Row of 3 Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp) // Space between icons
            ) {
                // Previous btn
                FilledTonalIconButton(
                    onClick = {
                        // TODO: JAY_LOG - Previous btn function (full list of previous tracks??)
                    },
                    modifier = Modifier.size(48.dp) // Slightly smaller than the 56.dp Play/Pause button helps hierarchy
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous"
                    )
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

                // Next - just play next random track
                FilledTonalIconButton(
                    onClick = {
                        mainViewModel.nextBtnOnClick()
                    },
                    modifier = Modifier.size(48.dp) // Slightly smaller than the 56.dp Play/Pause button helps hierarchy
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next"
                    )
                }
            }

            // Btm part: version
            Text(
                text = "TrueRandom $versionString",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

// Paged tracks list
@Composable
private fun TrackRow(track: LikedTrackWithCount) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = track.trackName ?: "-",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artistName ?: "-",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "${track.playCount}",
            style = MaterialTheme.typography.labelLarge
        )
    }
}