#ifndef HTTP_CLIENT_H
#define HTTP_CLIENT_H
#include "ConnectPool.h"
#include "include/nlohmann/json.hpp"
#include "include/httplib.h"
#include <string>
#include <QImage>
using namespace httplib;
using json = nlohmann::json;
class HttpClient {
public:
    // host 形如 "api.example.com"
    explicit HttpClient(const std::string& host, int port, const std::string& token);

    // static HttpClient& instance() {
    //     static HttpClient inst;
    //     return inst;
    // }
    // explicit HttpClient(const std::string& host,
    //                    std::size_t pool_size = 8);
    HttpClient(const HttpClient&) = delete;

    HttpClient& operator=(const HttpClient&) = delete;

    bool updateImageStatus();

    bool downloadImage(const std::string& imgUrl, const std::string& dir, const std::string& name);

    json fetchImageQueue();

private:
    
    std::string host_;
    int port_;
    std::string token_;
    // const std::string host_ = "waic-api.klingai.com";
    // const int port_ = 6443;
    // std::string token_ = "wEJvopXEvl6OnNUHl8DbAd-8Ixkjef9";
    long connTimeout_ = 5000, readTimeout_ = 5000;
    std::unique_ptr<ConnectPool> pool_;
    static const std::string DOWNLOAD_PATH;
    json getJson(const std::string& path,
             const std::vector<std::pair<std::string, std::string>>& headers = {});

    json postJson(const std::string& path,
              const json& body,
              const std::vector<std::pair<std::string, std::string>>& headers = {});
    QImage getImage(const std::string& path,
              const std::vector<std::pair<std::string, std::string>>& headers = {});
};
#endif