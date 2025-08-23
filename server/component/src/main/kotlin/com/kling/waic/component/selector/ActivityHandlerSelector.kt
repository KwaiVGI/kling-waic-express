package com.kling.waic.component.selector

import com.kling.waic.component.handler.ActivityHandler
import com.kling.waic.component.utils.ThreadContextUtils
import org.springframework.stereotype.Component

@Component
class ActivityHandlerSelector(
    private val activityHandlerMap: Map<String, ActivityHandler>,
) {

    fun selectActivityHandler(): ActivityHandler {
        val activity = ThreadContextUtils.getActivity()
        return activityHandlerMap[activity] ?: activityHandlerMap["default"]!!
    }
}