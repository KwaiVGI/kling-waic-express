#ifdef _MSC_VER
#ifndef _PRINTER_H_
#define _PRINTER_H_

#include <windows.h>
#include <gdiplus.h>
#include <mutex>
#include <queue>
#include <thread>
#include <atomic>
class Printer
{
public:
    Printer(const std::wstring& printerName, int printerPageWidth = 0, int printerPageHeight = 0, int dpi = 0, bool savePhotos = false);
    ~Printer();
    void addPhotoFile(const std::string& filename);
    DWORD getJobsCount();

private:
    void run();
    bool preparePrinterSetting();
    bool updatePrinterConfig(double imageWidth, double imageHeight);
    std::string getImageFile();
    void monitorLoop();

private:
    std::thread m_printThread;
    std::thread m_monitorThread;
    int m_docSize = { 0 };
    std::queue<std::string> m_filenameQueue;
    std::mutex m_mutex;
    bool m_isRunning = true;
    bool m_isListening{true};
    int m_loopCount{0};

    HANDLE m_hPrinter = nullptr;
    std::wstring m_printerName;
    HGLOBAL m_hDevMode;
    PRINTDLG m_printInfo = { 0 };
    double m_lastPrintImageWidth = 0;
    double m_lastPrintImageHeight = 0;
    int m_pageWidthInMM = 0;
    int m_pageHeightInMM = 0;
    int m_dpi = 0;
    bool m_savePhotos = false;
};

#endif // _PRINTER_H_

#endif