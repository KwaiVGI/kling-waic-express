#ifndef HTTP_CLIENT_H
#define HTTP_CLIENT_H
#include "ConnectPool.h"
#include <stdexcept>
#include "include/httplib.h"
#include "include/nlohmann/json.hpp"
#include <string>
using namespace httplib;
using json = nlohmann::json;

class HttpClient {
public:
    // host 形如 "api.example.com"
    explicit HttpClient(const std::string& host,
                       std::size_t pool_size = 8);

    void setToken(const std::string& token);
    void setTimeout(long connMS, long readMS);

    json get(const std::string& path,
             const std::vector<std::pair<std::string, std::string>>& headers = {});

    json post(const std::string& path,
              const json& body,
              const std::vector<std::pair<std::string, std::string>>& headers = {});

private:

    std::string host_;
    bool https_;
    std::string token_;
    long connTimeout_ = 5000, readTimeout_ = 5000;

    std::unique_ptr<ConnectPool> pool_;
};
#endif