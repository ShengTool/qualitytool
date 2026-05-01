package com.qualitytool.app.ui.screens

import android.content.Intent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qualitytool.app.model.Task
import com.qualitytool.app.model.TaskCategory
import com.qualitytool.app.model.TaskPriority
import com.qualitytool.app.ui.SortOption
import com.qualitytool.app.ui.TaskFilterStatus
import com.qualitytool.app.ui.TaskViewModel
import com.qualitytool.app.ui.components.TaskEditDialog
import kotlinx.coroutines.launch
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
    val sortOption by vm.sortOption.collectAsState()
    val progress by vm.completionProgress.collectAsState()
    val categoryStats by vm.categoryStats.collectAsState()
    val darkTheme by vm.darkTheme.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var viewingTask by remember { mutableStateOf<Task?>(null) }
    var showBatchMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        launch {
            vm.errorMessage.collect { msg ->
                snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            }
        }
        launch {
            vm.snackbarEvent.collect { event ->
                val result = snackbarHostState.showSnackbar(
                    event.message, event.actionLabel, duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    when (event.undoAction) {
                        "complete" -> vm.undoCompleteAllTasks()
                        "deleteCompleted" -> vm.undoDeleteCompletedTasks()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("\uD83C\uDFAF 开发任务清单", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                actions = {
                    IconButton(onClick = {
                        vm.setDarkTheme(when (darkTheme) {
                            null -> false; false -> true; true -> null
                        })
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Settings,
                                contentDescription = "主题切换",
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                when (darkTheme) {
                                    true -> "深色"
                                    false -> "浅色"
                                    null -> "自动"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Box {                    IconButton(onClick = { showBatchMenu = true }) {
                        Icon(Icons.Outlined.MoreVert, "更多操作")
                    }
                        DropdownMenu(expanded = showBatchMenu, onDismissRequest = { showBatchMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("一键完成全部") },
                                onClick = { showBatchMenu = false; vm.completeAllTasks() },
                                leadingIcon = { Icon(Icons.Outlined.Check, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("删除已完成") },
                                onClick = { showBatchMenu = false; vm.deleteCompletedTasks() },
                                leadingIcon = { Icon(Icons.Outlined.Delete, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("导出任务") },
                                onClick = {
                                    showBatchMenu = false
                                    val json = vm.exportTasks()
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_TEXT, json)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "导出任务"))
                                },
                                leadingIcon = { Icon(Icons.Outlined.Share, null) }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Default.Add, "添加") }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

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

            Spacer(Modifier.height(8.dp))

            if (allTasks.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("分类统计", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        CategoryStatsBar(categoryStats, allTasks.size)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

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

            Spacer(Modifier.height(8.dp))

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

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TaskFilterStatus.entries.forEach { s ->
                    FilterChip(selected = filterStatus == s,
                        onClick = { vm.setFilterStatus(s) },
                        label = { Text(s.label, style = MaterialTheme.typography.labelSmall) })
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SortOption.entries.forEach { opt ->
                    FilterChip(selected = sortOption == opt,
                        onClick = { vm.setSortOption(opt) },
                        label = { Text(opt.label, style = MaterialTheme.typography.labelSmall) })
                }
            }

            Spacer(Modifier.height(8.dp))

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
                        val taskIndex = tasks.indexOfFirst { it.id == task.id }
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    taskToDelete = task; false
                                } else false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val color by animateColorAsState(
                                    targetValue = MaterialTheme.colorScheme.errorContainer,
                                    label = "dismissBg"
                                )
                                Box(
                                    Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                                        .background(color).padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(Icons.Default.Delete, "删除",
                                        tint = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        ) {
                            TaskCard(
                                task = task,
                                onToggle = { vm.toggleTask(task.id) },
                                onEdit = { editingTask = task },
                                onDelete = { taskToDelete = task },
                                onClick = { viewingTask = task },
                                onMoveUp = if (taskIndex > 0) ({ vm.moveTaskUp(task.id) }) else null,
                                onMoveDown = if (taskIndex < tasks.lastIndex) ({ vm.moveTaskDown(task.id) }) else null
                            )
                        }
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

    if (taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            shape = RoundedCornerShape(20.dp),
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("确认删除") },
            text = { Text("确定要删除任务「${taskToDelete!!.title}」吗？此操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        val deleted = taskToDelete!!
                        vm.deleteTask(deleted.id)
                        taskToDelete = null
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                "已删除「${deleted.title}」",
                                actionLabel = "撤销",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                vm.addTask(deleted)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { taskToDelete = null }) { Text("取消") } }
        )
    }

    if (viewingTask != null) {
        TaskDetailDialog(task = viewingTask!!, onDismiss = { viewingTask = null })
    }
}

@Composable
private fun CategoryStatsBar(stats: Map<TaskCategory, Int>, total: Int) {
    val maxVal = stats.values.maxOrNull() ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TaskCategory.entries.forEach { cat ->
            val count = stats[cat] ?: 0
            val cc = categoryColor[cat] ?: Color.Gray
            val fraction = if (total > 0) count.toFloat() / total else 0f
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${cat.emoji} ${cat.label}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(56.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .clip(RoundedCornerShape(4.dp))
                            .background(cc.copy(alpha = 0.6f))
                    )
                }
                Text("$count",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(24.dp).padding(start = 4.dp))
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    val fmt = remember { SimpleDateFormat("M/d HH:mm", Locale.getDefault()) }
    val deadlineFmt = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
    val pc = priorityColor[task.priority] ?: Color.Gray
    val cc = categoryColor[task.category] ?: Color.Gray

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(24.dp)
            ) {
                if (onMoveUp != null)
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Filled.KeyboardArrowUp, "上移",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    }
                if (onMoveDown != null)
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Filled.KeyboardArrowDown, "下移",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    }
            }
            Checkbox(
                checked = task.isCompleted, onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = pc, uncheckedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
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
                    if (task.deadline != null) {
                        Icon(Icons.Outlined.DateRange, "截止日期",
                            modifier = Modifier.size(14.dp),
                            tint = if (task.deadline!! < System.currentTimeMillis() && !task.isCompleted)
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Text(deadlineFmt.format(Date(task.deadline!!)),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (task.deadline!! < System.currentTimeMillis() && !task.isCompleted)
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, "编辑", modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, "删除", modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun TaskDetailDialog(task: Task, onDismiss: () -> Unit) {
    val fmt = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val pc = priorityColor[task.priority] ?: Color.Gray
    val cc = categoryColor[task.category] ?: Color.Gray

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(task.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "关闭") }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(6.dp), color = cc.copy(alpha = 0.15f)) {
                        Text("${task.category.emoji} ${task.category.label}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium, color = cc)
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = pc.copy(alpha = 0.15f)) {
                        Text(task.priority.label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium, color = pc)
                    }
                    if (task.isCompleted) {
                        Surface(shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF43A047).copy(alpha = 0.15f)) {
                            Text("已完成",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF43A047))
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                if (task.description.isNotBlank()) {
                    Text("描述", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(task.description, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("（无描述）", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                DetailRow("创建时间", fmt.format(Date(task.createdAt)))
                if (task.deadline != null) {
                    val overdue = task.deadline!! < System.currentTimeMillis() && !task.isCompleted
                    DetailRow("截止日期", fmt.format(Date(task.deadline!!)),
                        valueColor = if (overdue) MaterialTheme.colorScheme.error else null,
                        suffix = if (overdue) " \u26A0\uFE0F 已过期" else "")
                }
                if (task.completedAt != null) {
                    DetailRow("完成时间", fmt.format(Date(task.completedAt!!)))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color? = null, suffix: String = "") {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value + suffix, style = MaterialTheme.typography.bodySmall,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ColumnScope.EmptyPlaceholder(onAddClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("\uD83D\uDCCB", fontSize = 56.sp)
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
