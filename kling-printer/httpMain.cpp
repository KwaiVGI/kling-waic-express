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
    HttpClient* client = new HttpClient("52.81.18.108:10238/api/tokens/latest/printings", 5);
    client->setToken("wEJvopXEvl6OnNUHl8DbAd-8Ixkjef9");
    json result = client->post("fetch", {}, {});
    std::cout << "[POST]" << result;
}