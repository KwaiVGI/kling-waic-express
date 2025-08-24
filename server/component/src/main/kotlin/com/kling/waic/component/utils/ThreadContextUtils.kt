package com.kling.waic.component.utils

import org.slf4j.MDC

object ThreadContextUtils {
    private const val ACTIVITY = "activity"

    fun putActivity(activity: String) {
        MDC.put(ACTIVITY, activity)
    }

    fun getActivity(): String {
        return MDC.get(ACTIVITY) ?: ""
    }
}

object Constants {
    const val DEFAULT_ACTIVITY = "default"
}