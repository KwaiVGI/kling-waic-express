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
    OutputRedirector redirect("log.txt");
    int n = 100;
    json ret = HttpClient::instance().downloadImage("/api/sudoku-images/sudoku-No.100061.jpg", "/cppcode/kling-waic-express/kling-printer/download/test1.jpg");
    std::cout << "json ret:" << ret << std::endl;
    Sleep(1000);

}