package com.kling.waic.test.printer.adapter

import com.kling.waic.printer.adapter.PrintAdapter
import com.kling.waic.test.SpringBaseTestForPrinter
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PrinterAdapterTest : SpringBaseTestForPrinter() {

    @Autowired
    private lateinit var printerAdapter: PrintAdapter

    @Test
    fun testTryFetchAndPrint() {
        printerAdapter.tryFetchAndPrint()
    }
}