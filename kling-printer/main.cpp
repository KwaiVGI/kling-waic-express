#include <glog/logging.h>
#include <iostream>
#include <string>
#include <streambuf>
#include <fstream>
#include <conio.h>
#include <fstream>
#include "crash_handler.h"
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
    fflush(stdout);
}


std::vector<std::string> collectJpgRelative(const std::string& dir)
{
    SetUnhandledExceptionFilter(TopLevelExceptionHandler);
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

std::string stringPrefix(const std::string& s, const std::string& prefix) {
    return (s.size() >= prefix.size() &&
            s.compare(0, prefix.size(), prefix) == 0)
           ? s.substr(prefix.size())
           : s;
}

bool checkJson(json ret) {
    if (ret["status"] == "FAILED") {
        return false;
    }
    if (!ret.contains("data") || ret["data"].is_null() || ret["status"] != "SUCCEED") {
        // 队列为空
        return false;
    }
    return true;
}

std::string DOWNLOAD_DIR = "/cppcode/kling-waic-express/kling-printer/download/";

std::string DOWNLOAD_PREFIX = "https://kling-waic.s3.cn-north-1.amazonaws.com.cn";

std::string FETCH_PREFEIX = "https://waic-api.klingai.com";

std::atomic<bool> g_stop{false};

namespace fs = std::filesystem;

std::string makeLogDir(const std::string& dir) {
    fs::create_directories(dir);          // 确保目录存在
    return dir + "/";                           // 返回目录即可
}

std::wstring string2wstring(const std::string& s)
{
    if (s.empty()) return {};
    int len = MultiByteToWideChar(CP_UTF8, 0, s.c_str(), -1, nullptr, 0);
    std::wstring out(len, 0);
    MultiByteToWideChar(CP_UTF8, 0, s.c_str(), -1, &out[0], len);
    return out;
}

void httpRun(HttpClient* baseClient, HttpClient* downloadClient, PrinterManager* printerManager) {
    while (!g_stop.load()) {
            // 增加逻辑
        json ret = baseClient->fetchImageQueue();
        // 檢測是否成功
        if (checkJson(ret)) {
            std::string downloadUrl = stringPrefix(ret.at("data").at("task").at("outputs").at("url")
            , DOWNLOAD_PREFIX);
            long long id = ret.at("data").at("id");
            std::string name = std::to_string(id) + ".jpg";
            LOG(INFO) << "[INFO] ready to download. name: " + name + " url:" + downloadUrl;
            if (!downloadClient->downloadImage(downloadUrl, DOWNLOAD_DIR, name)) {
                LOG(INFO) << "[INFO] DownLoad image failed. url:" << downloadUrl;
                continue;
            }
            LOG(INFO) << "[INFO] Download image success. json: " << ret;
            std::string printer_name = ret.at("data").at("name");
            printf("Download image success.%s\n", printer_name.c_str());
            std::string input = ".\\download\\" + name;
            if (!fileExists(input)) {
                LOG(INFO) << "[ERROR] cannot find this file";
            } else {
                printerManager->addImage(input);
                LOG(INFO) << "[INFO] Add printer queue success";
            }
        }
        // http轮询次数
        // Sleep(1000);
    }
}

int main(int argc, char* argv[]) {
    google::InitGoogleLogging(argv[0]);
    FLAGS_logbuflevel = -1;          // 所有级别都缓冲
    FLAGS_logbufsecs  = 1;           // 最多 1 秒刷一次盘
    // 让 INFO 级别写到我们指定的文件
    std::string logDir = makeLogDir("logs");
    google::SetLogDestination(google::GLOG_INFO, logDir.c_str());

    // 开启一个控制台窗口，printf重定向至此
    initConsoleOutput();
    LOG(INFO) << "Program Begin";
    std::vector<PrinterInfo> printerInfoList;
    printerInfoList.push_back({L"Canon SELPHY CP1500(1)", 100, 148 , 300, false});
    printerInfoList.push_back({L"Canon SELPHY CP1500(2)", 100, 148 , 300, false});
    printerInfoList.push_back({L"Canon SELPHY CP1500(3)", 100, 148 , 300, false});
    printerInfoList.push_back({L"Canon SELPHY CP1500(4)", 100, 148 , 300, false});
    printerInfoList.push_back({L"Canon SELPHY CP1500(5)", 100, 148 , 300, false});
    PrinterManager* printerManager = new PrinterManager(printerInfoList);
    HttpClient* baseClient = new HttpClient("waic-api.klingai.com", 443, "wEJvopXEvl6OnNUHl8DbAd-8Ixkjef9");
    HttpClient* downloadClient = new HttpClient("kling-waic.s3.cn-north-1.amazonaws.com.cn", 443, "");
    printf("Wait Printer Jobs. Enter list for find all printer. remove and add for operate printer connect.\n");
    fflush(stdout);
    std::thread httpWorker(httpRun, baseClient, downloadClient, printerManager);
    while (!g_stop.load()) {
        std::string line;
        std::getline(std::cin, line);
        if (!line.empty()) {
            LOG(INFO) << line;
            if (line._Starts_with("add:")) {
                std::string param = line.substr(4);
                if (param.empty()) {
                    printf("parameter error\n");
                    fflush(stdout);
                    continue;
                }
                if (!printerManager->addPrinter(string2wstring(param))) {
                    printf("add printer failed\n");
                    fflush(stdout);
                }
                continue;
            } else if (line._Starts_with("remove:")) {
                std::string param = line.substr(7);
                if (param.empty()) {
                    printf("parameter error\n");
                    fflush(stdout);
                    continue;
                }
                try {
                    int idx = stoi(param);
                    printerManager->removePrinter(idx);
                } catch (exception e) {
                    printf("%s\n", e.what());
                }
                continue;
            } else if (line._Equal("exit")) {
                g_stop.store(true);
                break;
            } else if (line._Equal("list")){
                std::vector<std::string> v = printerManager->listPrinter();
                int idx = 0;
                for (auto element : v) {
                    printf("%d:%s\n", idx++, element.c_str());
                }
                fflush(stdout);
            }
            else {
                printf("parameter error! please check\n");
                fflush(stdout);
            }
            continue;
        }
    }
    LOG(INFO) << "delete";
    httpWorker.join();
    delete baseClient;
    delete downloadClient;
    delete printerManager;
    return 0;
        // if (_kbhit()) {
            // int ch = _getch();
            // if (ch == 'q' || ch == 'Q') {
            //     g_stop.store(true);
            //     break;
            // }
            // if (ch == '\r') {

            // }
        // }
    // while (running) {
        // std::string input;
        // if(!std::getline(std::cin, input)) {
        //     LOG(INFO) << "input stream closed";
        //     if (std::cin.eof()) {
        //         LOG(INFO) << "原因：EOF (End of File)\n";
        //     }
        //     if (std::cin.fail()) {
        //         LOG(INFO) << "原因：failbit set (读取失败)\n";
        //     }
        //     if (std::cin.bad()) {
        //         LOG(INFO) << "原因：badbit set (致命错误)\n";
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
        // std::vector<string> inputs;
        //     // 增加逻辑
        // json ret = baseClient->fetchImageQueue();
        // // 檢測是否成功
        // if (checkJson(ret)) {
        //     std::string downloadUrl = stringPrefix(ret.at("data").at("task").at("outputs").at("url")
        //     , DOWNLOAD_PREFIX);
        //     long long id = ret.at("data").at("id");
        //     std::string name = std::to_string(id) + ".jpg";
        //     LOG(INFO) << "[INFO] ready to download. name: " + name + " url:" + downloadUrl;
        //     if (!downloadClient->downloadImage(downloadUrl, DOWNLOAD_DIR, name)) {
        //         LOG(INFO) << "[INFO] DownLoad image failed. url:" << downloadUrl;
        //         continue;
        //     }
        //     LOG(INFO) << "[INFO] Download image success. json: " << ret;
        //     std::string input = ".\\download\\" + name;
        //     if (!fileExists(input)) {
        //         LOG(INFO) << "[ERROR] cannot find this file";
        //     } else {
        //         printerManager->addImage(input);
        //         LOG(INFO) << "[INFO] Add printer queue success";
        //     }
        // }
        // inputs = collectJpgRelative("/cppcode/kling-waic-express/kling-printer/download");
        // for (auto input : inputs) {
        //     input = ".\\download\\" + input;
        //     LOG(INFO) << input;
        //     if (!fileExists(input)) {
        //         printf("Cannot find this file.\n");
        //     } else {
        //         printerManager->addImage(input);
        //     }
        // }
    //     Sleep(1000);
    // }
}