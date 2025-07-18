package com.kling.waic.exception

import com.kling.waic.entity.ResultStatus
import java.lang.RuntimeException

class NoHumanFaceDetectException : WAICException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
    constructor(
        message: String?,
        cause: Throwable?,
        enableSuppression: Boolean,
        writableStackTrace: Boolean
    ) : super(message, cause, enableSuppression, writableStackTrace)

    override fun resultStatus(): ResultStatus {
        return ResultStatus.NO_HUMAN_DETECTED
    }
}