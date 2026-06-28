package com.sonar.app.ui.screens.result

import android.media.MediaPlayer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sonar.domain.entities.Recording
import com.sonar.domain.entities.Solution
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    recordingId: String,
    onBack: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(recordingId) {
        viewModel.loadRecording(UUID.fromString(recordingId))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分析结果") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.recording?.analysisResult != null -> {
                    AnalysisContent(
                        recording = uiState.recording!!,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Text(
                        text = "暂无分析结果",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalysisContent(
    recording: Recording,
    modifier: Modifier = Modifier
) {
    val result = recording.analysisResult ?: return

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Audio playback section
        recording.filePath?.let { path ->
            item {
                AudioPlaybackCard(filePath = path)
            }
        }

        item {
            SectionCard(
                title = "核心问题",
                icon = Icons.Default.QuestionMark,
                iconTint = MaterialTheme.colorScheme.error
            ) {
                Text(
                    text = result.coreQuestion,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        item {
            SectionCard(
                title = "背景上下文",
                icon = Icons.Default.Info,
                iconTint = MaterialTheme.colorScheme.secondary
            ) {
                Text(
                    text = result.background,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Text(
                text = "解决方案",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(result.solutions) { solution ->
            SolutionCard(solution = solution)
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SolutionCard(solution: Solution) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = solution.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = solution.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            solution.url?.let { url ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Audio playback card for recorded audio files.
 */
@Composable
private fun AudioPlaybackCard(filePath: String) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPrepared by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Initialize MediaPlayer asynchronously
    LaunchedEffect(filePath) {
        try {
            val player = MediaPlayer().apply {
                setDataSource(filePath)
                setOnPreparedListener {
                    isPrepared = true
                }
                setOnCompletionListener {
                    isPlaying = false
                }
                setOnErrorListener { _, what, extra ->
                    errorMessage = "播放失败 (what=$what, extra=$extra)"
                    isPlaying = false
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            errorMessage = "无法加载音频文件: ${e.message}"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    errorMessage?.let { return@clickable }
                    if (!isPrepared) return@clickable

                    if (isPlaying) {
                        mediaPlayer?.pause()
                        isPlaying = false
                    } else {
                        val player = mediaPlayer
                        if (player != null) {
                            if (player.isPlaying) {
                                player.pause()
                                isPlaying = false
                            } else {
                                player.seekTo(player.currentPosition)
                                player.start()
                                isPlaying = true
                            }
                        }
                    }
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                errorMessage != null -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "播放失败",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = errorMessage ?: "播放失败",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                !isPrepared -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "准备音频...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isPlaying) "正在播放录音..." else "点击播放录音",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (filePath.isNotBlank()) {
                            Text(
                                text = filePath.substringAfterLast("/"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
}
