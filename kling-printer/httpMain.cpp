#include "HttpClient.h"
#include <iostream>
#include <fstream>
#include <thread>

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
    OutputRedirector redirect("log.txt");
    int n = 100;
    json ret = HttpClient::instance().downloadImage("/api/sudoku-images/sudoku-No.100061.jpg", "/cppcode/kling-waic-express/kling-printer/download/test1.jpg");
    std::cout << "json ret:" << ret << std::endl;
    Sleep(1000);

}