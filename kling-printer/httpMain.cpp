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
    std::vector<std::string> inputs = collectJpgRelative("/cppcode/kling-waic-express/kling-printer/download");
    HttpClient* downloadClient = new HttpClient("kling-waic.s3.cn-north-1.amazonaws.com.cn", 443, "");
    downloadClient->downloadImage("/output-images/atxbYr1Jq6oykEzmk4qrkKmnyVOR_h_Oxuqv4OrZAxk.jpg", "/cppcode/kling-waic-express/kling-printer/download/", "123.jpg");
}