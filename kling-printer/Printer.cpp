#ifdef _MSC_VER

#include "Printer.h"
#include <glog/logging.h>
#include <chrono>
#include <cmath>
#include <iostream>
#include <locale>
#include <vector>
#include <map>
#include <QFile>
#include <QString>
#include <codecvt>
#pragma comment(lib, "Gdiplus.lib")

static const int kThreadSleepTime = 100; // in ms

// char* 转 Unicode char*
const wchar_t *GetWC(const char *c)
{
    const size_t cSize = strlen(c)+1;
    wchar_t* wc = new wchar_t[cSize];
    mbstowcs (wc, c, cSize);

    return wc;
}

std::string wstring2string(const std::wstring& wstr) {
    std::wstring_convert<std::codecvt_utf8<wchar_t>, wchar_t> converter;
    return converter.to_bytes(wstr);
} 

Printer::Printer(const std::wstring& printerName, int printerPageWidth /* = 0*/, int printerPageHeight /* = 0*/, int dpi /* = 600*/, bool savePhotos /* = false */)
{
    LOG(INFO) << "constructor function" ;
    PRINTER_DEFAULTSW defaults = { nullptr, nullptr, GENERIC_READ };

    if (!OpenPrinter(const_cast<LPWSTR>(printerName.c_str()), &m_hPrinter, &defaults)) {
        DWORD error = GetLastError();
        LPWSTR errorMsg = NULL;
    
        FormatMessageW(
        FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM,
        NULL, error, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
        (LPWSTR)&errorMsg, 0, NULL
        );
        if (errorMsg) LocalFree(errorMsg);
        throw std::runtime_error("OpenPrinter failed. errorCode:" + std::to_string(error));
    }
    printf("OpenPrinter success!\n");
    if (printerPageWidth > 0 && printerPageHeight > 0)
    {
        m_pageWidthInMM = printerPageWidth;
        m_pageHeightInMM = printerPageHeight;
    }
    if (dpi > 0)
    {
        m_dpi = dpi;
    }
    m_savePhotos = savePhotos;
    m_printerName = printerName;
    // 获取 DEVMODE 所需大小
    LONG size = DocumentProperties(NULL, m_hPrinter, (LPWSTR)printerName.c_str(), NULL, NULL, 0);
    if (size <= 0) {
        throw std::runtime_error("DocumentProperties 获取大小失败，错误码: " + std::to_string(GetLastError()));
    }

    // 分配内存
    m_hDevMode = GlobalAlloc(GHND, size);
    if (!m_hDevMode) {
        throw std::runtime_error("GlobalAlloc hDevMode失败");
    }

    DEVMODE* pDevMode = (DEVMODE*)GlobalLock(m_hDevMode);
    if (!pDevMode) {
        GlobalFree(m_hDevMode);
        m_hDevMode = NULL;
        throw std::runtime_error("GlobalLock 失败");
    }

    // 获取并填充默认 DevMode
    LONG ret = DocumentProperties(NULL, m_hPrinter, (LPWSTR)m_printerName.c_str(), pDevMode, NULL, DM_OUT_BUFFER);
    GlobalUnlock(m_hDevMode);
    if (ret != IDOK) {
        GlobalFree(m_hDevMode);
        m_hDevMode = NULL;
        throw std::runtime_error("DocumentProperties 获取 DevMode 失败，错误码: " + std::to_string(GetLastError()));
    }
    m_printThread = std::thread(&Printer::run, this);
    m_monitorThread = std::thread(&Printer::monitorLoop, this);
}

Printer::~Printer()
{
    if (m_hPrinter) {
        ClosePrinter(m_hPrinter);
    }
    if (m_hDevMode) {
        GlobalFree(m_hDevMode);
    }
    m_isRunning = false;
    m_printThread.join();
    m_isListening = false;
    m_monitorThread.join();
}

void Printer::monitorLoop() {
    while (m_isListening) {
        // 轮询间隔（毫秒）
        // std::this_thread::sleep_for(std::chrono::milliseconds(1000));

        // 枚举所有打印任务
        DWORD needed = 0, numJobs = 0;
        EnumJobs(m_hPrinter, 0, ULONG_MAX, 2, NULL, 0, &needed, &numJobs);
        DWORD err = GetLastError();
        m_loopCount++;
        if (needed == 0) {
            continue; // 无任务
        }
        std::vector<BYTE> buffer(needed);
        if (!EnumJobs(m_hPrinter, 0, ULONG_MAX, 2, buffer.data(), needed, &needed, &numJobs)) {
            std::cerr << "EnumJobs failed: " << GetLastError();
            continue;
        }

        JOB_INFO_2* jobs = reinterpret_cast<JOB_INFO_2*>(buffer.data());
        for (DWORD i = 0; i < numJobs; i++) {
            DWORD jobId = jobs[i].JobId;
            DWORD status = jobs[i].Status;
            LPWSTR documentName = jobs[i].pDocument;
            // 检查状态并触发回调
            if (status & JOB_STATUS_PRINTING) {
                // 避免状态重复提交
                if (printMap.find(jobId) != printMap.end()) {
                    continue;
                }
                // 已经更新过该状态
                printMap[jobId] = true;
                LOG(INFO) << "[job] ID:" << jobId << "printing";
                // if (onPrinting_) {
                //     onPrinting_(jobId, documentName);
                // }
            } else if (status & JOB_STATUS_COMPLETE) {
                if (completeMap.find(jobId) != completeMap.end()) {
                    continue;
                }
                // 已经更新过该状态
                completeMap[jobId] = true;
                LOG(INFO) << "[job] ID:" << jobId << "complete";
                
            //     if (onCompleted_) {
            //         onCompleted_(jobId, documentName);
            //     }
            }
            Sleep(1000);
        }
    }
}
void Printer::PrintImage(HDC hdcPrint, const std::shared_ptr<Gdiplus::Image>& img)
{
    int printW = GetDeviceCaps(hdcPrint, HORZRES);
    int printH = GetDeviceCaps(hdcPrint, VERTRES);
    double printWidthMm = GetDeviceCaps(hdcPrint, HORZSIZE);
    double printHeightMm = GetDeviceCaps(hdcPrint, VERTSIZE);
    double printWidthPixel = printWidthMm * m_dpi / 25.4;
    double printHeightPixel = printHeightMm * m_dpi / 25.4;
    int imgW = img->GetWidth();
    int imgH = img->GetHeight();
    double scale = std::min((double)printWidthPixel / imgW, (double)printHeightPixel / imgH);
    int dstW = static_cast<int>(imgW * scale);
    int dstH = static_cast<int>(imgH * scale);

    Gdiplus::Graphics g(hdcPrint);
    g.SetPageUnit(Gdiplus::UnitPixel);
    g.SetInterpolationMode(Gdiplus::InterpolationModeHighQualityBicubic);

    int x = (printW - dstW) / 2;
    int y = (printH - dstH) / 2;

    g.DrawImage(img.get(), x, y, dstW, dstH);
}

void Printer::run()
{
    Gdiplus::GdiplusStartupInput gdiplusStartupInput;
    ULONG_PTR gdiplusToken;
    Gdiplus::GdiplusStartup(&gdiplusToken, &gdiplusStartupInput, NULL);
    if (!preparePrinterSetting())
    {
        LOG(INFO) << "failed Prepare\n";
        return;
    }
    LOG(INFO) << "thread running while m_isRunning" ;
    while (m_isRunning)
    {
        std::string filename = getImageFile();
        if (!filename.empty())
        {
            auto image = std::shared_ptr<Gdiplus::Image>(Gdiplus::Image::FromFile(GetWC(filename.c_str())));
            unsigned int imageWidth = image->GetWidth();
            unsigned int imageHeight = image->GetHeight();
            if (imageWidth <= 0 ||
                imageHeight <= 0)
            {
                printf("printer: invalid image file: %s\n", filename.c_str());
                continue;
            }
            //开始打印
            // doc name从这修改
            DOCINFO doc_info = { sizeof(DOCINFO), L"Interaction-angle photo printer" };
            HDC hPrintDC = static_cast<HDC>(m_printInfo.hDC);
            int doc_idd = StartDoc(hPrintDC, &doc_info);
            if (doc_idd <= 0)
            {
                printf("printer: StartDoc error! code: %d\n", GetLastError());
                continue;
            }
            printf("printer name: %s\n", wstring2string(m_printerName).c_str());
            printf("printer doc id: %d\n", doc_idd);

            //开始一个打印页面
            if (StartPage(hPrintDC) < 0)
            {
                printf("printer: StartPage error\n");
                AbortDoc(hPrintDC);
                continue;
            }
            PrintImage(hPrintDC, image);
            // double calcWidth = imageWidth, calcHeight = imageHeight;
            // LOG(INFO) << "image size(width x height):" << imageWidth << ", " << imageHeight ;
            // double scaling = std::min((m_pageWidthInMM / calcWidth), (m_pageHeightInMM / calcHeight));
            // calcWidth *= scaling;
            // calcHeight *= scaling;
                
            // if (!updatePrinterConfig(calcWidth, calcHeight))
            // {
            //     printf("printer: update config fail\n");
            //     continue;
            // }

            // auto graphics = std::make_shared<Gdiplus::Graphics>(hPrintDC);
            // graphics->SetPageUnit(Gdiplus::Unit::UnitMillimeter);
            // graphics->DrawImage(image.get(), (m_pageWidthInMM - calcWidth) / 2, 0, calcWidth, calcHeight);

            EndPage(hPrintDC);
            EndDoc(hPrintDC);
            printf("printer: print success: %s\n", filename.c_str());
            image.reset();
            if (!m_savePhotos)
            {
                QFile::remove(QString::fromLocal8Bit(filename.c_str()));
            }

        }

        std::this_thread::sleep_for(std::chrono::milliseconds(kThreadSleepTime));
    }
    Gdiplus::GdiplusShutdown(gdiplusToken);
}

void Printer::addPhotoFile(const std::string& filename)
{
    LOG(INFO) << "adding file" ;
    std::lock_guard<std::mutex> guard(m_mutex);
    m_filenameQueue.push(filename);
    LOG(INFO) << "[addPhotoFile] added: " << filename ;
}

std::string Printer::getImageFile()
{
    std::lock_guard<std::mutex> guard(m_mutex);
    if (m_filenameQueue.empty())
    {
        return std::string();
    }
    auto filename = m_filenameQueue.front();
    m_filenameQueue.pop();
    LOG(INFO) << "[getImageFile] fetched: " << filename 
              << ", remaining size: " << m_filenameQueue.size() ;
    return filename;
}

// 准备系统默认打印机，需要在“Windows设置” - “打印机和扫描仪”设定一个默认打印机
// 没有物理打印机可以使用“Microsoft Print to PDF”替代
bool Printer::preparePrinterSetting()
{
// 检查打印机是否已通过 OpenPrinter 打开
    if (m_hPrinter == nullptr || m_hPrinter == INVALID_HANDLE_VALUE) {
        printf("printer: 请先调用 OpenPrinter 打开打印机\n");
        return false;
    }

    // 创建打印机设备上下文 (DC)
    // 注意：需要获取打印机驱动名称和设备名称
    DWORD needed = 0;
    // 先获取信息缓冲区大小
    if (!GetPrinter(m_hPrinter, 2, nullptr, 0, &needed)) {
        // 可预见错误为122，因为第一次是nullptr
        if (GetLastError() != ERROR_INSUFFICIENT_BUFFER) {
        DWORD error = GetLastError();
        LOG(INFO) << "获取打印机信息大小失败，错误码: " << error << "need size:" << needed ;
        return false;
        }
    }

    if (needed == 0) {
        printf("printer: 获取打印机信息失败\n");
        return false;
    }
    
    PRINTER_INFO_2W* printerInfo = (PRINTER_INFO_2W*)malloc(needed);
    if (!GetPrinterW(m_hPrinter, 2, (LPBYTE)printerInfo, needed, &needed)) {
        printf("printer: 获取打印机信息失败\n");
        return false;
    }
    // 创建打印机 DC
    HDC hPrintDC = CreateDC(
        printerInfo->pDriverName,  // 驱动名称
        printerInfo->pPrinterName, // 打印机名称
        NULL,                     // 输出端口（NULL 表示使用默认）
        NULL                      // 默认设备模式
    );
    free((void*)printerInfo);
    if (!hPrintDC) {
        printf("printer: 创建打印机 DC 失败\n");
        return false;
    }

    // 设置绘图模式，防止图像失真
    SetStretchBltMode(hPrintDC, HALFTONE);

    // 保存 DC 供后续打印使用
    m_printInfo.hDC = hPrintDC;
    
    m_printInfo.lStructSize = sizeof(m_printInfo);
    //设置文档信息（需要在打印前调用 StartDocPrinter）
    // DOC_INFO_1 docInfo = {0};
    // docInfo.pDocName = L"Print Job";
    // docInfo.pOutputFile = NULL;
    // docInfo.pDatatype = L"RAW";
    return true;
}

bool Printer::updatePrinterConfig(double imageWidth, double imageHeight)
{
    if (fabs(m_lastPrintImageWidth - imageWidth) < 0.001 &&
        fabs(m_lastPrintImageHeight - imageHeight) < 0.001)
    {
        return true;
    }
    //------------------------------------
    //锁定全局对象，获取对象指针。 devmode是有关设备初始化和打印机环境的信息
    DEVMODE* devMode = (DEVMODE*)GlobalLock(m_hDevMode);
    if (devMode == 0)
    {
        printf("printer: error in devMode.\n");
        return false;
    }

    m_lastPrintImageWidth = imageWidth;
    m_lastPrintImageHeight = imageHeight;

    //DMORIENT_LANDSCAPE 是横向打印
    //对打印方向的设置，会影响hPrintDC的大小
    //假设宽度为1024，高度为300，则
    //横向打印的时候dc大小会是宽1024 * 高300
    //纵向打印的时候dc大小会是宽300 * 高1024
    devMode->dmOrientation = DMORIENT_PORTRAIT; //打印方向设置成纵向打印

    if (m_pageWidthInMM > 0 &&
        m_pageHeightInMM > 0)
    {
        devMode->dmPaperWidth = m_pageWidthInMM;
        devMode->dmPaperLength = m_pageHeightInMM;
    }
    else
    {
        devMode->dmPaperSize = DMPAPER_USER;
    }


    //设置打印质量的，因为像素被打印到纸上的时候是有做转换的
    //单位是dpi，意为像素每英寸(dots per inch)。就是一英寸的纸张上打印多少个像素点
    //设置的质量可以是具体数值，也可以是宏DMRES_MEDIUM
    //一般我们选择300，或者600，DMRES_MEDIUM = 600dpi
    if (m_dpi > 0)
    {
        devMode->dmPrintQuality = m_dpi;

        //计算scale
        double pixelsInMM = m_dpi / 254;
        double scale = std::min((double)imageHeight / m_pageHeightInMM * pixelsInMM,
                   (double)imageWidth / m_pageWidthInMM * pixelsInMM);
        double old_scale = (imageWidth * imageHeight) / (m_pageWidthInMM * pixelsInMM * m_pageHeightInMM * pixelsInMM);
        LOG(INFO) << "old_scale:" << old_scale << "new_scale:" << scale ;
        devMode->dmFields |= DM_SCALE;
        // double scale = (imageWidth * imageHeight) / (m_pageWidthInMM * pixelsInMM * m_pageHeightInMM * pixelsInMM);
        devMode->dmScale = 50;
    }
    else
    {
        int printQuality = devMode->dmPrintQuality; //获取打印机的打印质量
        printf("printer: default print quality is %d", printQuality);
    }

    HDC hPrintDC = static_cast<HDC>(m_printInfo.hDC);
    ResetDC(hPrintDC, devMode);

    GlobalUnlock(m_hDevMode);


    //------------------------------------
    //获取dc的大小，实际上还有一种HORZRES和VERTRES就是宽度和高度，但是我查得到的结果说计算下来准确的
    //HORZSIZE 是Horizontal size in millimeters，页面宽度（水平），单位毫米mm
    //VERTSIZE 是Vertical size in millimeters，页面高度（垂直），单位毫米mm
    //LOGPIXELSX 是Logical pixels/inch in X，x方向的逻辑像素每英寸.单位 pix / inch，
    //LOGPIXELSY 是Logical pixels/inch in Y，y方向的逻辑像素每英寸.单位 pix / inch，
    //1 inch = 25.4 mm，所以这里是 (mm / 25.4)inch * (pix / inch)，得到的结果就是dc大小像素数为单位
    if (m_pageWidthInMM <= 0 || m_pageHeightInMM <= 0)
    {
        m_pageWidthInMM = GetDeviceCaps(hPrintDC, HORZSIZE);
        m_pageHeightInMM = GetDeviceCaps(hPrintDC, VERTSIZE);
    }
    printf("printer page size (width * height): %d * %dmm\n", m_pageWidthInMM, m_pageHeightInMM);
    //m_pageWidthInPixel = m_pageWidthInMM / 25.4 * GetDeviceCaps(hPrintDC, LOGPIXELSX);
    //m_pageHeightInPixel = m_pageHeightInMM / 25.4 * GetDeviceCaps(hPrintDC, LOGPIXELSY);
    return true;
}

DWORD Printer::getJobsCount() {
    if (!m_hPrinter) {
        std::cerr << "打印机句柄无效\n";
        return 0;
    }

    DWORD needed = 0;
    DWORD returned = 0;

    // 调用时，缓冲区为空，numJobs设为0xFFFFFFFF表示获取所有作业数
    if (!EnumJobs(m_hPrinter, 0, 0xFFFFFFFF, 1, NULL, 0, &needed, &returned)) {
        DWORD err = GetLastError();
        if (err != ERROR_INSUFFICIENT_BUFFER) {
            std::cerr << "EnumJobs 获取缓冲区大小失败，错误码：" << err ;
            return 0;
        }
    }

    return returned;  // 当前打印队列作业数
}
#endif