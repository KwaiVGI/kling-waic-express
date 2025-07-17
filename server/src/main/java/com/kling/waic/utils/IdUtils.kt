package com.kling.waic.utils

import java.util.*

class IdUtils {

    companion object {
        fun generateId(): Long {
            return UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE
        }
    }
}