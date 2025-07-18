package com.kling.waic.exception

import com.kling.waic.entity.Result
import com.kling.waic.entity.ResultStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.lang.Exception

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(value = [Exception::class])
    fun handleRuntimeException(ex: Exception): Result<Any> {

        if (ex is WAICException) {
            return Result(
                status = ex.resultStatus()
            )
        }

        return Result(
            status = ResultStatus.FAILED
        )
    }
}