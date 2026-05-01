package com.qualitytool.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.qualitytool.app.model.Task
import com.qualitytool.app.model.TaskCategory
import com.qualitytool.app.model.TaskPriority

@Entity(tableName = "tasks")
@TypeConverters(TaskConverters::class)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: TaskCategory,
    val priority: TaskPriority,
    val isCompleted: Boolean,
    val createdAt: Long,
    val deadline: Long?,
    val completedAt: Long?,
    val sortOrder: Int = 0
) {
    fun toTask(): Task = Task(
        id = id,
        title = title,
        description = description,
        category = category,
        priority = priority,
        isCompleted = isCompleted,
        createdAt = createdAt,
        deadline = deadline,
        completedAt = completedAt
    )

    companion object {
        fun fromTask(task: Task, sortOrder: Int = 0): TaskEntity = TaskEntity(
            id = task.id,
            title = task.title,
            description = task.description,
            category = task.category,
            priority = task.priority,
            isCompleted = task.isCompleted,
            createdAt = task.createdAt,
            deadline = task.deadline,
            completedAt = task.completedAt,
            sortOrder = sortOrder
        )
    }
}

class TaskConverters {
    @TypeConverter
    fun fromCategory(category: TaskCategory): String = category.name

    @TypeConverter
    fun toCategory(value: String): TaskCategory = TaskCategory.valueOf(value)

    @TypeConverter
    fun fromPriority(priority: TaskPriority): String = priority.name

    @TypeConverter
    fun toPriority(value: String): TaskPriority = TaskPriority.valueOf(value)
}
