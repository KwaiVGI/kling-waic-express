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
import java.util.concurrent.Executors
import javax.print.*
import javax.print.attribute.HashDocAttributeSet
import javax.print.attribute.HashPrintRequestAttributeSet
import javax.print.attribute.standard.JobName
import javax.print.attribute.standard.MediaPrintableArea
import javax.print.attribute.standard.OrientationRequested
import javax.print.attribute.standard.PrinterInfo
import javax.print.attribute.standard.PrinterIsAcceptingJobs
import javax.print.attribute.standard.PrinterName

@Component
class PrintAdapter(
    @param:Value("\${PRINTER_PRINTING_MODE:PDF_BATCH}")
    private val printingMode: PrintingMode,
    @param:Value("\${PRINTER_PRINTING_BATCH_SIZE:2}")
    private val printingBatchSize: Int,
    @param:Value("\${PRINTER_EXECUTOR_SIZE:10}")
    private val printerExecutorSize: Int,
    @param:Value("\${PRINTER_EXTRA_SCALE_FACTOR:1.00}")
    private val extraScaleFactor: Float,
    @param:Value("\${DRAW_IMAGE_X:0.0}")
    private val drawImageX: Float,
    @param:Value("\${DRAW_IMAGE_Y:0.0}")
    private val drawImageY: Float,
    private val printingDataClient: PrintingDataClient,
    private val printJobCallback: PrintJobCallback
) {
    private val executor = Executors.newFixedThreadPool(printerExecutorSize)

    private lateinit var printers: List<PrintService>

    @PostConstruct
    fun init() {
        printers = findPrinters()
    }

    private fun findPrinters(): List<PrintService> {
        return PrintServiceLookup.lookupPrintServices(null, null).toList()
    }

    private fun fetchQueuedJobCount(): Int {
        return printers.sumOf { printer ->
            val printerSystemName = printer.getAttribute(PrinterName::class.java)
            val command = arrayOf("sh", "-c", "lpstat -o \"$printerSystemName\" | wc -l")
            val output = ProcessBuilder(*command)
                .redirectErrorStream(true)
                .start()
                .inputStream.bufferedReader().readText().trim()
            output.toIntOrNull() ?: 0
        }
    }

    fun tryFetchAndPrint() {
        printers.shuffled().forEach { printer ->
            doFetchAndPrintForOnePrinter(printer)
        }
    }

    fun doFetchAndPrintForOnePrinter(printer: PrintService) {
        val printerName = printer.getAttribute(PrinterInfo::class.java).value
        val printerIsAcceptingJobs = printer.getAttribute(PrinterIsAcceptingJobs::class.java)
        if (printerIsAcceptingJobs.value < 1) {
            log.warn("Printer: ${printerName} is not accepting jobs, skip printing job.")
            return
        }

        val printings = printingDataClient.fetchPrinting(printingBatchSize)
        log.info("Printer: ${printerName} fetch job, batchSize: $printingBatchSize, printings.size: ${printings.size}")
        if (printings.isEmpty()) {
            return
        }

        when (printingMode) {
            PrintingMode.EACH_ONE -> printByEachOne(printer, printings)
            PrintingMode.PDF_BATCH -> printBatchAsPDF(printer, printings)
        }
    }

    private fun printByEachOne(printer: PrintService, printings: List<Printing>) {
        for (printing in printings) {
            executor.submit {
                printOne(printer, printing)
            }
        }
    }

    private fun printOne(printer: PrintService, printing: Printing) {
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
        printer: PrintService,
        printings: List<Printing>
    ) {
        log.debug("Starting PDF generation for ${printings.size} photos")

        val tempPdfPath = PhotoUtils.generateTempPdfPath(printings)
        log.debug("Temporary PDF path: $tempPdfPath")

        val pdfPath = PhotoUtils.generateBatchAsOnePdf(
            printings = printings,
            outputPath = tempPdfPath,
            extraScaleFactor = extraScaleFactor,
            drawImageX = drawImageX,
            drawImageY = drawImageY
        )

        printPdfFile(printer, pdfPath, printings)
        cleanupTempFile(pdfPath)
    }

    private fun printPdfFile(
        printer: PrintService,
        pdfPath: String,
        printings: List<Printing>) {
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
