package com.qualitytool.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qualitytool.app.model.Task
import com.qualitytool.app.model.TaskCategory
import com.qualitytool.app.model.TaskPriority
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditDialog(
    task: Task?,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit
) {
    var title by remember(task) { mutableStateOf(task?.title ?: "") }
    var description by remember(task) { mutableStateOf(task?.description ?: "") }
    var category by remember(task) { mutableStateOf(task?.category ?: TaskCategory.FEATURE) }
    var priority by remember(task) { mutableStateOf(task?.priority ?: TaskPriority.MEDIUM) }
    var deadline by remember(task) { mutableStateOf(task?.deadline) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val isEdit = task != null

    val dateFmt = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (isEdit) "编辑任务" else "新建任务", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "关闭") }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("任务标题") }, placeholder = { Text("输入任务标题...") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("任务描述（可选）") }, placeholder = { Text("输入任务描述...") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = "${category.emoji} ${category.label}", onValueChange = {},
                            readOnly = true, label = { Text("分类") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp), singleLine = true
                        )
                        ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                            TaskCategory.entries.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text("${cat.emoji} ${cat.label}") },
                                    onClick = { category = cat; categoryExpanded = false }
                                )
                            }
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = priorityExpanded, onExpandedChange = { priorityExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = priority.label, onValueChange = {},
                            readOnly = true, label = { Text("优先级") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(priorityExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp), singleLine = true
                        )
                        ExposedDropdownMenu(expanded = priorityExpanded, onDismissRequest = { priorityExpanded = false }) {
                            TaskPriority.entries.forEach { pri ->
                                DropdownMenuItem(
                                    text = { Text(pri.label) },
                                    onClick = { priority = pri; priorityExpanded = false }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = if (deadline != null) dateFmt.format(Date(deadline!!)) else "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("截止日期（可选）") },
                    placeholder = { Text("点击选择截止日期") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, "选择日期")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp), singleLine = true,
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        disabledTrailingIconColor = MaterialTheme.colorScheme.primary
                    )
                )
                if (deadline != null) {
                    TextButton(
                        onClick = { deadline = null },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text("清除截止日期", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(if (isEdit) task!!.copy(
                            title = title, description = description,
                            category = category, priority = priority, deadline = deadline
                        )
                        else Task(
                            title = title, description = description,
                            category = category, priority = priority, deadline = deadline
                        ))
                    }
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) { Text(if (isEdit) "保存" else "创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = deadline ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    deadline = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
