package com.qualitytool.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qualitytool.app.ui.TaskViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: TaskViewModel) {
    val darkTheme by vm.darkTheme.collectAsState()
    val notificationEnabled by vm.notificationEnabled.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearDialog by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                    if (json.isBlank()) {
                        snackbarHostState.showSnackbar("文件为空", duration = SnackbarDuration.Short)
                        return@launch
                    }
                    vm.importTasksFromJson(json)
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("读取失败: ${e.message}", duration = SnackbarDuration.Short)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        launch {
            vm.snackbarEvent.collect { event ->
                snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
            }
        }
        launch {
            vm.errorMessage.collect { msg ->
                snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("⚙️ 设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            // ---- Appearance ----
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.FavoriteBorder, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("外观", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(12.dp))

                    listOf(
                        Triple(null, "跟随系统", "自动切换深色/浅色"),
                        Triple(false, "浅色模式", "始终使用浅色主题"),
                        Triple(true, "深色模式", "始终使用深色主题")
                    ).forEach { (value, label, desc) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                                Text(desc, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            RadioButton(
                                selected = darkTheme == value,
                                onClick = { vm.setDarkTheme(value) }
                            )
                        }
                    }
                }
            }

            // ---- Notifications ----
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("通知", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("截止日期提醒", style = MaterialTheme.typography.bodyMedium)
                            Text("任务到期时发送通知", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = notificationEnabled, onCheckedChange = { vm.setNotificationEnabled(it) })
                    }
                }
            }

            // ---- Data ----
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Build, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("数据管理", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(12.dp))

                    SettingButton("导入 JSON", "从文件导入任务数据", Icons.Outlined.AddCircle) {
                        importLauncher.launch(arrayOf("application/json", "*/*"))
                    }

                    SettingButton("导出 JSON", "将任务导出为 JSON 文件", Icons.Outlined.Share) {
                        val json = vm.exportTasks()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_TEXT, json)
                        }
                        context.startActivity(Intent.createChooser(intent, "导出任务 JSON"))
                    }

                    SettingButton("清空所有数据", "删除全部任务（不可恢复）", Icons.Outlined.Delete,
                        color = MaterialTheme.colorScheme.error) {
                        showClearDialog = true
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            shape = RoundedCornerShape(20.dp),
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("确认清空") },
            text = { Text("确定要删除所有任务数据吗？此操作不可恢复。") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.clearAllData()
                        showClearDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("所有数据已清除", duration = SnackbarDuration.Short)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)
                ) { Text("确认清空") }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun SettingButton(title: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
                          color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
                          onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.bodyMedium, color = color)
                    Text(desc, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp))
        }
    }
}
