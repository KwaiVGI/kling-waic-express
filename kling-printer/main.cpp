#include <iostream>
#include <string>
#include <streambuf>
#include <fstream>
#include <conio.h>
#include <fstream>
#include "HttpClient.h"
#include "PrinterManager.h"


using namespace std;

DWORD cnt = 0;

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

std::vector<std::string> collectJpgRelative(const std::string& dir)
{
    std::vector<std::string> out;
    std::filesystem::path root(dir);
    for (const auto& e : std::filesystem::directory_iterator(root))
        if (e.is_regular_file()) {
            auto ext = e.path().extension().string();
            std::transform(ext.begin(), ext.end(), ext.begin(), ::tolower);
            if (ext == ".jpg" || ext == ".jpeg")
                out.push_back(std::filesystem::relative(e.path(), root).string());
        }
    return out;
}

int main() {
    SetConsoleOutputCP(65001);
    // 开启一个控制台窗口，printf重定向至此
    initConsoleOutput();
    // cout 定向至指定txt
    OutputRedirector redirect("log.txt");
    std::cout << "Program Begin" << std::endl;
    HGLOBAL hDevMode = NULL;
    std::vector<PrinterInfo> printerInfoList;
    // printerInfoList.push_back({L"Canon SELPHY CP1500 (test1)", 89, 119 , 300, true});
    printerInfoList.push_back({L"Canon SELPHY CP1500", 100, 148 , 300, false});
    printerInfoList.push_back({L"Canon SELPHY CP1500 (test2)", 100, 148 , 300, false});
    PrinterManager* printerManager = new PrinterManager(printerInfoList);
    bool running = true;
    printf("Please input image path to print. press Enter for end. input empty line for quit.\n");
    while (running) {
        std::vector<string> inputs;
        if(HttpClient::instance().fetchImageQueue()) {
            // 增加逻辑
            inputs = collectJpgRelative("/cppcode/kling-waic-express/kling-printer/download");
        }
        for (auto input : inputs) {
            input = "/download/" + input;
            if (!fileExists(input)) {
                printf("Cannot find this file.\n");
            } else {
                printerManager->addImage(input);
            }
        }
        Sleep(10);
    }
    std::cout << "delete" << std::endl;
    delete printerManager;
    return 0;
}