#include <glog/logging.h>
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
        LOG(INFO) << "No data.";
        return false;
    }
    return true;
}

std::string DOWNLOAD_DIR = "/cppcode/kling-waic-express/kling-printer/download/";

std::string DOWNLOAD_PREFIX = "https://kling-waic.s3.cn-north-1.amazonaws.com.cn";

std::string FETCH_PREFEIX = "https://waic-api.klingai.com";

namespace fs = std::filesystem;

std::string makeLogDir(const std::string& dir) {
    fs::create_directories(dir);          // 确保目录存在
    return dir + "/";                           // 返回目录即可
}

int main(int argc, char* argv[]) {
    google::InitGoogleLogging(argv[0]);
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
    bool running = true;
    std::queue<std::string> imageQueue;
    std::mutex queue_mutex;
    printf("Please input image path to print. press Enter for end. input empty line for quit.\n");
    while (running) {
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
        std::vector<string> inputs;
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
            }
            LOG(INFO) << "[INFO] Download image success. json: " << ret;
            std::string input = ".\\download\\" + name;
            if (!fileExists(input)) {
                LOG(INFO) << "[ERROR] cannot find this file";
            } else {
                printerManager->addImage(input);
                LOG(INFO) << "[INFO] Add printer queue success";
            }
        }
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
        Sleep(1000);
    }
    LOG(INFO) << "delete";
    delete printerManager;
    return 0;
}