package com.kling.waic.exception

import com.kling.waic.entity.Result
import com.kling.waic.entity.ResultStatus
import com.kling.waic.utils.Slf4j.Companion.log
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.lang.Exception

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(value = [Exception::class])
    fun handleRuntimeException(ex: Exception): Result<Any> {
        if (ex is NoResourceFoundException) {
            log.debug("Static resource not found: ${ex.message}")
            throw ex
        }
        
        log.error("handleRuntimeException", ex)

        if (ex is KlingOpenAPIException) {
            return Result(
                status = ex.resultStatus(),
                message = ex.message ?: "",
                klingOpenAPIResult = ex.result
            )
        }

        if (ex is WAICException) {
            return Result(
                status = ex.resultStatus(),
                message = ex.message ?: ""
            )
        }

        return Result(
            status = ResultStatus.FAILED,
            message = ex.message ?: ""
        )
    }
}