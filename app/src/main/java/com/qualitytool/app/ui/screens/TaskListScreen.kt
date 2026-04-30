package com.qualitytool.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qualitytool.app.model.Task
import com.qualitytool.app.model.TaskCategory
import com.qualitytool.app.model.TaskPriority
import com.qualitytool.app.ui.TaskFilterStatus
import com.qualitytool.app.ui.TaskViewModel
import com.qualitytool.app.ui.components.TaskEditDialog
import java.text.SimpleDateFormat
import java.util.*

private val priorityColor = mapOf(
    TaskPriority.HIGH to Color(0xFFE53935),
    TaskPriority.MEDIUM to Color(0xFFFB8C00),
    TaskPriority.LOW to Color(0xFF43A047)
)
private val categoryColor = mapOf(
    TaskCategory.FEATURE to Color(0xFF7C4DFF),
    TaskCategory.BUG to Color(0xFFE53935),
    TaskCategory.REFACTOR to Color(0xFF00897B),
    TaskCategory.UI to Color(0xFF5C6BC0),
    TaskCategory.DOCS to Color(0xFF43A047),
    TaskCategory.OTHER to Color(0xFF78909C)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(vm: TaskViewModel) {
    val tasks by vm.filteredTasks.collectAsState()
    val allTasks by vm.tasks.collectAsState()
    val filterCategory by vm.filterCategory.collectAsState()
    val filterStatus by vm.filterStatus.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val progress by vm.completionProgress.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎯 开发任务清单", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Default.Add, "添加") }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // --- Progress Card ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("任务进度", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(2.dp))
                        Text("${allTasks.count { it.isCompleted }} / ${allTasks.size} 已完成",
                            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                        CircularProgressIndicator(
                            progress = { progress }, modifier = Modifier.fillMaxSize(),
                            strokeWidth = 4.dp, trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text("${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            // --- Search ---
            OutlinedTextField(
                value = searchQuery, onValueChange = { vm.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(), placeholder = { Text("搜索任务...") },
                leadingIcon = { Icon(Icons.Default.Search, "搜索") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty())
                        IconButton(onClick = { vm.setSearchQuery("") }) { Icon(Icons.Default.Close, "清除") }
                },
                singleLine = true, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
            )

            Spacer(Modifier.height(10.dp))

            // --- Category Chips ---
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = filterCategory == null,
                    onClick = { vm.setFilterCategory(null) },
                    label = { Text("全部", style = MaterialTheme.typography.labelSmall) })
                TaskCategory.entries.forEach { cat ->
                    FilterChip(selected = filterCategory == cat,
                        onClick = { vm.setFilterCategory(if (filterCategory == cat) null else cat) },
                        label = { Text("${cat.emoji} ${cat.label}", style = MaterialTheme.typography.labelSmall) })
                }
            }

            Spacer(Modifier.height(6.dp))

            // --- Status Chips ---
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TaskFilterStatus.entries.forEach { s ->
                    FilterChip(selected = filterStatus == s,
                        onClick = { vm.setFilterStatus(s) },
                        label = { Text(s.label, style = MaterialTheme.typography.labelSmall) })
                }
            }

            Spacer(Modifier.height(10.dp))

            // --- Content ---
            when {
                allTasks.isEmpty() -> EmptyPlaceholder { showDialog = true }
                tasks.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("没有匹配的任务", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggle = { vm.toggleTask(task.id) },
                            onEdit = { editingTask = task }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showDialog || editingTask != null) {
        TaskEditDialog(
            task = editingTask,
            onDismiss = { showDialog = false; editingTask = null },
            onSave = { t ->
                if (editingTask != null) vm.updateTask(t) else vm.addTask(t)
                showDialog = false; editingTask = null
            }
        )
    }
}

@Composable
private fun TaskCard(task: Task, onToggle: () -> Unit, onEdit: () -> Unit) {
    val fmt = remember { SimpleDateFormat("M/d HH:mm", Locale.getDefault()) }
    val pc = priorityColor[task.priority] ?: Color.Gray
    val cc = categoryColor[task.category] ?: Color.Gray

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted, onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = pc, uncheckedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                // Title with strikethrough animation
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.SemiBold
                        ),
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                    )
                }
                if (task.description.isNotBlank())
                    Text(task.description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(6.dp), color = cc.copy(alpha = 0.12f)) {
                        Text("${task.category.emoji} ${task.category.label}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall, color = cc)
                    }
                    Box(Modifier.size(8.dp).clip(CircleShape).background(pc))
                    Text(task.priority.label, style = MaterialTheme.typography.labelSmall, color = pc)
                    Text(fmt.format(Date(task.createdAt)), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
            }
            // Edit icon
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, "编辑", modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun ColumnScope.EmptyPlaceholder(onAddClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📋", fontSize = 56.sp)
            Spacer(Modifier.height(12.dp))
            Text("还没有开发任务", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text("点击下方按钮添加第一个任务", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(20.dp))
            OutlinedButton(onClick = onAddClick, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("添加任务")
            }
        }
    }
}
