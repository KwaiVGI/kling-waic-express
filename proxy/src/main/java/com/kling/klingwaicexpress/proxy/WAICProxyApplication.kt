package com.kling.klingwaicexpress.proxy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class WAICProxyApplication {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<WAICProxyApplication>(*args)
        }
    }
}