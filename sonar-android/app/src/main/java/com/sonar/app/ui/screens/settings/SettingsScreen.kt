package com.sonar.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showWhisperKey by remember { mutableStateOf(false) }
    var showGeminiKey by remember { mutableStateOf(false) }
    var showSearchKey by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.saveSettings() }) {
                        Text("保存", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // API Keys section
            Text(
                text = "API 密钥",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "设置 API Key 后系统将使用真实服务，留空则使用 Mock 数据。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            // Whisper API Key
            OutlinedTextField(
                value = uiState.whisperApiKey,
                onValueChange = { viewModel.updateWhisperKey(it) },
                label = { Text("Whisper API Key") },
                placeholder = { Text("sk-... (OpenAI)") },
                leadingIcon = { Icon(Icons.Default.Mic, contentDescription = null) },
                visualTransformation = if (showWhisperKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showWhisperKey = !showWhisperKey }) {
                        Icon(
                            imageVector = if (showWhisperKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Gemini API Key
            OutlinedTextField(
                value = uiState.geminiApiKey,
                onValueChange = { viewModel.updateGeminiKey(it) },
                label = { Text("Gemini API Key") },
                placeholder = { Text("AIza...") },
                leadingIcon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                        Icon(
                            imageVector = if (showGeminiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Search API Key
            OutlinedTextField(
                value = uiState.searchApiKey,
                onValueChange = { viewModel.updateSearchKey(it) },
                label = { Text("Search API Key") },
                placeholder = { Text("SerpAPI / Google Search") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                visualTransformation = if (showSearchKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showSearchKey = !showSearchKey }) {
                        Icon(
                            imageVector = if (showSearchKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Save button
            Button(
                onClick = { viewModel.saveSettings() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.saved
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (uiState.saved) "已保存" else "保存设置")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About section
            Text(
                text = "关于 Sonar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Sonar v0.1.0 — 录音对话分析工具\n录音 → 转文字 → AI 分析 → 搜索解决方案 → 输出报告",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
