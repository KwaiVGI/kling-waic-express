package com.kling.waic.controller

import com.kling.waic.auth.Authorization
import com.kling.waic.auth.AuthorizationType
import com.kling.waic.entity.Printing
import com.kling.waic.entity.Result
import com.kling.waic.entity.Task
import com.kling.waic.entity.TaskType
import com.kling.waic.helper.TaskServiceSelector
import com.kling.waic.utils.CoroutineUtils
import com.kling.waic.utils.Slf4j.Companion.log
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/tasks")
class TaskController (
    private val taskServiceSelector: TaskServiceSelector
) {

    @PostMapping("{type}/new")
    @Authorization(AuthorizationType.CREATE_TASK)
    fun newTask(@PathVariable type: TaskType,
                @RequestParam("file") file: MultipartFile): Result<Task> {
        log.debug("Creating new task of type: {}, file: {}", type, file.originalFilename)

        val task = CoroutineUtils.runSuspend {
            taskServiceSelector.selectTaskService(type)
                .createTask(type, file)
        }

        log.debug("Created new task of type: {}, file: {}, task: {}",
            type, file.originalFilename, task)
        return Result(task)
    }

    @GetMapping("{type}/{name}/query")
    @Authorization(AuthorizationType.CREATE_TASK)
    fun queryTask(@PathVariable type: TaskType,
                  @PathVariable name: String): Result<Task> {
        log.debug("Query task of type: {}, name: {}", type, name)

        val task = CoroutineUtils.runSuspend {
            taskServiceSelector.selectTaskService(type)
                .queryTask(type, name)
        }

        log.debug("Query task with result, taskId: {}, taskStatus: {}", task.id, task.status)
        return Result(task)
    }

    @PostMapping("{type}/{name}/print")
    @Authorization(AuthorizationType.CREATE_TASK)
    fun printTask(
        @PathVariable type: TaskType,
        @PathVariable name: String): Result<Printing> {

        val printing = CoroutineUtils.runSuspend {
            taskServiceSelector.selectTaskService(type)
                .printTask(type, name)
        }
        return Result(printing)
    }

    @PostMapping("{type}/{name}/printFromConsole")
    @Authorization(AuthorizationType.CREATE_TASK)
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