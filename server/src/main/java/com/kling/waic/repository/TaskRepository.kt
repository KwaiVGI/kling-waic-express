package com.kling.waic.repository

import com.kling.waic.entity.Task
import com.kling.waic.utils.ObjectMapperUtils
import com.kling.waic.utils.Slf4j.Companion.log
import org.springframework.stereotype.Repository
import redis.clients.jedis.commands.JedisCommands

@Repository
class TaskRepository(
    private val jedis: JedisCommands,
) {
    fun setTask(task: Task): String {
        val taskValue = ObjectMapperUtils.toJSON(task)
        val result = jedis.set(task.name, taskValue)
        log.info("Set task in Redis with name: ${task.name}, value: $taskValue, result: $result")
        return result
    }

    fun getTask(taskName: String): Task? {
        val taskValue = jedis.get(taskName)
        return ObjectMapperUtils.fromJSON(taskValue, Task::class.java)
    }
}