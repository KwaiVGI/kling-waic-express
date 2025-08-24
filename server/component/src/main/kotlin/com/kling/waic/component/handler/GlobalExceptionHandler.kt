package com.kling.waic.component.handler

import com.kling.waic.component.entity.Result
import com.kling.waic.component.entity.ResultStatus
import com.kling.waic.component.exception.KlingOpenAPIException
import com.kling.waic.component.exception.base.WAICException
import com.kling.waic.component.utils.ObjectMapperUtils
import com.kling.waic.component.utils.Slf4j.Companion.log
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(value = [Exception::class])
    fun handleRuntimeException(ex: Exception): Result<Any> {
        if (ex is NoResourceFoundException) {
            log.debug("Static resource not found: ${ex.message}")
            throw ex
        }

        if (ex is WAICException) {
            if (ex is KlingOpenAPIException) {
                log.error("handle KlingOpenAPIException: " +
                        "result: ${ObjectMapperUtils.toJSON(ex.result)}", ex)
                return Result(
                    status = ex.resultStatus(),
                    message = ex.message ?: "",
                    klingOpenAPIResult = ex.result
                )
            }

            log.error("handle WAICException: ${ex::class.java.simpleName}, " +
                    "message: ${ex.message}", ex)
            return Result(
                status = ex.resultStatus(),
                message = ex.message ?: ""
            )
        }

        log.error("handle Exception: ${ex::class.java.simpleName}, " +
                "message: ${ex.message}", ex)
        return Result(
            status = ResultStatus.FAILED,
            message = ex.message ?: ""
        )
    }
}