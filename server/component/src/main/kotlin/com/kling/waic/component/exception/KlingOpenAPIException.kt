package com.kling.waic.component.exception

import com.kling.waic.component.entity.ResultStatus
import com.kling.waic.component.exception.base.SystemException
import com.kling.waic.component.exception.base.WAICException
import com.kling.waic.component.external.model.KlingOpenAPIResult

class KlingOpenAPIException : WAICException, SystemException {
    var result: KlingOpenAPIResult<*>

    constructor(result: KlingOpenAPIResult<*>) {
        this.result = result
    }

    override fun resultStatus(): ResultStatus {
        return ResultStatus.KLING_OPEN_API_EXCEPTION
    }
}