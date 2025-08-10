package com.kling.waic.printer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = [
        "com.kling.waic.printer",
        "com.kling.waic.component.config"
    ]
)
open class WAICPrinterApplication {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<WAICPrinterApplication>(*args)
        }
    }
}