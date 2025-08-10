package com.kling.waic.component.repository

import com.kling.waic.component.entity.TaskType
import org.springframework.stereotype.Repository
import redis.clients.jedis.commands.JedisCommands

@Repository
class CodeGenerateRepository(
    private val jedis: JedisCommands,
    private val nextCodeLuaScript: String
) {

    fun nextCode(taskType: TaskType, prefix: String = "No."): String {
        val key = "kling-waic:${taskType.name}"
//        val step = Random.nextInt(2, 10)
        val startValue = taskType.startValue

        val code = jedis.eval(
            nextCodeLuaScript,
            listOf(key), listOf(startValue.toString())
        )
        return prefix + code
    }
}