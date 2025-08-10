package com.kling.waic

import com.kling.waic.printer.adapter.PrintAdapter
import com.kling.waic.printer.PrintingScheduler
import com.kling.waic.printer.client.PrintingDataClient
import com.kling.waic.printer.listener.PrintJobCallback
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    exclude = [
        PrintAdapter::class,
        PrintingScheduler::class,
        PrintingDataClient::class,
        PrintJobCallback::class
    ]
)
open class WAICExpressApplication {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<WAICExpressApplication>(*args)
        }
    }
}