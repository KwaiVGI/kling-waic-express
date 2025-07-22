#include "PrinterManager.h"
#include <glog/logging.h>

PrinterManager::PrinterManager(std::vector<PrinterInfo>& printerInfos){
    for (PrinterInfo info : printerInfos) {
        Printer* printer = new Printer(info.printerName, info.pageWidth, info.pageHeight, info.dpi, info.savePhoto);
        m_printerList.push_back(printer);
    }

}

PrinterManager::~PrinterManager() {
    for (Printer* printer_ptr : m_printerList) {
        delete printer_ptr;
    }
    m_printerList.clear();
}

void PrinterManager::addImage(std::string imgPath) {
    std::lock_guard<std::mutex> guard(m_mutex);
    int idxIdle = getMostIdlePrinter();
    m_printerList[idxIdle]->addPhotoFile(imgPath);
}

int PrinterManager::getMostIdlePrinter() {
    lastUseIdx++;
    if (lastUseIdx >= m_printerList.size()) {
        lastUseIdx = 0;
    } 
    LOG(INFO) << "mostIdleID" << lastUseIdx;
    return lastUseIdx.load(std::memory_order_relaxed);
}
