package com.kling.waic.printer.adapter

import com.kling.waic.component.entity.Printing
import com.kling.waic.component.utils.Slf4j.Companion.log
import com.kling.waic.printer.client.PrintingDataClient
import com.kling.waic.printer.listener.PrintJobCallback
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.*
import javax.imageio.ImageIO
import javax.print.*
import javax.print.attribute.HashDocAttributeSet
import javax.print.attribute.HashPrintRequestAttributeSet
import javax.print.attribute.standard.*

@Component
class PrintAdapter(
    @param:Value("\${PRINTER_NAME:DNP DP-DS620}") private val printerName: String,
    @param:Value("\${PRINTER_SYSTEM_NAME:Dai_Nippon_Printing_DP_DS620}")
    private val printerSystemName: String,
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

        val printings = printingDataClient.fetchPrinting(2)
        if (printings.isEmpty()) {
            log.debug("Printing queue is empty, or queuedJobCount is too large" +
                    ", skip printing job.")
            return
        }

//        printings.forEach { printOne(it) }
//        val printings = printingDataClient.fetchPrinting(2)
        if (printings.size == 2) {
            printTwoAsOne(printings)
        } else if (printings.size == 1) {
            printOne(printings[0])
        }
    }

    /**
     * 打印两张照片（6x4） -> 合成一张 6x8
     */
    fun printTwoAsOne(printings: List<Printing>) {
        if (printings.size < 2) {
            log.warn("Not enough printings to merge 2 into 1, skipping...")
            return
        }

        // 1. 下载两张图片
        val img1: BufferedImage = ImageIO.read(URL(printings[0].task.outputs!!.url))
        val img2: BufferedImage = ImageIO.read(URL(printings[1].task.outputs!!.url))

        // 2. 合成一张大图（横向拼接）
        val combined = BufferedImage(
            img1.width + img2.width,
            img1.height,
            BufferedImage.TYPE_INT_RGB
        )
        val g = combined.createGraphics()
        g.drawImage(img1, 0, 0, null)
        g.drawImage(img2, img1.width, 0, null)
        g.dispose()

        // 3. 转换成 InputStream
        val baos = ByteArrayOutputStream()
        ImageIO.write(combined, "jpg", baos)
        val inputStream = ByteArrayInputStream(baos.toByteArray())

        // 4. 打印
        val flavor = DocFlavor.INPUT_STREAM.JPEG
        val docAttrs = HashDocAttributeSet()
        val doc: Doc = SimpleDoc(inputStream, flavor, docAttrs)

        val attrs = HashPrintRequestAttributeSet()
        attrs.add(OrientationRequested.LANDSCAPE)
        attrs.add(MediaPrintableArea(0f, 0f, 8f, 6f, MediaPrintableArea.INCH))
        attrs.add(JobName("Two:${printings[0].name}-${printings[1].name}", Locale.getDefault()))

        val job = printer.createPrintJob()
        job.addPrintJobListener(printJobCallback)
        job.print(doc, attrs)

        log.info("Submitted 2 photos as one 6x8 print job.")
    }

    private fun printOne(printing: Printing) {
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
}
