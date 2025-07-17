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
    static HttpClient& instance() {
        static HttpClient inst;
        return inst;
    }
    // explicit HttpClient(const std::string& host,
    //                    std::size_t pool_size = 8);
    HttpClient(const HttpClient&) = delete;

    HttpClient& operator=(const HttpClient&) = delete;

    bool updateImageStatus();

    bool downloadImage(const std::string& imgUrl);

    bool fetchImageQueue();

private:
    
    HttpClient();
    const std::string host_ = "waic.staging.kuaishou.com";
    std::string token_ = "wEJvopXEvl6OnNUHl8DbAd-8Ixkjef9";
    long connTimeout_ = 5000, readTimeout_ = 5000;
    std::unique_ptr<ConnectPool> pool_;
    json get(const std::string& path,
             const std::vector<std::pair<std::string, std::string>>& headers = {});

    json post(const std::string& path,
              const json& body,
              const std::vector<std::pair<std::string, std::string>>& headers = {});
};
#endif