package com.kling.waic.repositories

import com.kling.waic.entities.TaskType
import org.springframework.stereotype.Repository
import redis.clients.jedis.Jedis

@Repository
class CodeGenerateRepository(
    private val jedis: Jedis,
    private val nextCodeLuaScript: String
) {

    fun nextCode(taskType: TaskType, prefix: String = "No."): String {
        val key = "kling-waic:${taskType.name}"
//        val step = Random.nextInt(2, 10)
        val step = 1
        val startValue = taskType.startValue

        val code = jedis.eval(
            nextCodeLuaScript,
            listOf(key), listOf(step.toString(), startValue.toString())
        )
        return prefix + code
    }
}