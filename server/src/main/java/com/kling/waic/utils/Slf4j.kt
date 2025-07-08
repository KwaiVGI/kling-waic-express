package com.kuaishou.m2v.kling.component.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author longxinnan <longxinnan@kuaishou.com>
 * Created on 2024-06-19
 */
interface Slf4j {

    companion object {

        val <reified T> T.log: Logger
            inline get() = LoggerFactory.getLogger(T::class.java)
    }
}