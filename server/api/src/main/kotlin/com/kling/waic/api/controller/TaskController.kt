package com.kling.waic.api.controller

import com.kling.waic.component.auth.Authorization
import com.kling.waic.component.auth.AuthorizationType
import com.kling.waic.component.entity.Locale
import com.kling.waic.component.entity.Printing
import com.kling.waic.component.entity.Result
import com.kling.waic.component.entity.Task
import com.kling.waic.component.entity.TaskNewInput
import com.kling.waic.component.entity.TaskType
import com.kling.waic.component.selector.TaskServiceSelector
import com.kling.waic.component.utils.CoroutineUtils
import com.kling.waic.component.utils.Slf4j.Companion.log
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/tasks")
class TaskController (
    private val taskServiceSelector: TaskServiceSelector,
) {

    @PostMapping("{type}/upload_image")
    @Authorization(AuthorizationType.CREATE_TASK)
    fun uploadImage(@PathVariable type: TaskType,
                    @RequestParam("file") file: MultipartFile): Result<String> {
        log.info("Upload image of type: $type, file.name: ${file.name}, " +
                "file.originalFilename: ${file.originalFilename}, " +
                "file.contentType: ${file.contentType}, " +
                "file.size: ${file.size}"
            )

        val url = taskServiceSelector.selectTaskService(type).uploadImage(type, file)

        log.debug("Uploaded image of type: {}, file: {}, url: {}", type, file.name, url)
        return Result(url)
    }

    @PostMapping("{type}/new")
    @Authorization(AuthorizationType.CREATE_TASK)
    fun newTask(@PathVariable type: TaskType,
                @RequestBody input: TaskNewInput
    ): Result<Task> {
        log.debug("Creating new task of type: {}, url: {}", type, input.url)

        val task = CoroutineUtils.runSuspend {
            taskServiceSelector.selectTaskService(type)
                .createTask(type, input.url)
        }

        log.debug("Created new task of type: {}, url: {}, task: {}", type, input.url, task)
        return Result(task)
    }

    @GetMapping("{type}/{name}/query")
    @Authorization(AuthorizationType.CREATE_TASK)
    fun queryTask(@PathVariable type: TaskType,
                  @PathVariable name: String,
                  @RequestParam(required = false, defaultValue = "CN") locale: Locale): Result<Task> {
        log.debug("Query task of type: {}, name: {}", type, name)

        val task = CoroutineUtils.runSuspend {
            taskServiceSelector.selectTaskService(type)
                .queryTask(type, name, locale)
        }

        log.debug("Query task with result, taskId: {}, taskStatus: {}", task.id, task.status)
        return Result(task)
    }

    @PostMapping("{type}/{name}/print")
    @Authorization(AuthorizationType.CREATE_TASK)
    fun printTask(
        @PathVariable type: TaskType,
        @PathVariable name: String): Result<Printing> {

        val printing = taskServiceSelector.selectTaskService(type).printTask(type, name)
        return Result(printing)
    }

    @PostMapping("{type}/{name}/printFromConsole")
    @Authorization(AuthorizationType.MANAGEMENT)
    fun printTaskFromConsole(
        @PathVariable type: TaskType,
        @PathVariable name: String): Result<Printing> {

        val printing = CoroutineUtils.runSuspend {
            taskServiceSelector.selectTaskService(type)
                .printTask(type, name, true)
        }
        return Result(printing)
    }

//    @PostMapping("{name}/delete")
//    fun deleteTask(
//        @PathVariable name: String): Result<Boolean> {
//        return Result(null)
//    }

}