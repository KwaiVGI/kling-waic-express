package com.kling.waic.printer.listener

import com.kling.waic.entity.PrintingStatus
import com.kling.waic.printer.client.PrintingDataClient
import com.kling.waic.utils.Slf4j.Companion.log
import org.springframework.stereotype.Component
import javax.print.attribute.standard.JobName
import javax.print.event.PrintJobAdapter
import javax.print.event.PrintJobEvent

@Component
class PrintJobCallback(
    private val printingDataClient: PrintingDataClient
) : PrintJobAdapter() {
    override fun printJobCompleted(pje: PrintJobEvent) {
        val taskName = getTaskName(pje)
        val printing = printingDataClient.updatePrintingStatus(taskName, PrintingStatus.COMPLETED)
        log.info("✅ Print job completed for taskName: ${taskName}, printing status: ${printing.status}!")
    }

    override fun printJobFailed(pje: PrintJobEvent) {
        val taskName = getTaskName(pje)
        val printing = printingDataClient.updatePrintingStatus(taskName, PrintingStatus.FAILED)
        log.info("❌ Print job failed for taskName: ${taskName}, printing status: ${printing.status}!")
    }

    override fun printJobCanceled(pje: PrintJobEvent) {
        val taskName = getTaskName(pje)
        val printing = printingDataClient.updatePrintingStatus(taskName, PrintingStatus.CANCELLED)
        log.info("⚠️ Print job cancelled for taskName: ${taskName}, printing status: ${printing.status}!")
    }

    override fun printDataTransferCompleted(pje: PrintJobEvent) {
        val taskName = getTaskName(pje)
        val printing = printingDataClient.updatePrintingStatus(taskName, PrintingStatus.PRINTING)
        log.info("📤 Data transfer completed for taskName: ${taskName}, printing status: ${printing.status}!")
    }

    override fun printJobNoMoreEvents(pje: PrintJobEvent) {
        val taskName = getTaskName(pje)
        log.info("✅ No more events for taskName: ${taskName}!")
    }

    override fun printJobRequiresAttention(pje: PrintJobEvent) {
        val taskName = getTaskName(pje)
        log.info("⚠️ Requires attention for taskName: ${taskName}!")
    }

    private fun getTaskName(pje: PrintJobEvent): String {
        val job = pje.printJob
        val jobNameAttr = job.attributes.get(JobName::class.java)
        val taskName = (jobNameAttr as? JobName)?.value ?: "Unknown Task"
        return taskName
    }
}