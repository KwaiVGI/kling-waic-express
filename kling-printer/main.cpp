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
    explicit OutputRedirector(const std::string& filename) {
        file.open(filename, std::ios::app); // 追加模式
        if (!file.is_open()) {
            throw std::runtime_error("Failed to open log file: " + filename);
        }

        // 创建带时间戳的 buffer，接管 file.rdbuf()
        old_buf = std::cout.rdbuf(new TimestampBuffer(file.rdbuf()));
    }

    ~OutputRedirector() {
        delete std::cout.rdbuf(); // 删除我们注入的 buffer
        std::cout.rdbuf(old_buf); // 恢复原 buffer
        file.close();
    }

private:
class TimestampBuffer : public std::streambuf {
    std::streambuf* dest;
    char time_buf[128];
    bool need_time = true;

    int overflow(int c) override {
        if (c == EOF) return sync();

        if (need_time) {
            auto now = std::chrono::system_clock::now();
            auto t = std::chrono::system_clock::to_time_t(now);
            auto tm = *std::localtime(&t);
            dest->sputn(time_buf,
                        std::strftime(time_buf, sizeof(time_buf),
                                      "[%Y-%m-%d %H:%M:%S] ", &tm));
            need_time = false;
        }

        int result = dest->sputc(c);

        if (c == '\n') {
            need_time = true;
            sync(); // 关键：刷新缓冲区
        }

        return result;
    }

    int sync() override {
        return dest->pubsync(); // 强制刷新
    }

public:
    explicit TimestampBuffer(std::streambuf* d) : dest(d) {}
};

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
    // cout 定向至指定txt
    OutputRedirector redirect("log.txt");
    // 开启一个控制台窗口，printf重定向至此
    initConsoleOutput();
    std::cout << "Program Begin" << std::endl;
    std::vector<PrinterInfo> printerInfoList;
    printerInfoList.push_back({L"Canon SELPHY CP1500(1)", 100, 148 , 300, false});
    printerInfoList.push_back({L"Canon SELPHY CP1500(2)", 100, 148 , 300, false});
    printerInfoList.push_back({L"Canon SELPHY CP1500(3)", 100, 148 , 300, false});
    printerInfoList.push_back({L"Canon SELPHY CP1500(4)", 100, 148 , 300, false});
    printerInfoList.push_back({L"Canon SELPHY CP1500(5)", 100, 148 , 300, false});
    PrinterManager* printerManager = new PrinterManager(printerInfoList);
    bool running = true;
    printf("Please input image path to print. press Enter for end. input empty line for quit.\n");
    while (running) {
        // std::string input;
        // if(!std::getline(std::cin, input)) {
        //     std::cout << "input stream closed" << std::endl;
        //     if (std::cin.eof()) {
        //         std::cout << "原因：EOF (End of File)\n";
        //     }
        //     if (std::cin.fail()) {
        //         std::cout << "原因：failbit set (读取失败)\n";
        //     }
        //     if (std::cin.bad()) {
        //         std::cout << "原因：badbit set (致命错误)\n";
        //     }
        //     running = false;
        //     continue;
        // };
        
        // if (input.empty()) {
        //     running =false;
        //     continue;
        // }
        
        // if (!fileExists(input)) {
        //     printf("Cannot find this file.\n");
        // } else {
        //     printerManager->addImage(input);
        // }
        std::vector<string> inputs;
        if(HttpClient::instance().fetchImageQueue()) {
            // 增加逻辑
            inputs = collectJpgRelative("/cppcode/kling-waic-express/kling-printer/download");
        }
        for (auto input : inputs) {
            input = ".\\download\\" + input;
            std::cout << input << std::endl;
            if (!fileExists(input)) {
                printf("Cannot find this file.\n");
            } else {
                printerManager->addImage(input);
            }
        }
        Sleep(1000);
    }
    std::cout << "delete" << std::endl;
    delete printerManager;
    return 0;
}