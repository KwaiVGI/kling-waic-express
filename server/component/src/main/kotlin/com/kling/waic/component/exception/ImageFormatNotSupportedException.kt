package com.kling.waic.component.exception

import com.kling.waic.component.entity.ResultStatus
import com.kling.waic.component.exception.base.HumanOperationException
import com.kling.waic.component.exception.base.WAICException

class ImageFormatNotSupportedException : WAICException, HumanOperationException {
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
        return ResultStatus.IMAGE_FORMAT_NOT_SUPPORTED
    }
}