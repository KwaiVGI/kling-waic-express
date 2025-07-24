#ifndef PRINTER_MANAGER_H
#define PRINTER_MANAGER_H
#include <vector>
#include <string>
#include <mutex>
#include <atomic>
#include <algorithm>
#include "Printer.h"

struct PrinterInfo{
    std::wstring printerName;
    int pageWidth;
    int pageHeight;
    int dpi;
    bool savePhoto;
};

class PrinterManager {
public:
    PrinterManager(std::vector<PrinterInfo>& printerInfos);

    ~PrinterManager();
    
    void addImage(const std::string imgPath);

    bool addPrinter(const std::wstring& name);

    bool removePrinter(const int idx);

private:
    std::vector<Printer*> m_printerList;
    void doPrint(const int idx, const std::string imgPath);
    std::atomic<int> lastUseIdx = -1;
    std::mutex m_mutex;
    // 返回idx
    int getMostIdlePrinter();
};
#endif