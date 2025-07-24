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

bool PrinterManager::addPrinter(const std::wstring& printerName) {
    
    Printer* printer = new Printer(printerName);
    if (!printer) {
        delete printer;
        return false;
    }
    {
        std::lock_guard<std::mutex> guard(m_mutex);
        m_printerList.push_back(printer);
    }
    return true;
}

bool PrinterManager::removePrinter(const int idx) {
    std::lock_guard<std::mutex> guard(m_mutex);
    if (idx < 0 || idx >= m_printerList.size()) {
        LOG(INFO) << "remove printer out of size";
        return false;
    }
    Printer* printer = m_printerList[idx];
    if (printer) delete printer;
    m_printerList.erase(m_printerList.begin() + idx);
    return true;
}
