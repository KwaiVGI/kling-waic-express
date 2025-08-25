package com.kling.waic.printer.listener

import com.kling.waic.component.entity.PrintingStatus
import com.kling.waic.printer.client.PrintingDataClient
import com.kling.waic.component.utils.Slf4j.Companion.log
import org.springframework.stereotype.Component
import javax.print.attribute.standard.JobName
import javax.print.event.PrintJobAdapter
import javax.print.event.PrintJobEvent

@Component
class PrintJobCallback(
    private val printingDataClient: PrintingDataClient
) : PrintJobAdapter() {
    override fun printJobCompleted(pje: PrintJobEvent) {
        updatePrintingStatus(pje, PrintingStatus.COMPLETED)
    }

    override fun printJobFailed(pje: PrintJobEvent) {
        updatePrintingStatus(pje, PrintingStatus.FAILED)
    }

    override fun printJobCanceled(pje: PrintJobEvent) {
        updatePrintingStatus(pje, PrintingStatus.CANCELLED)
    }

    override fun printDataTransferCompleted(pje: PrintJobEvent) {
        updatePrintingStatus(pje, PrintingStatus.PRINTING)
    }

    override fun printJobNoMoreEvents(pje: PrintJobEvent) {
        val printingName = getPrintingName(pje)
        log.info("✅ No more events for taskName: ${printingName}!")
    }

    override fun printJobRequiresAttention(pje: PrintJobEvent) {
        val printingName = getPrintingName(pje)
        log.info("⚠️ Requires attention for taskName: ${printingName}!")
    }

    private fun updatePrintingStatus(pje: PrintJobEvent, status: PrintingStatus) {
        val printingName = getPrintingName(pje)

        if (printingName.startsWith("Batch:")) {
            val twoPrintingNames: String = printingName.substring("Batch:".length)
            val printingNames = twoPrintingNames.split("-")
            printingNames.forEach {
                updatePrintingStatusForOne(it, status)
            }
        } else {
            updatePrintingStatusForOne(printingName, status)
        }
    }

    private fun updatePrintingStatusForOne(printingName: String, status: PrintingStatus) {
        val printing = printingDataClient.updatePrintingStatus(printingName, status)
        log.info("Receive event from printer, update printingName: ${printingName}, " +
                "status: ${status}, result status: ${printing.status}!")
    }

    private fun getPrintingName(pje: PrintJobEvent): String {
        val job = pje.printJob
        val jobNameAttr = job.attributes.get(JobName::class.java)
        val printingName = (jobNameAttr as? JobName)?.value ?: "Unknown Task"
        return printingName
    }
}