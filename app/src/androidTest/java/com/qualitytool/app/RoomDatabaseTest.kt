package com.qualitytool.app

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.qualitytool.app.data.AppDatabase
import com.qualitytool.app.data.TaskEntity
import com.qualitytool.app.model.Task
import com.qualitytool.app.model.TaskCategory
import com.qualitytool.app.model.TaskPriority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndQuery() = runBlocking {
        val entity = TaskEntity(
            id = "1", title = "Test", description = "Desc",
            category = TaskCategory.FEATURE, priority = TaskPriority.MEDIUM,
            isCompleted = false, createdAt = 1000L,
            deadline = null, completedAt = null, sortOrder = 0
        )
        db.taskDao().insertTask(entity)

        val tasks = db.taskDao().getAllTasks().first()
        assertEquals(1, tasks.size)
        assertEquals("Test", tasks[0].title)
        assertEquals(TaskCategory.FEATURE, tasks[0].category)
    }

    @Test
    fun updateTask() = runBlocking {
        db.taskDao().insertTask(TaskEntity(
            id = "1", title = "Original", description = "",
            category = TaskCategory.BUG, priority = TaskPriority.HIGH,
            isCompleted = false, createdAt = 1000L,
            deadline = null, completedAt = null, sortOrder = 0
        ))
        db.taskDao().updateTask(TaskEntity(
            id = "1", title = "Updated", description = "",
            category = TaskCategory.BUG, priority = TaskPriority.HIGH,
            isCompleted = false, createdAt = 1000L,
            deadline = null, completedAt = null, sortOrder = 0
        ))
        val tasks = db.taskDao().getAllTasks().first()
        assertEquals("Updated", tasks[0].title)
    }

    @Test
    fun deleteTask() = runBlocking {
        db.taskDao().insertTask(TaskEntity(
            id = "1", title = "Test", description = "",
            category = TaskCategory.FEATURE, priority = TaskPriority.MEDIUM,
            isCompleted = false, createdAt = 1000L,
            deadline = null, completedAt = null, sortOrder = 0
        ))
        db.taskDao().deleteTask("1")
        val tasks = db.taskDao().getAllTasks().first()
        assertTrue(tasks.isEmpty())
    }

    @Test
    fun completeAllTasks() = runBlocking {
        db.taskDao().insertTask(TaskEntity(
            id = "1", title = "A", description = "",
            category = TaskCategory.FEATURE, priority = TaskPriority.MEDIUM,
            isCompleted = false, createdAt = 1000L,
            deadline = null, completedAt = null, sortOrder = 0
        ))
        db.taskDao().insertTask(TaskEntity(
            id = "2", title = "B", description = "",
            category = TaskCategory.BUG, priority = TaskPriority.HIGH,
            isCompleted = true, createdAt = 2000L,
            deadline = null, completedAt = 3000L, sortOrder = 1
        ))
        db.taskDao().completeAllTasks(5000L)

        val tasks = db.taskDao().getAllTasks().first()
        assertTrue(tasks.all { it.isCompleted })
    }

    @Test
    fun entityToTaskConversion() {
        val entity = TaskEntity(
            id = "123", title = "Convert", description = "Hello",
            category = TaskCategory.UI, priority = TaskPriority.LOW,
            isCompleted = true, createdAt = 999L,
            deadline = 888L, completedAt = 777L, sortOrder = 5
        )
        val task = entity.toTask()
        assertEquals("123", task.id)
        assertEquals("Convert", task.title)
        assertEquals("Hello", task.description)
        assertEquals(TaskCategory.UI, task.category)
        assertEquals(TaskPriority.LOW, task.priority)
        assertTrue(task.isCompleted)
        assertEquals(999L, task.createdAt)
        assertEquals(888L, task.deadline)
        assertEquals(777L, task.completedAt)
    }

    @Test
    fun taskToEntityConversion() {
        val task = Task(
            id = "abc", title = "FromTask", description = "",
            category = TaskCategory.DOCS, priority = TaskPriority.HIGH,
            isCompleted = false, createdAt = 111L,
            deadline = null, completedAt = null
        )
        val entity = TaskEntity.fromTask(task, sortOrder = 3)
        assertEquals("abc", entity.id)
        assertEquals("FromTask", entity.title)
        assertEquals(TaskCategory.DOCS, entity.category)
        assertEquals(TaskPriority.HIGH, entity.priority)
        assertEquals(3, entity.sortOrder)
        assertNull(entity.deadline)
    }
}
