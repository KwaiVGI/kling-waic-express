package com.kling.waic.entity

enum class ResultStatus {
    SUCCEED,
    FAILED,
    NO_HUMAN_DETECTED,
    JSON_EXCEPTION,
    DUPLICATE_PRINT,
    IMAGE_FORMAT_NOT_SUPPORTED,
    KLING_OPEN_API_EXCEPTION,
}