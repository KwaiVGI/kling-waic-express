package com.kling.waic.handler

import com.kling.waic.entity.Result
import com.kling.waic.entity.ResultStatus
import com.kling.waic.exception.KlingOpenAPIException
import com.kling.waic.exception.base.HumanOperationException
import com.kling.waic.exception.base.SystemException
import com.kling.waic.exception.base.WAICException
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

        if (ex is WAICException) {
            if (ex is SystemException) {
                log.error("handle SystemException: ${ex.message}", ex)
            } else if (ex is HumanOperationException) {
                log.info("handle HumanOperationException: ${ex.message}")
            }

            if (ex is KlingOpenAPIException) {
                return Result(
                    status = ex.resultStatus(),
                    message = ex.message ?: "",
                    klingOpenAPIResult = ex.result
                )
            }
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