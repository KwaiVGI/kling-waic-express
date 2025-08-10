package com.kling.waic.printer.scheduler

import com.kling.waic.component.utils.Slf4j.Companion.log
import com.kling.waic.printer.adapter.PrintAdapter
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import org.springframework.scheduling.support.CronTrigger
import org.springframework.stereotype.Component
import java.util.TimeZone

@Component
@EnableScheduling
class PrintingScheduler(
    private val printAdapter: PrintAdapter
) : SchedulingConfigurer {

    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        taskRegistrar.addTriggerTask(
            { process() },
            { triggerContext ->
                val cron = "*/1 * * * * *" // execute each second
                val timezone = TimeZone.getTimeZone("Asia/Shanghai")

                val cronTrigger = CronTrigger(cron, timezone).nextExecution(triggerContext)
                log.info("Cron task registered, cron: {}, timezone: {}", cron, timezone)

                cronTrigger
            }
        )
    }

    private fun process() {
        log.debug("Printing image...")
        printAdapter.tryFetchAndPrint()
    }
}