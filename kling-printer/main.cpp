#include <iostream>
#include <string>
#include <streambuf>
#include <fstream>
#include <conio.h>
#include <windows.h>
#include <fstream>
#include "Printer.h"
#include "PrinterManager.h"


using namespace std;

DWORD cnt = 0;

PRINTER_INFO_2* getPrinterList() {
    PRINTER_INFO_2*    list;
    DWORD            sz = 0;
    DWORD Level = 2;
    int            i;
    int            sl;

    EnumPrinters(PRINTER_ENUM_LOCAL | PRINTER_ENUM_CONNECTIONS, NULL, Level, NULL, 0, &sz, &cnt);
    if ((list = (PRINTER_INFO_2*)malloc(sz)) == 0)    return nullptr;
    
    if (!EnumPrinters(PRINTER_ENUM_LOCAL | PRINTER_ENUM_CONNECTIONS, NULL, Level, (LPBYTE)list, sz, &sz, &cnt))
    {
        free(list);
        return nullptr;
    }
    for (int i = 0 ; i < (int) cnt; ++i) {
        if (list[i].Attributes & PRINTER_ATTRIBUTE_NETWORK) continue;
        printf("%d-th Local Printer Name: %ls\n", i+1, list[i].pPrinterName);
        fflush(stdout);
    }
    return list;
}

bool fileExists(const std::string& path) {
    std::ifstream file(path);
    return file.good();  // 如果文件打开成功，则存在
}

void initConsoleOutput() {
    // 确保控制台已分配（如果从GUI程序启动）
    if (!GetConsoleWindow()) {
        AllocConsole();
        freopen("CONOUT$", "w", stdout);
        freopen("CONIN$", "r", stdin);
        freopen("CONOUT$", "w", stderr);
    }
    printf("Console Panel init success!\n");
}

class OutputRedirector {
public:
    OutputRedirector(const std::string& filename) {
        file.open(filename);
        old_buf = std::cout.rdbuf(file.rdbuf());
    }

    ~OutputRedirector() {
        std::cout.rdbuf(old_buf);
        file.close();
    }

private:
    std::ofstream file;
    std::streambuf* old_buf;
};


int main() {
    SetConsoleOutputCP(65001);
    // 开启一个控制台窗口，printf重定向至此
    initConsoleOutput();
    // cout 定向至指定txt
    OutputRedirector redirect("log.txt");
    std::cout << "Program Begin" << std::endl;
    getPrinterList();
    HGLOBAL hDevMode = NULL;
    std::vector<PrinterInfo> printerInfoList;
    printerInfoList.push_back({L"Canon SELPHY CP1500 (test1)", 89, 119 , 300, true});
    printerInfoList.push_back({L"Canon SELPHY CP1500", 100, 148 , 300, true});
    printerInfoList.push_back({L"Canon SELPHY CP1500 (test2)", 89, 119 , 300, true});
    PrinterManager* printerManager = new PrinterManager(printerInfoList);
    bool running = true;
    printf("Please input image path to print. press Enter for end. input empty line for quit.\n");
    while (running) {
        std::string input;
        if(!std::getline(std::cin, input)) {
            std::cout << "input stream closed" << std::endl;
            if (std::cin.eof()) {
                std::cout << "原因：EOF (End of File)\n";
            }
            if (std::cin.fail()) {
                std::cout << "原因：failbit set (读取失败)\n";
            }
            if (std::cin.bad()) {
                std::cout << "原因：badbit set (致命错误)\n";
            }
            running = false;
            continue;
        };
        
        if (input.empty()) {
            running =false;
            continue;
        }
        
        if (!fileExists(input)) {
            printf("Cannot find this file.\n");
        } else {
            printerManager->addImage(input);
        }
        Sleep(10);
    }
    std::cout << "delete" << std::endl;
    delete printerManager;
    return 0;
}