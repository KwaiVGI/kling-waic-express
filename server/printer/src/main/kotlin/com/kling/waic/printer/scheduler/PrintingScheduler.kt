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
        val cron = "*/1 * * * * *"
        val timezone = TimeZone.getTimeZone("Asia/Shanghai")
        log.info("Cron task registered, cron: {}, timezone: {}", cron, timezone)

        taskRegistrar.addTriggerTask(
            { process() },
            { triggerContext ->
                CronTrigger(cron, timezone).nextExecution(triggerContext)
            }
        )
    }

    private fun process() {
        printAdapter.tryFetchAndPrint()
    }
}