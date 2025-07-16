package com.kling.waic

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class WAICExpressApplication {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<WAICExpressApplication>(*args)
        }
    }
}