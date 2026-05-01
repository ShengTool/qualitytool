package com.qualitytool.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qualitytool.app.data.TaskStorage
import com.qualitytool.app.model.Task
import com.qualitytool.app.model.TaskCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class TaskFilterStatus(val label: String) {
    ALL("全部"), ACTIVE("进行中"), COMPLETED("已完成")
}

enum class SortOption(val label: String) {
    DEFAULT("默认"),
    PRIORITY_DESC("优先级↓"),
    PRIORITY_ASC("优先级↑"),
    DATE_DESC("最新↓"),
    DATE_ASC("最早↑"),
    NAME("名称")
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

    private val _sortOption = MutableStateFlow(SortOption.DEFAULT)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _darkTheme = MutableStateFlow<Boolean?>(null)
    val darkTheme: StateFlow<Boolean?> = _darkTheme.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    val filteredTasks: StateFlow<List<Task>> = combine(
        _tasks, _searchQuery, _filterCategory, _filterStatus, _sortOption
    ) { tasks, query, category, status, sort ->
        val filtered = tasks.filter { task ->
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
        }
        when (sort) {
            SortOption.DEFAULT -> filtered.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenByDescending { it.priority.ordinal }
                    .thenByDescending { it.createdAt }
            )
            SortOption.PRIORITY_DESC -> filtered.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenByDescending { it.priority.ordinal }
            )
            SortOption.PRIORITY_ASC -> filtered.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenBy { it.priority.ordinal }
            )
            SortOption.DATE_DESC -> filtered.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenByDescending { it.createdAt }
            )
            SortOption.DATE_ASC -> filtered.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenBy { it.createdAt }
            )
            SortOption.NAME -> filtered.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenBy { it.title.lowercase() }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completionProgress: StateFlow<Float> = _tasks.map { list ->
        if (list.isEmpty()) 0f else list.count { it.isCompleted }.toFloat() / list.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val categoryStats: StateFlow<Map<TaskCategory, Int>> = _tasks.map { list ->
        TaskCategory.entries.associateWith { cat -> list.count { it.category == cat } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _tasks.value = storage.loadTasks()
            } catch (e: Exception) {
                _errorMessage.emit("加载任务失败: ${e.message}")
            }
        }
    }

    fun addTask(task: Task) { _tasks.update { it + task }; saveTasks() }

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
        viewModelScope.launch(Dispatchers.IO) {
            try { storage.deleteTask(taskId) } catch (e: Exception) {
                _errorMessage.emit("删除失败: ${e.message}")
            }
        }
    }

    fun completeAllTasks() {
        _tasks.update { current ->
            val now = System.currentTimeMillis()
            current.map { if (!it.isCompleted) it.copy(isCompleted = true, completedAt = now) else it }
        }
        viewModelScope.launch(Dispatchers.IO) {
            try { storage.completeAllTasks() } catch (e: Exception) {
                _errorMessage.emit("操作失败: ${e.message}")
            }
        }
    }

    fun deleteCompletedTasks() {
        _tasks.update { current -> current.filter { !it.isCompleted } }
        viewModelScope.launch(Dispatchers.IO) {
            try { storage.deleteCompletedTasks() } catch (e: Exception) {
                _errorMessage.emit("删除失败: ${e.message}")
            }
        }
    }

    fun moveTaskUp(taskId: String) {
        _tasks.update { current ->
            val index = current.indexOfFirst { it.id == taskId }
            if (index <= 0) current
            else current.toMutableList().also { it.add(index - 1, it.removeAt(index)) }
        }
        saveTasks()
    }

    fun moveTaskDown(taskId: String) {
        _tasks.update { current ->
            val index = current.indexOfFirst { it.id == taskId }
            if (index < 0 || index >= current.lastIndex) current
            else current.toMutableList().also { it.add(index + 1, it.removeAt(index)) }
        }
        saveTasks()
    }

    fun exportTasks(): String = storage.exportTasksJson(_tasks.value)

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setFilterCategory(category: TaskCategory?) { _filterCategory.value = category }
    fun setFilterStatus(status: TaskFilterStatus) { _filterStatus.value = status }
    fun setSortOption(option: SortOption) { _sortOption.value = option }

    fun setDarkTheme(enabled: Boolean?) {
        _darkTheme.value = enabled
        saveTasks()
    }

    private fun saveTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            try { storage.saveTasks(_tasks.value) } catch (e: Exception) {
                _errorMessage.emit("保存任务失败: ${e.message}")
            }
        }
    }
}
