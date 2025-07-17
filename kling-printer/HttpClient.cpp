#include "HttpClient.h"

using namespace httplib;

#include "HttpClient.h"

// ---------- 连接包装 ----------
// struct ConnFactory {
//     static 
// };

// ---------- ApiClient 实现 ----------
HttpClient::HttpClient() {
    pool_ = std::make_unique<ConnectPool>(5, host_, connTimeout_, readTimeout_);
}

template <typename Conn>
static json doRequest(Conn& conn,
                      const std::string& token,
                      const std::string& path,
                      const json& body,
                      const std::vector<std::pair<std::string, std::string>>& headers,
                      bool isPost) {
    std::cout << "doRequest" << std::endl;
    httplib::Headers hdrs(headers.begin(), headers.end());
    if (!token.empty()) hdrs.emplace("Authorization", "Token " + token);
    hdrs.emplace("Content-Type", "application/json");

    httplib::Result res;
    if (isPost) {
        res = conn->Post(path.c_str(), hdrs, body.dump(), "application/json");
    } else {
        res = conn->Get(path.c_str(), hdrs);
    }
    if (!res) {
        std::cout << "res error " << res.error() << std::endl;
        throw std::runtime_error("transport error");
    }
    if (res->status != 200) {
        throw std::runtime_error("HTTP " + std::to_string(res->status));
    }
    std::cout << "status code:" << res->status << std::endl; 
    return json::parse(res->body);
}

json HttpClient::get(const std::string& path,
                    const std::vector<std::pair<std::string, std::string>>& headers = {}) {
    auto conn = pool_->acquire();
    auto ret = doRequest(conn->cli, token_, path, json(), headers, false);
    pool_->release(std::move(conn));
    return ret;
}

json HttpClient::post(const std::string& path,
                     const json& body,
                     const std::vector<std::pair<std::string, std::string>>& headers = {}) {
    std::cout << "doPost" << std::endl;
    auto conn = pool_->acquire();
    auto ret = doRequest(conn->cli, token_, path, body, headers, true);
    std::cout << "endRequest" << std::endl;
    pool_->release(std::move(conn));
    return ret;
}

bool HttpClient::fetchImageQueue() {
    json ret = post("/api/printings/fetch", {});
    return true;
}

bool HttpClient::downloadImage(const std::string& imgUrl) {
    return true;
}

bool HttpClient::updateImageStatus() {
    return true;
}