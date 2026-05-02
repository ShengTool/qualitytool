package com.qualitytool.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qualitytool.app.model.TaskCategory
import com.qualitytool.app.model.TaskPriority
import com.qualitytool.app.ui.TaskViewModel
import com.qualitytool.app.ui.components.BarChart
import com.qualitytool.app.ui.components.BarItem
import com.qualitytool.app.ui.components.PieChart
import com.qualitytool.app.ui.components.PieSlice

private val catColor = mapOf(
    TaskCategory.FEATURE to Color(0xFF7C4DFF),
    TaskCategory.BUG to Color(0xFFE53935),
    TaskCategory.REFACTOR to Color(0xFF00897B),
    TaskCategory.UI to Color(0xFF5C6BC0),
    TaskCategory.DOCS to Color(0xFF43A047),
    TaskCategory.OTHER to Color(0xFF78909C)
)
private val priColor = mapOf(
    TaskPriority.HIGH to Color(0xFFE53935),
    TaskPriority.MEDIUM to Color(0xFFFB8C00),
    TaskPriority.LOW to Color(0xFF43A047)
)

@Composable
fun StatsScreen(vm: TaskViewModel) {
    val tasks by vm.tasks.collectAsState()
    val catStats by vm.categoryStats.collectAsState()
    val priStats by vm.priorityStats.collectAsState()

    val completed = tasks.count { it.isCompleted }
    val active = tasks.count { !it.isCompleted }
    val overdue = tasks.count {
        it.deadline != null && it.deadline!! < System.currentTimeMillis() && !it.isCompleted
    }
    val rate = if (tasks.isEmpty()) 0f else completed.toFloat() / tasks.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("📊 任务统计", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        // Summary cards
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("总计", "${tasks.size}", Icons.Default.CheckCircle, Color(0xFF5C6BC0), Modifier.weight(1f))
            StatCard("已完成", "$completed", Icons.Default.CheckCircle, Color(0xFF43A047), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("进行中", "$active", Icons.Default.PlayArrow, Color(0xFFFB8C00), Modifier.weight(1f))
            StatCard("已过期", "$overdue", Icons.Default.Warning, Color(0xFFE53935), Modifier.weight(1f))
        }

        // Completion rate
        Card(
            Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("完成率", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("${(rate * 100).toInt()}%", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = Color(0xFF43A047))
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { rate },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Color(0xFF43A047), trackColor = MaterialTheme.colorScheme.surfaceVariant)
            }
        }

        // Category pie chart
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(14.dp)) {
                Text("分类占比", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                val slices = TaskCategory.entries
                    .filter { (catStats[it] ?: 0) > 0 }
                    .map { cat -> PieSlice("${cat.emoji} ${cat.label}", (catStats[cat] ?: 0).toFloat(), catColor[cat]!!) }
                PieChart(slices, Modifier.fillMaxWidth())
            }
        }

        // Priority bar chart
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(14.dp)) {
                Text("优先级分布", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                val bars = TaskPriority.entries.map { pri ->
                    BarItem(pri.label, priStats[pri] ?: 0, priColor[pri]!!)
                }
                BarChart(bars, Modifier.fillMaxWidth())
            }
        }

        // Category detail
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(14.dp)) {
                Text("分类明细", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                TaskCategory.entries.forEach { cat ->
                    val count = catStats[cat] ?: 0
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(6.dp),
                                color = catColor[cat]!!.copy(alpha = 0.15f), modifier = Modifier.size(32.dp)) {
                                Box(contentAlignment = Alignment.Center) { Text(cat.emoji) }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(cat.label, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("$count 项", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(color.copy(alpha = 0.08f))) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)) }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
