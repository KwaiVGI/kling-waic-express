package com.kling.waic.printer.adapter

import com.kling.waic.component.utils.Slf4j.Companion.log
import com.kling.waic.printer.client.PrintingDataClient
import com.kling.waic.printer.listener.PrintJobCallback
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URL
import java.util.*
import javax.print.*
import javax.print.attribute.HashDocAttributeSet
import javax.print.attribute.HashPrintRequestAttributeSet
import javax.print.attribute.standard.*

@Component
class PrintAdapter(
    @param:Value("\${PRINTER_NAME:DNP DP-DS620}") private val printerName: String,
    private val printingDataClient: PrintingDataClient,
    private val printJobCallback: PrintJobCallback
) {
    private lateinit var printer: PrintService

    @PostConstruct
    fun init() {
        this.printer = findPrinter()
    }

    private fun findPrinter(): PrintService {
        val services = PrintServiceLookup.lookupPrintServices(null, null)
        for (service in services) {
            if (printerName == service.name) {
                println("Printer found: ${service.name}")
                return service
            }
        }
        throw IllegalArgumentException("No printer found for $printerName")
    }

    private fun fetchQueuedJobCount(): Int {
        val queuedJobCount = printer.getAttribute(QueuedJobCount::class.java)
        return queuedJobCount?.value ?: 0
    }

    fun tryFetchAndPrint() {
        val queuedJobCount = fetchQueuedJobCount()
        val result = printingDataClient.setPrinterQueuedJobCount(queuedJobCount)
        log.info("Set Printer queuedJobCount: $result")

        val printerIsAcceptingJobs = printer.getAttribute(PrinterIsAcceptingJobs::class.java)
        if (printerIsAcceptingJobs.value < 1) {
            println("Printer is not accepting jobs, skip printing job.")
            return
        }

        val printing = printingDataClient.fetchPrinting()
        if (printing == null) {
            log.info("Printing queue is empty, skip printing job.")
            return
        }
        val taskName = printing.task.name
        val imageUrl = printing.task.outputs!!.url

        URL(imageUrl).openStream()
            .use { imageStream ->
                val flavor = DocFlavor.INPUT_STREAM.JPEG

                // Create document attributes - keep it simple to avoid ClassCastException
                val docAttrs = HashDocAttributeSet()

                val doc: Doc = SimpleDoc(imageStream, flavor, docAttrs)

                val attrs = HashPrintRequestAttributeSet()
                attrs.add(OrientationRequested.PORTRAIT)
                attrs.add(MediaPrintableArea(0f, 0f, 4f, 6f, MediaPrintableArea.INCH))
                attrs.add(JobName(taskName, Locale.getDefault()))

                val job: DocPrintJob = printer.createPrintJob()

                // Add job listener
                job.addPrintJobListener(printJobCallback)

                job.print(doc, attrs)
            }
    }
}
