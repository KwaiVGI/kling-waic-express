#include "HttpClient.h"
#include <iostream>
#include <fstream>

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
    HttpClient::instance().fetchImageQueue();
}