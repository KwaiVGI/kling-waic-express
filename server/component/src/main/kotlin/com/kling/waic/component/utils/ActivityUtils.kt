package com.kling.waic.component.utils

object ActivityUtils {
    const val SLASH = "/"
    const val COLON = ":"

    fun generateNewKey(rawKey: String, separator: String): String {
        val activity = ThreadContextUtils.getActivity()
        val newKey = if (activity.isEmpty()) {
            rawKey
        } else {
            "$activity$separator$rawKey"
        }
        return newKey
    }
}