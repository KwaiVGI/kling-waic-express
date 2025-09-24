package com.kling.waic.printer.scheduler

import com.kling.waic.component.utils.Slf4j.Companion.log
import com.kling.waic.printer.adapter.PrintAdapter
import org.springframework.core.env.AbstractEnvironment
import org.springframework.core.env.Environment
import org.springframework.core.env.MapPropertySource
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import org.springframework.scheduling.support.CronTrigger
import org.springframework.stereotype.Component
import java.util.TimeZone

@Component
@EnableScheduling
class PrintingScheduler(
    private val printAdapter: PrintAdapter,
    private val env: Environment
) : SchedulingConfigurer {

    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        log.info("Print All Configs:")
        printAllConfigs()

        val cron = "*/1 * * * * *"
        val timezone = TimeZone.getTimeZone("Asia/Shanghai")
        log.info("Cron task registered, cron: {}, timezone: {}", cron, timezone)

        taskRegistrar.addTriggerTask(
            { printAdapter.tryFetchAndPrint() },
            { triggerContext ->
                CronTrigger(cron, timezone).nextExecution(triggerContext)
            }
        )
        taskRegistrar.addTriggerTask(
            { printAdapter.setQueuedJobCount() },
            { triggerContext ->
                CronTrigger(cron, timezone).nextExecution(triggerContext)
            }
        )
    }

    fun printAllConfigs() {
        val propertySources = (env as AbstractEnvironment).propertySources
            .filterIsInstance<MapPropertySource>()

        val allProperties = propertySources
            .flatMap { it.source.entries }

        allProperties.forEach { (key, value) ->
            log.info("$key = $value")
        }
    }
}