package com.kling.waic.component.repository

import com.kling.waic.component.entity.Task
import com.kling.waic.component.utils.ObjectMapperUtils
import com.kling.waic.component.utils.Slf4j.Companion.log
import org.springframework.stereotype.Repository
import redis.clients.jedis.commands.JedisCommands

@Repository
class TaskRepository(
    private val jedis: JedisCommands,
) {
    fun setTask(task: Task): String {
        val taskValue = ObjectMapperUtils.toJSON(task)
        val result = jedis.set(task.name, taskValue)
        log.debug("Set task in Redis with name: ${task.name}, value: $taskValue, result: $result")
        return result
    }

    fun getTask(taskName: String): Task? {
        val taskValue = jedis.get(taskName)
        return ObjectMapperUtils.fromJSON(taskValue, Task::class.java)
    }
}