package com.qualitytool.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.qualitytool.app.model.Task
import java.io.File

class TaskStorage(private val context: Context) {

    private val gson = Gson()

    private val storageFile: File
        get() = File(context.filesDir, "tasks.json")

    fun loadTasks(): List<Task> {
        return try {
            if (storageFile.exists()) {
                val json = storageFile.readText()
                val type = object : TypeToken<List<Task>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveTasks(tasks: List<Task>) {
        try {
            val json = gson.toJson(tasks)
            storageFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
