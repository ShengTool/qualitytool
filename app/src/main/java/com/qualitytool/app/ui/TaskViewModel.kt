package com.qualitytool.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qualitytool.app.data.TaskStorage
import com.qualitytool.app.model.Task
import com.qualitytool.app.model.TaskCategory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class TaskFilterStatus(val label: String) {
    ALL("全部"), ACTIVE("进行中"), COMPLETED("已完成")
}

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = TaskStorage(application)

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterCategory = MutableStateFlow<TaskCategory?>(null)
    val filterCategory: StateFlow<TaskCategory?> = _filterCategory.asStateFlow()

    private val _filterStatus = MutableStateFlow(TaskFilterStatus.ALL)
    val filterStatus: StateFlow<TaskFilterStatus> = _filterStatus.asStateFlow()

    val filteredTasks: StateFlow<List<Task>> = combine(
        _tasks, _searchQuery, _filterCategory, _filterStatus
    ) { tasks, query, category, status ->
        tasks.filter { task ->
            val matchesQuery = query.isBlank() ||
                    task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true)
            val matchesCategory = category == null || task.category == category
            val matchesStatus = when (status) {
                TaskFilterStatus.ALL -> true
                TaskFilterStatus.ACTIVE -> !task.isCompleted
                TaskFilterStatus.COMPLETED -> task.isCompleted
            }
            matchesQuery && matchesCategory && matchesStatus
        }.sortedWith(
            compareBy<Task> { it.isCompleted }
                .thenByDescending { it.priority.ordinal }
                .thenByDescending { it.createdAt }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completionProgress: StateFlow<Float> = _tasks.map { list ->
        if (list.isEmpty()) 0f else list.count { it.isCompleted }.toFloat() / list.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    init { loadTasks() }

    private fun loadTasks() { _tasks.value = storage.loadTasks() }

    fun addTask(task: Task) { _tasks.update { current -> current + task }; saveTasks() }

    fun updateTask(updated: Task) {
        _tasks.update { current -> current.map { if (it.id == updated.id) updated else it } }
        saveTasks()
    }

    fun toggleTask(taskId: String) {
        _tasks.update { current ->
            current.map {
                if (it.id == taskId) it.copy(
                    isCompleted = !it.isCompleted,
                    completedAt = if (!it.isCompleted) System.currentTimeMillis() else null
                ) else it
            }
        }
        saveTasks()
    }

    fun deleteTask(taskId: String) {
        _tasks.update { current -> current.filter { it.id != taskId } }
        saveTasks()
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setFilterCategory(category: TaskCategory?) { _filterCategory.value = category }
    fun setFilterStatus(status: TaskFilterStatus) { _filterStatus.value = status }

    private fun saveTasks() {
        viewModelScope.launch { storage.saveTasks(_tasks.value) }
    }

    operator fun invoke(): TaskViewModel {
        return this

    }
}
