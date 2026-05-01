package com.qualitytool.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.qualitytool.app.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class TaskStorage(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).taskDao()
    private val gson = Gson()
    private val jsonFile: File get() = File(context.filesDir, "tasks.json")

    suspend fun loadTasks(): List<Task> = withContext(Dispatchers.IO) {
        val entities = withContext(Dispatchers.IO) {
            var snapshot: List<TaskEntity> = emptyList()
            dao.getAllTasks().collect { snapshot = it; return@collect }
            snapshot
        }
        if (entities.isEmpty()) migrateFromJson()
        entities.map { it.toTask() }
    }

    fun observeTasks(): Flow<List<Task>> = dao.getAllTasks().map { list ->
        list.map { it.toTask() }
    }

    suspend fun insertTask(task: Task) {
        val entity = TaskEntity.fromTask(task)
        dao.insertTask(entity)
    }

    suspend fun updateTask(task: Task) {
        val entity = TaskEntity.fromTask(task)
        dao.updateTask(entity)
    }

    suspend fun saveTasks(tasks: List<Task>) {
        val entities = tasks.mapIndexed { index, task ->
            TaskEntity.fromTask(task, sortOrder = index)
        }
        dao.insertTasks(entities)
    }

    suspend fun deleteTask(id: String) {
        dao.deleteTask(id)
    }

    suspend fun deleteCompletedTasks() {
        dao.deleteCompletedTasks()
    }

    suspend fun completeAllTasks() {
        dao.completeAllTasks(System.currentTimeMillis())
    }

    fun exportTasksJson(tasks: List<Task>): String = gson.toJson(tasks)

    private suspend fun migrateFromJson() {
        if (!jsonFile.exists()) return
        try {
            val json = jsonFile.readText()
            val type = object : TypeToken<List<Task>>() {}.type
            val tasks: List<Task> = gson.fromJson(json, type) ?: emptyList()
            saveTasks(tasks)
            jsonFile.delete()
        } catch (_: Exception) {}
    }
}
