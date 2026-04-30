package com.qualitytool.app.model

import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val category: TaskCategory = TaskCategory.FEATURE,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val deadline: Long? = null,
    val completedAt: Long? = null
)

enum class TaskCategory(val label: String, val emoji: String) {
    FEATURE("功能", "✨"),
    BUG("缺陷", "🐛"),
    REFACTOR("重构", "🔧"),
    UI("界面", "🎨"),
    DOCS("文档", "📄"),
    OTHER("其他", "📌")
}

enum class TaskPriority(val label: String) {
    HIGH("高"),
    MEDIUM("中"),
    LOW("低")
}
