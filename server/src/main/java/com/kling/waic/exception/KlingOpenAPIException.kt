package com.kling.waic.exception

import com.kling.waic.entity.ResultStatus
import com.kling.waic.external.model.KlingOpenAPIResult

class KlingOpenAPIException : WAICException {
    var result: KlingOpenAPIResult<*>

    constructor(result: KlingOpenAPIResult<*>) {
        this.result = result
    }

    override fun resultStatus(): ResultStatus {
        return ResultStatus.KLING_OPEN_API_EXCEPTION
    }
}