package com.sonar.app.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.sonar.domain.entities.Recording
import com.sonar.domain.entities.RecordingStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onRecordingClick: (UUID) -> Unit,
    onAnalyzeClick: (UUID) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Permission launcher for RECORD_AUDIO
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startRecording()
        } else {
            viewModel.onEvent(HomeEvent.Error("需要录音权限才能使用录音功能"))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onEvent(HomeEvent.LoadRecordings)
    }

    // Show error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(HomeEvent.DismissError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sonar") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (uiState.currentRecordingId != null) {
                        viewModel.stopRecording()
                    } else {
                        if (ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            viewModel.startRecording()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                containerColor = if (uiState.currentRecordingId != null)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = if (uiState.currentRecordingId != null)
                        Icons.Default.Stop
                    else
                        Icons.Default.Mic,
                    contentDescription = if (uiState.currentRecordingId != null) "停止" else "开始"
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.recordings.isEmpty() && !uiState.isLoading) {
                EmptyState()
            } else {
                RecordingList(
                    recordings = uiState.recordings,
                    currentRecordingId = uiState.currentRecordingId,
                    currentDurationMs = uiState.currentDurationMs,
                    onItemClick = onRecordingClick,
                    onAnalyzeClick = onAnalyzeClick,
                    onDeleteClick = { viewModel.onEvent(HomeEvent.DeleteRecording(it)) }
                )
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无录音",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击下方按钮开始录音",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun RecordingList(
    recordings: List<Recording>,
    currentRecordingId: UUID?,
    currentDurationMs: Long,
    onItemClick: (UUID) -> Unit,
    onAnalyzeClick: (UUID) -> Unit,
    onDeleteClick: (UUID) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(recordings, key = { it.id }) { recording ->
            val liveDurationMs = if (recording.id == currentRecordingId) currentDurationMs else recording.duration
            RecordingItem(
                recording = recording,
                isRecording = recording.id == currentRecordingId,
                liveDurationMs = liveDurationMs,
                onClick = { onItemClick(recording.id) },
                onAnalyzeClick = { onAnalyzeClick(recording.id) },
                onDeleteClick = { onDeleteClick(recording.id) }
            )
        }
    }
}

@Composable
private fun RecordingItem(
    recording: Recording,
    isRecording: Boolean,
    liveDurationMs: Long,
    onClick: () -> Unit,
    onAnalyzeClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            Icon(
                imageVector = when (recording.status) {
                    RecordingStatus.RECORDING -> Icons.Default.Mic
                    RecordingStatus.STOPPED -> Icons.Default.MicOff
                    RecordingStatus.TRANSCRIBING -> Icons.Default.Audiotrack
                    RecordingStatus.TRANSCRIBED -> Icons.Default.TextFields
                    RecordingStatus.ANALYZING, RecordingStatus.SEARCHING -> Icons.Default.Analytics
                    RecordingStatus.COMPLETED -> Icons.Default.CheckCircle
                    RecordingStatus.FAILED -> Icons.Default.Error
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = null,
                tint = when (recording.status) {
                    RecordingStatus.RECORDING -> MaterialTheme.colorScheme.error
                    RecordingStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDuration(liveDurationMs),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatDate(recording.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                recording.analysisResult?.let { result ->
                    Text(
                        text = result.coreQuestion,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Actions
            if (recording.status == RecordingStatus.STOPPED || recording.status == RecordingStatus.TRANSCRIBED) {
                IconButton(onClick = onAnalyzeClick) {
                    Icon(Icons.Default.Analytics, contentDescription = "分析")
                }
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "删除")
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / 1000 / 60) % 60
    val hours = durationMs / 1000 / 60 / 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
