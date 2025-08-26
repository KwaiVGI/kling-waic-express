package com.kling.waic.printer.adapter

import com.kling.waic.component.entity.Printing
import com.kling.waic.component.utils.PhotoUtils
import com.kling.waic.component.utils.Slf4j.Companion.log
import com.kling.waic.printer.client.PrintingDataClient
import com.kling.waic.printer.listener.PrintJobCallback
import com.kling.waic.printer.model.PrintingMode
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.net.URL
import java.util.*
import javax.print.*
import javax.print.attribute.HashDocAttributeSet
import javax.print.attribute.HashPrintRequestAttributeSet
import javax.print.attribute.standard.JobName
import javax.print.attribute.standard.MediaPrintableArea
import javax.print.attribute.standard.OrientationRequested
import javax.print.attribute.standard.PrinterIsAcceptingJobs

@Component
class PrintAdapter(
    @param:Value("\${PRINTER_NAME:DNP DP-DS620}")
    private val printerName: String,
    @param:Value("\${PRINTER_SYSTEM_NAME:Dai_Nippon_Printing_DP_DS620}")
    private val printerSystemName: String,
    @param:Value("\${PRINTER_PRINTING_MODE:PDF_BATCH}")
    private val printingMode: PrintingMode,
    @param:Value("\${PRINTER_PRINTING_BATCH_SIZE:2}")
    private val printingBatchSize: Int,
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
                log.info("Printer found: ${service.name}")
                return service
            }
        }
        throw IllegalArgumentException("No printer found for $printerName")
    }

    private fun fetchQueuedJobCount(): Int {
        return try {
            val os = System.getProperty("os.name").lowercase()
            if (os.contains("win")) {
                // Windows 使用 PowerShell 查询
                val command = arrayOf(
                    "powershell", "-Command",
                    "Get-PrintJob -PrinterName \"${printerName}\" | Measure-Object | Select -ExpandProperty Count"
                )
                val output = ProcessBuilder(*command)
                    .redirectErrorStream(true)
                    .start()
                    .inputStream.bufferedReader().readText().trim()
                output.toIntOrNull() ?: 0
            } else {
                // 再用真实名字查队列
                val command = arrayOf("sh", "-c", "lpstat -o \"$printerSystemName\" | wc -l")
                val output = ProcessBuilder(*command)
                    .redirectErrorStream(true)
                    .start()
                    .inputStream.bufferedReader().readText().trim()
                output.toIntOrNull() ?: 0
            }
        } catch (e: Exception) {
            log.error("Failed to get queued job count", e)
            0
        }
    }

    fun tryFetchAndPrint() {
        val printerIsAcceptingJobs = printer.getAttribute(PrinterIsAcceptingJobs::class.java)
        if (printerIsAcceptingJobs.value < 1) {
            log.warn("Printer is not accepting jobs, skip printing job.")
            return
        }

        val printings = printingDataClient.fetchPrinting(printingBatchSize)
        log.info("FetchPrinting from queue, batchSize: $printingBatchSize, " +
                "printings.size: ${printings.size}")
        if (printings.isEmpty()) {
            return
        }

        when (printingMode) {
            PrintingMode.EACH_ONE -> printings.forEach { printOne(it) }
            PrintingMode.PDF_BATCH -> printBatchAsPDF(printings)
        }
    }

    private fun printOne(printing: Printing) {
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
                attrs.add(JobName(printing.name, Locale.getDefault()))

                val job: DocPrintJob = printer.createPrintJob()

                // Add job listener
                job.addPrintJobListener(printJobCallback)

                job.print(doc, attrs)
            }
    }

    fun setQueuedJobCount() {
        val queuedJobCount = fetchQueuedJobCount()
        val result = printingDataClient.setPrinterQueuedJobCount(queuedJobCount)
        log.info("Set Printer queuedJobCount: $queuedJobCount, result: $result")
    }

    fun printBatchAsPDF(
        printings: List<Printing>
    ) {
        log.debug("Starting PDF generation for ${printings.size} photos")

        val tempPdfPath = PhotoUtils.generateTempPdfPath(printings)
        log.debug("Temporary PDF path: $tempPdfPath")

        val pdfPath = PhotoUtils.generateBatchAsOnePdf(
            printings = printings,
            outputPath = tempPdfPath
        )

        printPdfFile(pdfPath, printings)
        cleanupTempFile(pdfPath)
    }

    private fun printPdfFile(pdfPath: String, printings: List<Printing>) {
        val file = File(pdfPath)
        if (!file.exists()) {
            throw IllegalArgumentException("PDF file does not exist: $pdfPath")
        }

        val flavor = DocFlavor.INPUT_STREAM.PDF
        val docAttrs = HashDocAttributeSet()

        file.inputStream().use { pdfStream ->
            val doc: Doc = SimpleDoc(pdfStream, flavor, docAttrs)

            val attrs = HashPrintRequestAttributeSet()
            attrs.add(OrientationRequested.PORTRAIT)
            attrs.add(MediaPrintableArea(0f, 0f, 6f, 4f, MediaPrintableArea.INCH))

            val jointName = printings.joinToString("-") { it.name }
            val jobName = "Batch:$jointName"
            attrs.add(JobName(jobName, Locale.getDefault()))

            val job = printer.createPrintJob()
            job.addPrintJobListener(printJobCallback)
            job.print(doc, attrs)
        }
    }

    private fun cleanupTempFile(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            val deleted = file.delete()
            if (deleted) {
                log.debug("Temporary file deleted: $filePath")
            } else {
                log.warn("Failed to delete temporary file: $filePath")
            }
        }
    }
}
