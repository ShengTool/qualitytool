package com.qualitytool.app

import com.qualitytool.app.model.Task
import com.qualitytool.app.model.TaskCategory
import com.qualitytool.app.model.TaskPriority
import com.qualitytool.app.ui.SortOption
import com.qualitytool.app.ui.TaskFilterStatus
import org.junit.Assert.*
import org.junit.Test

class TaskModelTest {

    @Test
    fun task_creationWithDefaults() {
        val task = Task(title = "Test", description = "Desc")
        assertTrue(task.id.isNotEmpty())
        assertEquals("Test", task.title)
        assertEquals("Desc", task.description)
        assertEquals(TaskCategory.FEATURE, task.category)
        assertEquals(TaskPriority.MEDIUM, task.priority)
        assertFalse(task.isCompleted)
        assertNull(task.deadline)
        assertNull(task.completedAt)
    }

    @Test
    fun task_copyPreservesId() {
        val task = Task(title = "Original")
        val updated = task.copy(title = "Updated", isCompleted = true)
        assertEquals(task.id, updated.id)
        assertEquals("Updated", updated.title)
        assertTrue(updated.isCompleted)
    }

    @Test
    fun taskCategory_hasAllEntries() {
        assertEquals(6, TaskCategory.entries.size)
        assertTrue(TaskCategory.entries.any { it.label == "功能" })
        assertTrue(TaskCategory.entries.any { it.label == "缺陷" })
    }

    @Test
    fun taskPriority_order() {
        assertEquals(0, TaskPriority.HIGH.ordinal)
        assertEquals(1, TaskPriority.MEDIUM.ordinal)
        assertEquals(2, TaskPriority.LOW.ordinal)
    }

    @Test
    fun taskFilterStatus_allEntriesPresent() {
        assertEquals(3, TaskFilterStatus.entries.size)
    }

    @Test
    fun sortOption_allEntriesPresent() {
        assertTrue(SortOption.entries.size >= 5)
        assertTrue(SortOption.entries.any { it == SortOption.DEFAULT })
        assertTrue(SortOption.entries.any { it == SortOption.NAME })
    }

    @Test
    fun task_toggleCompleted() {
        val task = Task(title = "Toggle")
        assertFalse(task.isCompleted)
        val done = task.copy(
            isCompleted = true,
            completedAt = System.currentTimeMillis()
        )
        assertTrue(done.isCompleted)
        assertNotNull(done.completedAt)
    }
}
