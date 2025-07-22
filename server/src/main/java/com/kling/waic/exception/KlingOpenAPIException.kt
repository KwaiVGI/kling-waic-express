package com.kling.waic.exception

import com.kling.waic.entity.ResultStatus
import com.kling.waic.exception.base.SystemException
import com.kling.waic.exception.base.WAICException
import com.kling.waic.external.model.KlingOpenAPIResult

class KlingOpenAPIException : WAICException, SystemException {
    var result: KlingOpenAPIResult<*>

    constructor(result: KlingOpenAPIResult<*>) {
        this.result = result
    }

    override fun resultStatus(): ResultStatus {
        return ResultStatus.KLING_OPEN_API_EXCEPTION
    }
}