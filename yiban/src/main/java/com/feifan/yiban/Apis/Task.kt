package com.feifan.yiban.Apis


import android.content.Context
import com.feifan.yiban.Core.SchoolBasedAuth
import com.feifan.yiban.Core.TaskFeedback
import com.feifan.yiban.Core.BaseReq

class Task(context: Context) {
    private lateinit var taskFeedback: TaskFeedback
    private val req = BaseReq(context)

    fun init(mobile: String, password: String): Boolean {
        try {
            val auth = SchoolBasedAuth(req)
            auth.auth(mobile, password)
            // 初始化任务反馈模块
            taskFeedback = TaskFeedback(req)
            return true
        } catch (e: Exception) {
            throw e
        }
    }

    fun submitSignFeedback(): String {
        return taskFeedback.submitSign()
    }

    fun getSignTask(): Map<String, Any> {
        return taskFeedback.getSignTask()
    }
}

