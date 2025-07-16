package com.kling.waic.service

import com.kling.waic.entity.Task
import com.kling.waic.entity.TaskType
import com.kling.waic.external.KlingOpenAPIClient
import com.kling.waic.repository.CodeGenerateRepository
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import redis.clients.jedis.Jedis

@Service
class VideoTaskService(
    private val klingOpenAPIClient: KlingOpenAPIClient,
    private val styleImagePrompts: List<String>,
    private val codeGenerateRepository: CodeGenerateRepository,
    private val jedis: Jedis
) : TaskService {

    override suspend fun createTask(type: TaskType, file: MultipartFile): Task {
        TODO("Not yet implemented")
    }

    override suspend fun queryTask(
        type: TaskType,
        name: String
    ): Task {
        TODO("Not yet implemented")
    }

}